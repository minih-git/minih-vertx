package cn.minih.ms.client.proxy

import cn.minih.web.service.Service
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * 代理工厂
 * @author hubin
 * @since 2023-08-06 11:03:52
 * @desc
 */
@Suppress("UNCHECKED_CAST")
object ServiceProxyFactory {


    fun <T : Service> getProxy(service: KClass<*>): T {
        return Proxy.newProxyInstance(
            service.java.classLoader,
            arrayOf(service.java),
            ServiceProxyHandler()
        ) as T
    }

}