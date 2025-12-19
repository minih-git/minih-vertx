@file:Suppress("UNCHECKED_CAST")

package cn.minih.core.beans

import cn.minih.common.exception.MinihException
import cn.minih.common.util.Assert
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers.isDeclaredBy
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
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

    fun <T : Any> getProxy(service: KClass<T>): T {
        val parameters = service.primaryConstructor?.parameters
        val params = mutableListOf<KClass<*>>()
        val paramsMap = getParams(service)
        val args = mutableListOf<Any?>()
        parameters?.let {
            it.forEach { i ->
                params.add(i.type.classifier as KClass<*>)
                args.add(paramsMap[i])
            }
        }
        return ByteBuddy()
            .subclass(service.java)
            .method(isDeclaredBy(service.java))
            .intercept(InvocationHandlerAdapter.of(ServiceProxyHandler(service)))
            .make()
            .load(service.java.classLoader)
            .loaded
            .getDeclaredConstructor(*params.map { it.java }.toTypedArray())
            .newInstance(*args.toTypedArray()) as T
    }


    fun getParams(clazz: KClass<*>): MutableMap<KParameter, Any?> {
        val beanConstructor = clazz.primaryConstructor ?: clazz.constructors.first()
        val params = mutableMapOf<KParameter, Any?>()
        beanConstructor.parameters.forEach { it1 ->
            val type = BeanFactory.instance.getBeanDefinitionByType(it1.type)
            Assert.notNull(type) { MinihException("未找到实例${it1.name},请检查") }
            params[it1] = BeanFactory.instance.getBean(type.beanName)
        }
        return params
    }
}
