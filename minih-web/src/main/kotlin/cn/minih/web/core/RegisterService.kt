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
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions

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
                if (pathExists.contains(realPath)) {
                    throw MinihException("路径重复！")
                }
                pathExists.add(realPath)
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
        val serverName = getProjectName()
        vertx.eventBus().consumer(serverName.plus(realPath.replace("/", "."))) { p ->

            CoroutineScope(vertx.orCreateContext.dispatcher()).launch {
                var rawResult: Any?
                try {
                    val bean: Any? = getBeanCall(fn.parameters)
                    val realArgs = bean?.let {
                        fn.parameters.subList(1, fn.parameters.size)
                    } ?: fn.parameters
                    val args = generateArgs(realArgs, p.body())
                    rawResult = when {
                        bean == null && realArgs.isEmpty() -> if (fn.isSuspend) fn.callSuspend() else fn.call()
                        bean == null -> if (fn.isSuspend) fn.callSuspend(*args) else fn.call(*args)
                        realArgs.isEmpty() -> if (fn.isSuspend) fn.callSuspend(bean) else fn.call(bean)
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
                val fn = bean::class.memberFunctions.first { fn -> fn.name == it.first }
                handler(Triple(it.second, getHttpMethod(it.third), fn))
                registerEventBusConsumer(it.second, fn)
            }
        }
    }


}