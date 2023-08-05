@file:Suppress("unused")

package cn.minih.core.annotation

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
enum class CheckRoleType {
    AND, OR
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthCheckRole(vararg val value: String, val type: CheckRoleType = CheckRoleType.AND)