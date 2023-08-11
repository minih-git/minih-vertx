@file:Suppress("unused")

package cn.minih.core.util

import cn.minih.common.util.getSuperClassRecursion
import cn.minih.core.beans.BeanFactory
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

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
            return BeanFactory.instance.getBeanFromType(p1.type)
        }
    }
    return null
}