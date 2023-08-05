package cn.minih.auth.service

import cn.minih.auth.config.AuthConfig
import cn.minih.auth.data.AuthLoginModel
import cn.minih.common.util.getConfig

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
        return "${getConfig("auth", AuthConfig::class).projectName}:auth:login-role:"
    }

}