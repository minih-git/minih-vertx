package cn.minih.app.system.auth

import cn.minih.app.system.constants.SYSTEM_AME

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
interface AuthService {
    val loginRoleKey: String get() = "$SYSTEM_AME:auth:login-role:"

    suspend fun login(params: MutableMap<String, Any>): String
    suspend fun setLoginRole(loginId: String)
    suspend fun getLoginRole(loginId: String): List<String>

}