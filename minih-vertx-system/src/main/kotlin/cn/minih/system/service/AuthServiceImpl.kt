package cn.minih.system.service

import cn.minih.auth.data.AuthLoginModel
import cn.minih.auth.exception.AuthLoginException
import cn.minih.auth.logic.AuthService
import cn.minih.core.constants.SMS_REDIS_KEY_PREFIX
import cn.minih.core.repository.QueryWrapper
import cn.minih.core.repository.RedisManager
import cn.minih.core.utils.Assert
import cn.minih.core.utils.decrypt
import cn.minih.core.utils.getConfig
import cn.minih.system.data.user.SysUser
import cn.minih.system.service.user.UserRepository
import io.vertx.kotlin.coroutines.await
import org.mindrot.jbcrypt.BCrypt

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
        var password = params["password"]
        val mobile = params["mobile"]
        val code = params["code"]
        var user: SysUser? = null
        if (username is String && username.isNotBlank()) {
            Assert.notBlank(password) { AuthLoginException("password不能为空!") }
            val config = getConfig()
            password = decrypt(password.toString(), config.aesSecret)
            user = UserRepository.instance.getUserByUsername(username.toString()).await()
            Assert.isTrue(BCrypt.checkpw(password.toString(), user!!.password)) { AuthLoginException("密码错误!") }
        }
        if (mobile is String && mobile.isNotBlank()) {
            Assert.notBlank(code) { AuthLoginException("短信验证码不能为空!") }
            val redisApi = RedisManager.instance.getReidApi()
            val rawCode = redisApi.get(SMS_REDIS_KEY_PREFIX + mobile)?.await()?.toString()
            Assert.isTrue(rawCode == code) { AuthLoginException("验证码错误!") }
            user = UserRepository.instance.findOne(QueryWrapper<SysUser>().eq(SysUser::mobile, mobile)).await()

        }
        user?.let {
            Assert.notNull(it) { throw AuthLoginException("未找到用户,$username") }
            Assert.isTrue(it.state == 1) { AuthLoginException("账号已被永久封禁!") }
            return AuthLoginModel(it.username)
        }
        throw AuthLoginException("未找到用户,$username")
    }

    override suspend fun setLoginRole(loginId: String) {
        val user = UserRepository.instance.getUserByUsername(loginId).await()
        Assert.notNull(user) { throw AuthLoginException("未找到用户!") }
        if (user!!.role.isEmpty()) {
            return
        }
        val redisAPI = RedisManager.instance.getReidApi()
        redisAPI.del(listOf("${getLoginRoleKey()}:$loginId"))
        val args = mutableListOf("${getLoginRoleKey()}:$loginId")
        args.addAll(user.role)
        redisAPI.sadd(args)

    }

    override suspend fun getLoginRole(loginId: String): List<String> {
        val redisAPI = RedisManager.instance.getReidApi()
        return redisAPI.smembers("${getLoginRoleKey()}:$loginId").await().map { it.toString() }
    }


}
