@file:Suppress("unused")

package cn.minih.core.util

import cn.minih.common.util.getSuperClassRecursion
import cn.minih.core.beans.BeanFactory
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType

/**
 * 工具类
 * @author hubin
 * @since 2023-08-12 02:50:35
 * @desc
 */
fun getBeanCall(params: List<KParameter>): Any? {
    if (params.isNotEmpty()) {
        val p1 = params.first()
        val clazz = p1.type.classifier as KClass<*>
        val superClasses = getSuperClassRecursion(clazz)
        if (superClasses.contains(cn.minih.web.service.Service::class)) {
            return BeanFactory.instance.getBeanFromType(clazz.supertypes.first { it != Proxy::class.createType() })
        }
    }
    return null
}