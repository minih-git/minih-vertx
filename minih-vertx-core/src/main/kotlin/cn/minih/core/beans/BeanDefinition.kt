package cn.minih.core.beans

import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
data class BeanDefinition(
    var clazz: KClass<*>,
    var type: KType,
    var annotations: List<Annotation>,
    var beanName: String
)
