package cn.minih.ms.client.proxy

import cn.minih.common.exception.MinihErrorCode
import cn.minih.common.exception.MinihException
import cn.minih.common.util.*
import cn.minih.core.config.MICROSERVICE_INNER_REQUEST_HEADER
import cn.minih.core.config.MICROSERVICE_INNER_REQUEST_HEADER_VALUE
import cn.minih.ms.client.MsClient
import cn.minih.ms.client.config.Config
import cn.minih.web.annotation.*
import cn.minih.web.service.Service
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.RequestOptions
import io.vertx.core.json.JsonObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.valueParameters


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

    private fun invokeByEventBus(
        proxy: Any,
        method: Method,
        args: Array<out Any>?,
        context: Context
    ): Future<Any> {
        val proxied = getProxied(proxy, method)
        val address =
            getPath(proxied.first as KAnnotatedElement, proxied.second as KAnnotatedElement).replace("/", ".")
        val remoteServiceAnno = proxied.first!!.findAnnotation<RemoteService>()!!
        val args1 = buildArgs(proxied.second, args).first
        return context.owner().eventBus()
            .request<JsonObject>(remoteServiceAnno.remote.plus(address), args1)
            .compose {
                val rType = proxied.second.returnType
                if (isBasicType(rType)) {

                    Future.succeededFuture(it?.body()?.let { it1 -> covertBasic(it1, rType) })
                }
                Future.succeededFuture<Any>(it?.body()?.toJsonObject()?.covertTo(proxied.second.returnType))
            }

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
        val proxied = getProxied(proxy, method)
        val path =
            getPath(proxied.first as KAnnotatedElement, proxied.second as KAnnotatedElement)
        val methodMapping = findRequestMapping(proxied.second) ?: throw MinihException(
            "未找到远程服务配置！",
            errorCode = MinihErrorCode.ERR_CODE_NOT_FOUND_ERROR
        )
        val remoteServiceAnno = proxied.first?.findAnnotation<RemoteService>() ?: throw MinihException(
            "未找到远程服务配置！",
            errorCode = MinihErrorCode.ERR_CODE_NOT_FOUND_ERROR
        )
        return MsClient.getAvailableServiceNoSuspend(remoteServiceAnno.remote).compose {
            if (it == null) {
                throw MinihException(
                    "未找到服务器！",
                    errorCode = MinihErrorCode.ERR_CODE_NOT_FOUND_ERROR
                )
            }
            val argsRaw = buildArgs(proxied.second, args)
            val args1 = argsRaw.first.toBuffer()
            val header = argsRaw.second


            val httpClient = context.owner().createHttpClient()
            val requestOptions = RequestOptions()
            requestOptions.method = getHttpMethod(methodMapping.type)
            requestOptions.uri = path
            requestOptions.host = it.location.getString("host")
            requestOptions.port = it.location.getInteger("port")
            requestOptions.addHeader("Content-Length", args1.length().toString())
            requestOptions.addHeader(MICROSERVICE_INNER_REQUEST_HEADER, MICROSERVICE_INNER_REQUEST_HEADER_VALUE)
            requestOptions.addHeader("Content-Type", "application/json")
            if (!header.isEmpty) {
                header.forEach { h ->
                    requestOptions.addHeader(h.key, h.value.toString())
                }
            }
            requestOptions.setTimeout(getConfig("ms", Config::class, context).timeout)
            httpClient.request(requestOptions).compose { req ->
                req.write(args1)
                req.response().compose { res -> res.body() }
            }
        }.compose {
            val result = it.toJson().toJsonObject()
            val rType = proxied.second.returnType.arguments.first().type!!
            Future.succeededFuture(covertTypeData(result.getValue("data"), rType))
        }

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
            val proxied = getProxied(proxy, method)
            val remoteService = proxied.first?.findAnnotation<RemoteService>()
            return when (remoteService?.remoteType) {
                RemoteType.EVENT_BUS -> invokeByEventBus(proxy, method, args, context)
                else -> invokeByHttpClient(proxy, method, args, context)
            }
        } catch (e: Exception) {
            throw MinihException("远程服务执行错误!")
        }
    }


}
