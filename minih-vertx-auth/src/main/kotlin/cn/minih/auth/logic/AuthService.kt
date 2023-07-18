package cn.minih.auth.logic

import cn.minih.auth.data.AuthLoginModel

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
interface AuthService {
    val loginRoleKey: String get() = ":auth:login-role:"

    suspend fun login(params: MutableMap<String, Any>): AuthLoginModel
    suspend fun setLoginRole(loginId: String)
    suspend fun getLoginRole(loginId: String): List<String>

}
