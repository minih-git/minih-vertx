package cn.minih.web.core

import cn.minih.common.exception.MinihException
import cn.minih.common.util.Assert
import cn.minih.common.util.generateArgs
import cn.minih.common.util.getProjectName
import cn.minih.common.util.toJsonObject
import cn.minih.core.beans.BeanFactory
import cn.minih.web.annotation.Delete
import cn.minih.web.annotation.Get
import cn.minih.web.annotation.Post
import cn.minih.web.annotation.Put
import cn.minih.web.util.findRequestMapping
import cn.minih.web.util.formatPath
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun registerEventBusConsumer(realPath: String, fn: KFunction<Any?>) {
        val serverName = getProjectName()
        vertx.eventBus().consumer(serverName.plus(realPath.replace("/", "."))) { p ->
            GlobalScope.launch(vertx.orCreateContext.dispatcher()) {
                val args = generateArgs(fn.parameters, p.body())
                val rawResult = when (fn.parameters.size) {
                    0 -> if (fn.isSuspend) fn.callSuspend() else fn.call()
                    else -> if (fn.isSuspend) fn.callSuspend(*args) else fn.call(*args)
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