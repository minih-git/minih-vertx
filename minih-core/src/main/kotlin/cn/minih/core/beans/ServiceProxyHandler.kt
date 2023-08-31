package cn.minih.core.beans

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

/**
 *  代理处理
 * @author hubin
 * @since 2023-08-31 15:02:17
 */
class ServiceProxyHandler(clazz: KClass<*>) : InvocationHandler {

    private var delegate: Any

    init {
        val beanConstructor = clazz.primaryConstructor ?: clazz.constructors.first()
        delegate = if (beanConstructor.parameters.isEmpty()) {
            clazz.createInstance()
        } else {
            beanConstructor.callBy(ServiceProxyFactory.getParams(clazz))
        }
    }


    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
        if (method.name.equals("toString")) {
            return proxy::class.simpleName + "@" + Integer.toHexString(System.identityHashCode(proxy))
        } else if (method.name.equals("hashCode")) {
            return System.identityHashCode(proxy)
        } else if (method.name.equals("equals") && args?.size == 1) {
            return proxy == args[0]
        }
        if (args == null) {
            return method.invoke(delegate)
        }
        return method.invoke(delegate, *args)
    }


}