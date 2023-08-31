package cn.minih.core.beans

import cn.minih.common.exception.MinihException
import cn.minih.common.util.Assert
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

/**
 *  代理处理
 * @author hubin
 * @since 2023-08-31 15:02:17
 */
class ServiceProxyHandler(private val clazz: KClass<*>) : InvocationHandler {

    private var delegate: Any

    init {
        val beanConstructor = clazz.primaryConstructor ?: clazz.constructors.first()
        delegate = if (beanConstructor.parameters.isEmpty()) {
            clazz.createInstance()
        } else {
            val params = mutableMapOf<KParameter, Any?>()
            beanConstructor.parameters.forEach { it1 ->
                val type = BeanFactory.instance.getBeanDefinitionByType(it1.type)
                Assert.notNull(type) { MinihException("未找到实例${it1.name},请检查") }
                params[it1] = BeanFactory.instance.getBean(type.beanName)
            }
            beanConstructor.callBy(params)
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
        return method.invoke(delegate, args)
    }


}
