package cn.minih.core.annotation

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Component(val value: String = "")