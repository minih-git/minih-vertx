package cn.minih.ms.client

import cn.minih.common.util.getSuperClassRecursion
import cn.minih.core.annotation.Component
import cn.minih.core.beans.BeanDefinitionBuilder
import cn.minih.core.beans.BeanFactory
import cn.minih.core.boot.ReplenishInitBeanProcess
import cn.minih.ms.client.proxy.ServiceProxyFactory
import cn.minih.web.annotation.RemoteService
import cn.minih.web.service.Service
import io.vertx.core.Vertx
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/**
 * 初始化远程调用
 * @author hubin
 * @since 2023-08-06 12:24:57
 * @desc
 */
@Component
class InitRemoteService : ReplenishInitBeanProcess {
    private fun hasRemoteAnnotation(clazz: KClass<*>): Boolean {
        try {
            return clazz.hasAnnotation<RemoteService>()
        } catch (_: UnsupportedOperationException) {
        }
        return false
    }

    override suspend fun exec(vertx: Vertx, clazz: List<KClass<*>>) {

        val remoteService = clazz.filter { hasRemoteAnnotation(it) }
        remoteService.forEach {
            if (it.simpleName != null) {
                var beanName = it.simpleName!!
                it.findAnnotation<RemoteService>()?.let { con ->
                    if (con.name.isNotBlank()) {
                        beanName = con.name
                    }
                }
                if (getSuperClassRecursion(it).contains(Service::class)) {
                    val beanDefinition = BeanDefinitionBuilder().build(it, beanName)
                    BeanFactory.instance.registerBeanDefinition(beanName, beanDefinition)
                    BeanFactory.instance.registerBean(beanName, ServiceProxyFactory.getProxy(it))
                }

            }
        }


    }
}