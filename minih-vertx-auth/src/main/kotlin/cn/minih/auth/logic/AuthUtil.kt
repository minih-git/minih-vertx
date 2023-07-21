package cn.minih.auth.logic

import cn.minih.auth.constants.*
import cn.minih.auth.data.AuthConfig
import cn.minih.auth.data.AuthLoginModel
import cn.minih.auth.data.TokenInfo
import cn.minih.auth.exception.AuthLoginException
import cn.minih.core.utils.jsonConvertData
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.shareddata.AsyncMap
import io.vertx.kotlin.coroutines.await
import java.util.concurrent.ThreadLocalRandom

/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
object AuthUtil {
    private val shareConfig: Future<AsyncMap<String, Any>>
        get() {
            val sharedData = Vertx.currentContext().owner().sharedData()
            return sharedData.getLocalAsyncMap("auth")
        }

    suspend fun getConfig(): AuthConfig {
        return shareConfig.await().entries().await().toString().jsonConvertData(AuthConfig::class)
    }

    fun getRandomString(length: Int): String {
        val str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sb = StringBuilder()
        for (i in 0..<length) {
            val number = ThreadLocalRandom.current().nextInt(62)
            sb.append(str[number])
        }
        return sb.toString()
    }

    suspend fun login(
        id: String,
        device: String? = DEFAULT_DEVICE,
        timeout: Long = -1L,
        loginConfig: AuthLoginModel = AuthLoginModel()
    ): TokenInfo {
        val config = getConfig()
        loginConfig.device = device ?: DEFAULT_DEVICE
        loginConfig.timeout = if (timeout == -1L) config.timeout else timeout
        val tokenValue = AuthLogic.createLoginSession(id, loginConfig)
        return TokenInfo(
            tokenValue = tokenValue,
            tokenName = config.tokenName,
            loginId = id,
            expired = AuthLogic.getTokenTimeout(tokenValue),
            loginDevice = loginConfig.device,
            tokenPrefix = config.tokenPrefix
        )
    }

    suspend fun checkLogin(token: String?): String {
        val config = getConfig()
        if (token.isNullOrBlank()) {
            throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_NO_TOKEN)
        }
        if (!token.startsWith(config.tokenPrefix + TOKEN_CONNECTOR_CHAT)) {
            throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_NO_TOKEN)
        }
        val tokenValue = token.substring(config.tokenPrefix.length + TOKEN_CONNECTOR_CHAT.length)

        val loginId = AuthLogic.getLoginIdByToken(tokenValue)
        if (loginId.isNullOrBlank()) {
            throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_INVALID)
        }
        when (loginId) {
            MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_TIMEOUT.code.toString() -> throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_TIMEOUT)
            MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_KICK_OUT.code.toString() -> throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_KICK_OUT)
            MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_LOGOUT.code.toString() -> throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_LOGOUT)
            MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_REPLACED_OUT.code.toString() -> throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_REPLACED_OUT)
            MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_FREEZE.code.toString() -> throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_FREEZE)
        }
        if (config.autoKeepSign) {
            AuthLogic.keepSign(tokenValue)
        }
        return loginId
    }

    suspend fun getOnline(loginId: String): Int {
        val session = AuthLogic.getSessionByLoginId(loginId) ?: return 0
        val tokens = session.tokenSignList
        if (tokens.isEmpty()) return 0
        tokens.forEach {
            val token = it.token
            if (AuthLogic.getLoginIdByToken(token) == loginId) {
                return 1
            }
        }
        return 0
    }

    fun getCurrentLoginId(): String {
        return try {
            Vertx.currentContext().get(CONTEXT_LOGIN_ID)
        } catch (e: Exception) {
            throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_NO_TOKEN)
        }
    }

    fun currentIsSysAdmin(): Boolean {
        return Vertx.currentContext().get(CONTEXT_IS_SYSTEM_ADMIN)
    }
}