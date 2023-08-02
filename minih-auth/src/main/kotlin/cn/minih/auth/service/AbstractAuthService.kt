package cn.minih.auth.service

import cn.minih.auth.cache.MinihAuthRedisManager
import cn.minih.auth.data.AuthLoginModel
import cn.minih.auth.exception.AuthLoginException
import cn.minih.core.utils.Assert
import io.vertx.kotlin.coroutines.await

/**
 * 权限服务
 * @author hubin
 * @since 2023-08-01 23:58:25
 * @desc
 */
@Suppress("unused")
abstract class AbstractAuthService : AuthService {
    override suspend fun login(params: MutableMap<String, Any>): AuthLoginModel {
        val username = params["username"]
        val password = params["password"]
        Assert.notBlank(username) { AuthLoginException("username不能为空!") }
        Assert.notBlank(password) { AuthLoginException("password不能为空!") }
        if (password != "Minih@1234") {
            throw AuthLoginException("密码不正确!")
        }
        return AuthLoginModel("admin")
    }


    override suspend fun setLoginRole(loginId: String) {
        val roles = getUserRoles(loginId)
        val redisAPI = MinihAuthRedisManager.instance.getReidApi()
        redisAPI.del(listOf("${getLoginRoleKey()}:$loginId"))
        val args = mutableListOf("${getLoginRoleKey()}:$loginId")
        args.addAll(roles)
        redisAPI.sadd(args)
    }

    override suspend fun getLoginRole(loginId: String): List<String> {
        val redisAPI = MinihAuthRedisManager.instance.getReidApi()
        return redisAPI.smembers("${getLoginRoleKey()}:$loginId").await().map { it.toString() }
    }


    abstract suspend fun getUserRoles(loginId: String): List<String>

}