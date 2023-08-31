package cn.minih.core.beans

import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers.isDeclaredBy
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 *  代理工程
 * @author hubin
 * @since 2023-08-31 15:01:32
 */
object ServiceProxyFactory {

    fun <T : Any> getSuperProxy(service: KClass<T>, clazz: KClass<*>): T {
        return Proxy.newProxyInstance(
            service.java.classLoader,
            arrayOf(service.java),
            ServiceProxyHandler(clazz)
        ) as T
    }

    fun <T : Any> getCglibProxy(service: KClass<T>): T {
        val params = mutableListOf<KClass<*>>()
        service.primaryConstructor?.parameters?.map { it::class }?.let { params.addAll(it) }
        return ByteBuddy()
            .subclass(service.java)
            .method(isDeclaredBy(service.java))
            .intercept(InvocationHandlerAdapter.of(ServiceProxyHandler(service)))
            .make()
            .load(service.java.classLoader)
            .loaded
            .getDeclaredConstructor(*params.map { it.java }.toTypedArray())
            .newInstance()
    }
}
