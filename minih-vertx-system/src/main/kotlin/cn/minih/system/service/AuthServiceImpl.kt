package cn.minih.system.service

import cn.minih.auth.data.AuthLoginModel
import cn.minih.auth.exception.AuthLoginException
import cn.minih.auth.logic.AuthService
import cn.minih.auth.logic.RedisManager
import cn.minih.core.utils.Assert
import cn.minih.core.utils.covertTo
import cn.minih.system.data.user.SysUser
import cn.minih.system.service.user.UserRepository
import io.vertx.kotlin.coroutines.await

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
class AuthServiceImpl private constructor() : AuthService {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            AuthServiceImpl()
        }
    }

    override suspend fun login(params: MutableMap<String, Any>): AuthLoginModel {
        val username = params["username"]
        val password = params["password"]
        Assert.notBlank(username) { AuthLoginException("username不能为空!") }
        Assert.notBlank(password) { AuthLoginException("password不能为空!") }
        val user = UserRepository.instance.getUserByUsername(username.toString())?.await()?.covertTo(SysUser::class)
        Assert.notNull(user) { throw AuthLoginException("未找到用户,$username") }
        Assert.isTrue(user!!.password == password) { AuthLoginException("密码不正确!") }
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
