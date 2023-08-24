package cn.minih.web.core

import cn.minih.common.exception.MinihErrorCode
import cn.minih.common.exception.MinihException
import cn.minih.common.util.*
import cn.minih.core.annotation.Service
import cn.minih.core.beans.BeanFactory
import cn.minih.web.annotation.Delete
import cn.minih.web.annotation.Get
import cn.minih.web.annotation.Post
import cn.minih.web.annotation.Put
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

/**
 * 注册服务
 * @author hubin
 * @since 2023-08-05 19:10:40
 * @desc
 */
object RegisterService {

    private lateinit var vertx: Vertx
    private fun getNeedRegisterFunctions(iservice: KClass<*>): List<Triple<String, String, Annotation>?> {
        val pathExists = mutableListOf<String>()
        val iserviceMapping = findRequestMapping(iservice as KAnnotatedElement)
        var parentPath = ""
        iserviceMapping?.let { def -> parentPath = formatPath(def.url) }
        return iservice.members.filterIsInstance<KFunction<*>>().map { fn ->
            val requestType = findRequestMapping(fn as KAnnotatedElement)
            requestType?.let { mapping ->
                var realPath = parentPath
                if (mapping.url.isNotBlank() && mapping.url != "/") {
                    realPath = realPath.plus(formatPath(mapping.url))
                }
                if (pathExists.contains("${getHttpMethod(mapping.type)?.name()}_$realPath")) {
                    throw MinihException("路径重复！")
                }
                pathExists.add("${getHttpMethod(mapping.type)?.name()}_$realPath")
                Triple(fn.name, realPath, mapping.type)
            }

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

    private fun getBeanCall(params: List<KParameter>): Any? {
        if (params.isNotEmpty()) {
            val p1 = params.first()
            val clazz = p1.type.classifier as KClass<*>
            val superClasses = getSuperClassRecursion(clazz)
            if (superClasses.contains(cn.minih.web.service.Service::class)) {
                return BeanFactory.instance.getBeanFromType(p1.type)
            }
        }
        return null
    }

    private fun registerEventBusConsumer(realPath: String, fn: KFunction<Any?>) {
        val serverName = getProjectName(this.vertx.orCreateContext)
        vertx.eventBus().consumer(serverName.plus(realPath.replace("/", "."))) { p ->
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch {
                var rawResult: Any?
                try {
                    val bean: Any? = getBeanCall(fn.parameters)
                    val parameters = bean?.let {
                        fn.parameters.subList(1, fn.parameters.size)
                    } ?: fn.parameters
                    val bodyParameters =
                        parameters.filter { it.type == RoutingContext::class.createType() || it.type == HttpServerRequest::class.createType() }
                    Assert.isNull(bodyParameters, "接口[${fn.name}]需要http请求参数，无法创建eventBusConsumer!")
                    val args = generateArgs(parameters, p.body()).map { it.second }.toTypedArray()
                    rawResult = when {
                        bean == null && parameters.isEmpty() -> if (fn.isSuspend) fn.callSuspend() else fn.call()
                        bean == null -> if (fn.isSuspend) fn.callSuspend(*args) else fn.call(*args)
                        parameters.isEmpty() -> if (fn.isSuspend) fn.callSuspend(bean) else fn.call(bean)
                        else -> if (fn.isSuspend) fn.callSuspend(bean, *args) else fn.call(bean, *args)
                    }
                } catch (e: Exception) {
                    rawResult =
                        MinihException(
                            "远程接口调用出现错误,${e.cause}",
                            errorCode = MinihErrorCode.ERR_CODE_REMOTE_CALL_ERROR
                        )
                }
                p.reply(rawResult?.toJsonObject())
            }
        }
    }

    fun registerService(
        serviceList: List<KClass<*>>,
        vertx: Vertx,
        handler: (t: Triple<String, HttpMethod?, KFunction<*>>) -> Unit
    ) {
        this.vertx = vertx
        serviceList.forEach { iservice ->
            val serviceDefs = BeanFactory.instance.findBeanDefinitionByType(iservice)
                .filter { it.annotations.map { a1 -> a1.annotationClass }.contains(Service::class) }
            Assert.isTrue(serviceDefs.size == 1) {
                MinihException("${iservice.simpleName} 实例未找到或找到多个！")
            }
            val serviceDef = serviceDefs.first()
            val bean = BeanFactory.instance.getBean(serviceDef.beanName)
            getNeedRegisterFunctions(iservice).filterNotNull().forEach {
                val fns = bean::class.memberFunctions.filter { fn -> fn.name == it.first }
                var fn = fns.first()
                if (fns.size > 1) {
                    val iFns = iservice.members.filterIsInstance<KFunction<*>>().filter { iFn -> iFn.name == it.first }
                    iFns.forEach { fnTmp ->
                        val requestType = findRequestMapping(fnTmp as KAnnotatedElement)
                        requestType?.let { mapping ->
                            if (getHttpMethod(mapping.type) == getHttpMethod(it.third)) {
                                fn = fns.first { fnTmp1 ->
                                    var flag =
                                        fnTmp1.name == fnTmp.name
                                                && fnTmp1.valueParameters.size == fnTmp.valueParameters.size
                                                && fnTmp1.returnType::class == fnTmp.returnType::class
                                    if (flag) {
                                        for ((index, kParameter) in fnTmp1.valueParameters.withIndex()) {
                                            val i1 = fnTmp.valueParameters[index]
                                            if (kParameter.type != i1.type) {
                                                flag = false
                                            }
                                        }
                                    }
                                    flag
                                }
                            }
                        }
                    }
                }
                handler(Triple(it.second, getHttpMethod(it.third), fn))
                registerEventBusConsumer(it.second, fn)
            }
        }
    }


}
