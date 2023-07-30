package cn.minih.core.beans

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
data class BeanDefinition(
    var beanName: String,
    var clazz: KClass<*>,
    var type: KType,
    var annotations: List<Annotation>,
    var supertypes: List<KType>,
)

class BeanDefinitionBuilder {
    fun build(it: KClass<*>, beanName: String? = null): BeanDefinition {
        return BeanDefinition(beanName ?: it.simpleName!!, it, it.createType(), it.annotations, it.supertypes)
    }
}
