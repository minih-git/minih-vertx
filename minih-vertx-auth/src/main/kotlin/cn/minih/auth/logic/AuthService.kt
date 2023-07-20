package cn.minih.auth.logic

import cn.minih.auth.data.AuthLoginModel

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
interface AuthService {
    suspend fun login(params: MutableMap<String, Any>): AuthLoginModel
    suspend fun setLoginRole(loginId: String)
    suspend fun getLoginRole(loginId: String): List<String>

    suspend fun getLoginRoleKey(): String {
        return "${AuthUtil.getConfig().projectName}:auth:login-role:"
    }

}
