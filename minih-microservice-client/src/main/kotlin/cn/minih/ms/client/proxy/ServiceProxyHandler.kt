package cn.minih.ms.client.proxy

import cn.minih.common.exception.MinihErrorCode
import cn.minih.common.exception.MinihException
import cn.minih.common.util.*
import cn.minih.core.config.MICROSERVICE_INNER_REQUEST_HEADER
import cn.minih.core.config.MICROSERVICE_INNER_REQUEST_HEADER_VALUE
import cn.minih.ms.client.MsClient
import cn.minih.ms.client.config.Config
import cn.minih.ms.client.config.MsEnv
import cn.minih.web.annotation.*
import cn.minih.web.service.Service
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.RequestOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.*


/**
 * 代理处理实现
 * @author hubin
 * @since 2023-08-06 11:07:42
 * @desc
 */
class ServiceProxyHandler : InvocationHandler, Service {

    private fun getProxiedClazz(proxy: Any): KClass<*> {
        val superClazz = getSuperClassRecursion(proxy::class)
        return superClazz.first { it.hasAnnotation<RemoteService>() }
    }

    private fun getProxied(proxy: Any, method: Method): Pair<KClass<*>?, KCallable<*>> {
        val proxiedClazz = getProxiedClazz(proxy)
        val proxiedMethod = proxiedClazz.members.first { it.name == method.name }
        return Pair(proxiedClazz, proxiedMethod)
    }

    private fun getPath(proxiedClazz: KAnnotatedElement, proxiedMethod: KAnnotatedElement): String {
        val iserviceMapping = findRequestMapping(proxiedClazz)
        val methodMapping = findRequestMapping(proxiedMethod)
        var path = ""
        iserviceMapping?.let {
            path = path.plus(formatPath(it.url))
        }
        methodMapping?.let {
            path = path.plus(formatPath(it.url))
        }
        return formatPath(path)
    }

    private fun buildArgs(method: KCallable<*>, args: Array<out Any>?): Pair<JsonObject, JsonObject> {
        val params = JsonObject()
        val headers = JsonObject()
        if (method.valueParameters.isNotEmpty()) {
            if (args == null) {
                throw MinihException("参数数量错误")
            }
            for ((index, parameter) in method.valueParameters.withIndex()) {
                val header = parameter.findAnnotation<HttpHeader>()
                val value = args[index]
                val typeClass = args[index]::class
                if (header == null) {
                    params.put(
                        parameter.name,
                        when {
                            typeClass.simpleName!!.contains("List") -> value
                            isBasicType(typeClass.createType()) -> value
                            else -> value.toJsonObject()
                        }
                    )
                } else {
                    headers.put(
                        header.name.ifBlank { parameter.name },
                        when {
                            typeClass.simpleName!!.contains("List") -> value
                            isBasicType(typeClass.createType()) -> covertBasic(value, typeClass.createType())
                            value is Map<*, *> -> value.forEach { headers.put(it.key.toString(), it.value) }
                            else -> value.toJsonObject().forEach { headers.put(it.key.toString(), it.value) }
                        }
                    )
                }
            }
        }
        return Pair(params, headers)
    }


    private fun getHttpMethod(n: Annotation): HttpMethod? {
        return when (n) {
            is Post -> HttpMethod.POST
            is Get -> HttpMethod.GET
            is Put -> HttpMethod.PUT
            is Delete -> HttpMethod.DELETE
            else -> null
        }

    }

    private fun invokeByHttpClient(
        proxy: Any,
        method: Method,
        args: Array<out Any>?,
        context: Context,
    ): Future<Any> {
        val promise = Promise.promise<Any>()
        CoroutineScope(Dispatchers.IO).launch {
            val proxied = getProxied(proxy, method)
            val remoteServiceAnno = proxied.first?.findAnnotation<RemoteService>() ?: throw MinihException(
                "未找到远程服务配置！",
                errorCode = MinihErrorCode.ERR_CODE_NOT_FOUND_ERROR
            )

            val hasErrorBack = remoteServiceAnno.errorCallBack != Any::class
            var result: Any? = null
            if (hasErrorBack) {
                val ins = remoteServiceAnno.errorCallBack.createInstance()
                result = when {
                    args == null -> method.invoke(ins)
                    else -> method.invoke(ins, *args)
                }
            }
            val config = getConfig("ms", Config::class, context)

            var host = ""
            var port = ""
            if (remoteServiceAnno.url.isNotBlank()) {
                host = remoteServiceAnno.url
            } else {
                when (config.msEnv) {
                    MsEnv.CONSUL -> {
                        val record = MsClient.getAvailableServiceNoSuspend(remoteServiceAnno.remote).await()
                        host = record?.location?.getString("host") ?: ""
                        port = record?.location?.getString("port") ?: ""
                    }

                    MsEnv.K8S -> {
                        host = remoteServiceAnno.remote
                    }
                }
            }
            Assert.notBlank(host) { MinihException("未找到服务器", MinihErrorCode.ERR_CODE_NOT_FOUND_ERROR) }
            val argsRaw = buildArgs(proxied.second, args)
            val argsBuffer = argsRaw.first.toBuffer()
            val client = WebClient.create(context.owner())
            val requestOptions = buildRequestOption(proxied, host, port, argsRaw.second, config.timeout)
            requestOptions.addHeader("Content-Length", argsBuffer.length().toString())
            log.debug("远程服务调用: ${requestOptions.method.name()}  ${requestOptions.host} ${requestOptions.uri}")
            client.request(requestOptions.method, requestOptions).sendBuffer(argsBuffer).onSuccess {
                val result1 = it.body().toJsonObject()
                val rType = proxied.second.returnType.arguments.first().type!!
                val rValue = result1.getValue("data")
                log.debug("远程服务调用返回结果: $result1")
                promise.complete(rValue?.let { covertTypeData(rValue, rType) })
            }.onFailure {
                Assert.isTrue(hasErrorBack && result != null) {
                    MinihException(it.message, MinihErrorCode.ERR_CODE_REMOTE_CALL_ERROR)
                }
                promise.complete(
                    when (result) {
                        is Future<*> -> result.result()
                        else -> result
                    }
                )
            }
        }
        return promise.future()
    }

    private fun buildRequestOption(
        proxied: Pair<KClass<*>?, KCallable<*>>,
        host: String,
        port: String,
        header: JsonObject,
        timeout: Long
    ): RequestOptions {
        val path =
            getPath(proxied.first as KAnnotatedElement, proxied.second as KAnnotatedElement)
        val methodMapping = findRequestMapping(proxied.second) ?: throw MinihException(
            "未找到远程服务配置！",
            errorCode = MinihErrorCode.ERR_CODE_NOT_FOUND_ERROR
        )
        val requestOptions = RequestOptions()
        requestOptions.method = getHttpMethod(methodMapping.type)
        requestOptions.uri = path
        requestOptions.host = host
        if (port.isNotBlank()) {
            requestOptions.port = port.toInt()
        }

        requestOptions.addHeader(MICROSERVICE_INNER_REQUEST_HEADER, MICROSERVICE_INNER_REQUEST_HEADER_VALUE)
        requestOptions.addHeader("Content-Type", "application/json")
        if (!header.isEmpty) {
            header.forEach { h ->
                requestOptions.addHeader(h.key, h.value.toString())
            }
        }
        requestOptions.setTimeout(timeout)

        return requestOptions
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
        try {
            if (method.name.equals("toString")) {
                return proxy::class.simpleName + "@" + Integer.toHexString(System.identityHashCode(proxy))
            } else if (method.name.equals("hashCode")) {
                return System.identityHashCode(proxy)
            } else if (method.name.equals("equals") && args?.size == 1) {
                return proxy == args[0]
            }
            val context = Vertx.currentContext()
            return invokeByHttpClient(proxy, method, args, context)
        } catch (e: Throwable) {
            throw MinihException("远程服务执行错误!")
        }
    }


}
