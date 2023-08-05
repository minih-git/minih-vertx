@file:Suppress("unused")

package cn.minih.core.annotation

/**
 * 服务注解
 * @author hubin
 * @since 2023-08-05 19:34:19
 * @desc
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Service(val value: String = "")