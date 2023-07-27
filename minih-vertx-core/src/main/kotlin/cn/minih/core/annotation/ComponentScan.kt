package cn.minih.core.annotation

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class ComponentScan(val basePackage: String = "")