@file:Suppress("unused")

package cn.minih.core.beans

import cn.minih.common.exception.MinihException
import cn.minih.common.util.Assert
import cn.minih.common.util.log
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.superclasses

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

    fun registerBean(beanName: String, bean: Any) {
        if (this.singletonObjects.containsKey(beanName)) {
            log.info("$beanName 已经初始化过，重复注册将会覆盖原有")
        }
        this.singletonObjects[beanName] = bean
    }

    fun getBeanDefinitionByType(type: KType): BeanDefinition {
        val filters = this.beanDefinitionMap.filter { it.value.type == type || it.value.supertypes.contains(type) }
        Assert.notBlank(filters) { MinihException("未找到bean定义，$type") }
        return filters.firstNotNullOf { it.value }
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
            val superClass = clazz.superclasses.firstOrNull { it.java.isInterface }
            val bean = when {
                superClass != null -> ServiceProxyFactory.getSuperProxy(superClass, clazz)
                else -> ServiceProxyFactory.getProxy(clazz)
            }
            this.singletonObjects[beanName] = bean
            return bean
        }
        throw MinihException("未找到bean！")
    }

    fun getBeanFromType(type: KType): Any {
        val definition = getBeanDefinitionByType(type)
        return getBean(definition.beanName)
    }

    fun getBeanFromClass(clazz: KClass<*>): Any {
        val definition = getBeanDefinitionByType(clazz.createType())
        return getBean(definition.beanName)
    }


}
