package cn.minih.app.system.auth.annotation

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthCheckRoleAnd(vararg val value: String)