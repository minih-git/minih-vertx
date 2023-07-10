package cn.minih.app.system.auth

import cn.hutool.core.lang.Assert
import cn.minih.app.system.config.RedisManager
import cn.minih.app.system.exception.AuthLoginException
import cn.minih.app.system.user.UserRepository
import io.vertx.kotlin.coroutines.await

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
class DefaultAuthServiceImpl private constructor(): AuthService {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DefaultAuthServiceImpl()
        }
    }

    private val userRepository = UserRepository.instance
    override suspend fun login(params: MutableMap<String, Any>): String {
        val username = params["username"].toString()
        val password = params["password"].toString()
        Assert.notBlank(username) { AuthLoginException("username不能为空!") }
        Assert.notBlank(password) { AuthLoginException("password不能为空!") }
        val user = userRepository.getUserByUsername(username) ?: throw AuthLoginException("未找到用户!")
        if (user.password != password) {
            throw AuthLoginException("密码不正确!")
        }
        return user.username
    }

    override suspend fun setLoginRole(loginId: String) {
        val user = userRepository.getUserByUsername(loginId) ?: throw AuthLoginException("未找到用户!")
        if (user.role.isEmpty()) {
            return
        }
        val redisAPI = RedisManager.instance.getReidApi()
        val args = mutableListOf("$loginRoleKey:$loginId")
        args.addAll(user.role)
        redisAPI.sadd(args)
    }

    override suspend fun getLoginRole(loginId: String): List<String> {
        val redisAPI = RedisManager.instance.getReidApi()
        return redisAPI.smembers("$loginRoleKey:$loginId").await().map { it.toString() }
    }


}