package cn.minih.core.components

import cn.minih.core.annotation.Component
import cn.minih.core.annotation.ComponentScan
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.core.beans.BeanDefinition
import cn.minih.core.beans.BeanFactory
import cn.minih.core.utils.getClassesByPath
import io.vertx.core.Vertx
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
object MinihServiceRun {
    fun run(clazz: KClass<*>) {
        val vertx = Vertx.vertx()
        val componentsList = getClassesByPath("cn.minih")
        if (clazz.hasAnnotation<ComponentScan>()) {
            val componentScan = clazz.findAnnotation<ComponentScan>()
            if (componentScan != null && componentScan.basePacakge.isNotBlank()) {
                componentsList.addAll(getClassesByPath(componentScan.basePacakge))
            }
        }
        componentsList.forEach {
            if (hasComponentsAnnotation(it) && it.simpleName != null) {
                var beanName = it.simpleName!!
                clazz.findAnnotation<Component>()?.let { con ->
                    if (con.value.isNotBlank()) {
                        beanName = con.value
                    }
                }
                val beanDefinition = BeanDefinition(it, it.createType(), it.annotations, beanName)
                BeanFactory.instance.registerBeanDefinition(beanName, beanDefinition)
            }
        }
        val services = BeanFactory.instance.findBeanDefinitionByAnnotation(MinihServiceVerticle::class)
        println(services)

    }

    private fun hasComponentsAnnotation(clazz: KClass<*>): Boolean {
        try {
            return clazz.hasAnnotation<Component>() || clazz.hasAnnotation<MinihServiceVerticle>()
        } catch (_: UnsupportedOperationException) {
        }
        return false
    }

}
