@file:Suppress("unused")

package cn.minih.core.annotation

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Component(val value: String = "")

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Configuration(val value: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Bean(val value: String = "")