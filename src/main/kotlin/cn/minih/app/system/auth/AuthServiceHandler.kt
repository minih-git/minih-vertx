package cn.minih.app.system.auth

import cn.hutool.core.lang.Assert
import cn.minih.app.system.exception.PasswordErrorException
import cn.minih.app.system.user.UserRepository
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.json.JsonObject

val log: KLogger = KotlinLogging.logger {}

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class AuthServiceHandler(private val userRepository: UserRepository) {
    suspend fun login(params: JsonObject) {
        val username = params.getString("username")
        val password = params.getString("password")
        val device = params.getString("device") ?: "PC"
        Assert.notBlank(username) { IllegalArgumentException("username不能为空!") }
        Assert.notBlank(password) { IllegalArgumentException("password不能为空!") }
        log.info("$username 开始登录...")
        val user = userRepository.getUserByUsername(username)
        Assert.notNull(user) { IllegalArgumentException("${username}用户未找到!") }
        Assert.isTrue(user?.getString("password") == password) { PasswordErrorException() }
        log.info("$username 登录成功...")
    }
}