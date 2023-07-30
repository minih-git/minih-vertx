package cn.minih.core.beans

import cn.minih.core.exception.MinihException
import cn.minih.core.utils.Assert
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
class BeanFactory {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BeanFactory()
        }
    }

    private val beanDefinitionMap = mutableMapOf<String, BeanDefinition>()
    private val beanDefinitionNames = mutableListOf<String>()
    private val singletonObjects = mutableMapOf<String, Any>()

    fun registerBeanDefinition(beanName: String, beanDefinition: BeanDefinition) {
        Assert.notBlank(beanName) { MinihException("bean name 不能为空！") }
        Assert.notNull(beanDefinition) { MinihException("bean definition不能为null！") }
        val existingDefinition = beanDefinitionMap[beanName]
        Assert.isNull(existingDefinition) { MinihException("$beanName bean definition不能重复定义！") }
        beanDefinitionMap[beanName] = beanDefinition
        this.beanDefinitionNames.add(beanName)
    }

    private fun getBeanDefinitionByType(type: KType): BeanDefinition {
        return this.beanDefinitionMap.filter { it.value.type == type || it.value.supertypes.contains(type) }
            .firstNotNullOf { it.value }
    }

    fun findBeanDefinitionByType(type: KClass<*>): Collection<BeanDefinition> {
        return this.beanDefinitionMap.filter {
            it.value.clazz == type || it.value.type == type.createType() || it.value.supertypes.contains(type.createType())
        }.values
    }

    fun findBeanDefinitionByAnnotation(annotationClass: KClass<*>): Collection<BeanDefinition> {
        return this.beanDefinitionMap.filter {
            it.value.annotations.any { annotation -> annotation.annotationClass == annotationClass }
        }.values
    }


    fun getBean(beanName: String): Any {
        Assert.isTrue(this.beanDefinitionNames.contains(beanName)) { MinihException("$beanName 未定义！") }
        if (this.singletonObjects.containsKey(beanName)) {
            val bean = this.singletonObjects[beanName]
            if (bean != null) {
                return bean
            }
        }
        val beanDefinition = beanDefinitionMap[beanName]
        beanDefinition?.let { it ->
            val clazz = it.clazz
            val beanConstructor = clazz.primaryConstructor ?: clazz.constructors.first()
            if (beanConstructor.parameters.isEmpty()) {
                val bean = clazz.createInstance()
                this.singletonObjects[beanName] = bean
                return bean
            }
            val params = mutableMapOf<KParameter, Any?>()
            beanConstructor.parameters.forEach { it1 ->
                val type = getBeanDefinitionByType(it1.type).beanName
                params[it1] = getBean(type)
            }
            val bean = beanConstructor.callBy(params)
            this.singletonObjects[beanName] = bean
            return bean
        }
        throw MinihException("未找到bean！")
    }


}