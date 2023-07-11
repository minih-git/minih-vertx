package cn.minih.app.system.auth

import cn.minih.app.system.auth.data.AuthLoginModel
import cn.minih.app.system.config.RedisManager
import cn.minih.app.system.exception.AuthLoginException
import cn.minih.app.system.user.UserRepository
import cn.minih.app.system.user.data.SysUser
import cn.minih.app.system.utils.Assert
import cn.minih.app.system.utils.covertTo
import io.vertx.kotlin.coroutines.await

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
class DefaultAuthServiceImpl private constructor() : AuthService {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DefaultAuthServiceImpl()
        }
    }

    override suspend fun login(params: MutableMap<String, Any>): AuthLoginModel {
        val username = params["username"]
        val password = params["password"]
        Assert.notBlank(username) { AuthLoginException("username不能为空!") }
        Assert.notBlank(password) { AuthLoginException("password不能为空!") }
        val user = UserRepository.instance.getUserByUsername(username.toString())?.await()?.covertTo(SysUser::class)
        Assert.notNull(user) { throw AuthLoginException("未找到用户,$username") }
        if (user!!.password != password) {
            throw AuthLoginException("密码不正确!")
        }
        return AuthLoginModel(user.username)
    }

    override suspend fun setLoginRole(loginId: String) {
        val user = UserRepository.instance.getUserByUsername(loginId)?.await()?.covertTo(SysUser::class)
        Assert.notNull(user) { throw AuthLoginException("未找到用户!") }
        if (user!!.role.isEmpty()) {
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