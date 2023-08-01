package cn.minih.auth.logic

import cn.minih.auth.cache.MinihAuthRedisManager
import cn.minih.auth.config.AuthConfig
import cn.minih.auth.constants.*
import cn.minih.auth.data.AuthLoginModel
import cn.minih.auth.data.AuthSession
import cn.minih.auth.data.TokenSign
import cn.minih.auth.exception.AuthLoginException
import cn.minih.auth.logic.AuthUtil.getRandomString
import cn.minih.core.exception.IMinihErrorCode
import cn.minih.core.utils.*
import io.vertx.core.Vertx
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import java.util.*
import kotlin.reflect.full.createType

/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
object AuthLogic {

    private fun createTokenValue(): String {
        val config = getConfig("auth", AuthConfig::class)
        val token = when (config.tokenStyle) {
            TOKEN_STYLE_UUID -> UUID.randomUUID().toString()
            TOKEN_STYLE_SIMPLE_UUID -> UUID.randomUUID().toString().replace("-".toRegex(), "")
            TOKEN_STYLE_RANDOM_32 -> getRandomString(32)
            TOKEN_STYLE_RANDOM_64 -> getRandomString(64)
            TOKEN_STYLE_RANDOM_128 -> getRandomString(128)
            TOKEN_STYLE_TIK -> "${getRandomString(2)}_${getRandomString(14)}_${getRandomString(16)}__"
            TOKEN_STYLE_SNOWFLAKE -> SnowFlake.nextId().toString()
            else -> UUID.randomUUID().toString()
        }
        return token
    }

    suspend fun createLoginSession(id: String, loginConfig: AuthLoginModel): String {
        checkLoginId(id)
        val redisApi = MinihAuthRedisManager.instance.getReidApi()
        val tokenValue = distUsableToken(id, loginConfig)
        val session = getSessionByLoginId(id, true, loginConfig)!!
        updateSessionTimeout(session, loginConfig)
        val tokenSign = TokenSign(tokenValue, loginConfig.device, loginConfig.timeout)
        addTokenSign(session, tokenSign)
        redisApi.set(listOf(getTokenKey(tokenValue), id, "EX", trans(loginConfig.timeout).toString())).await()
        return tokenValue
    }

    suspend fun keepSign(token: String) {
        val redisApi = MinihAuthRedisManager.instance.getReidApi()
        val loginId = getLoginIdByToken(token)
        loginId?.notNullAndExecSuspend {
            val sessionRaw = getSessionByLoginId(loginId)
            sessionRaw?.let { session ->
                val tokenSign = session.tokenSignList.first { tokenSign -> tokenSign.token == token }
                redisApi.expire(listOf(getTokenKey(token), tokenSign.timeout.toString()))
                redisApi.expire(listOf(session.id, tokenSign.timeout.toString()))
                redisApi.set(
                    listOf(
                        getSessionActiveKey(session.loginId),
                        Date().time.toString(),
                        "EX",
                        tokenSign.timeout.toString()
                    )
                )
                Vertx.currentContext().owner()?.eventBus()
                    ?.publish(AUTH_SESSION_KEEP, jsonObjectOf("token" to token, "loginId" to loginId))
            }
        }

    }

    suspend fun checkLoginState(key: String) {
        val redisApi = MinihAuthRedisManager.instance.getReidApi()
        var count = redisApi.get(getLoginErrorKey(key))?.await()?.toInteger() ?: 0
        count++
        val config = getConfig("auth", AuthConfig::class)
        redisApi.set(listOf(getLoginErrorKey(key), count.toString(), "EX", config.loginMaxTryLockTimes.toString()))
        if (count > config.loginMaxTryTimes) {
            throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TRY_MAX_TIMES)
        }

    }

    private suspend fun updateSessionTimeout(session: AuthSession, loginConfig: AuthLoginModel) {
        val redisApi = MinihAuthRedisManager.instance.getReidApi()
        val curr = trans(redisApi.ttl(session.id).await().toLong())
        val timeout = trans(loginConfig.timeout)
        if (curr < timeout) {
            redisApi.expire(listOf(session.id, timeout.toString()))
        }
    }

    private suspend fun addTokenSign(session: AuthSession, tokenSign: TokenSign) {
        val old = session.tokenSignList.filter { it.token == tokenSign.token }
        if (old.isEmpty()) {
            session.tokenSignList.add(tokenSign)
        } else {
            old.first().token = tokenSign.token
            old.first().device = tokenSign.device
        }
        updateSession(session)
    }

    private suspend fun updateSession(session: AuthSession) {
        val redisApi = MinihAuthRedisManager.instance.getReidApi()
        val ttl = trans(redisApi.ttl(session.id).await().toLong())
        redisApi.set(listOf(session.id, session.toJsonString(), "EX", ttl.toString()))
    }

    suspend fun getTokenTimeout(token: String): Long {
        val redisApi = MinihAuthRedisManager.instance.getReidApi()
        return trans(redisApi.ttl(getTokenKey(token)).await().toLong())
    }

    private fun trans(value: Long): Long {
        return if (value == NEVER_EXPIRE) Long.MAX_VALUE else value
    }

    private suspend fun distUsableToken(id: String, loginConfig: AuthLoginModel): String {
        val isConcurrent = getConfig("auth", AuthConfig::class).isConcurrent
        if (!isConcurrent) {
            replaced(id, loginConfig)
        }
        if (isConcurrent) {
            val token = getTokenValueByLoginId(id, loginConfig.device)
            if (!token.isNullOrBlank()) {
                return token
            }
        }
        return generateUniqueToken()
    }

    private suspend fun generateUniqueToken(): String {
        val maxTryTimes = getConfig("auth", AuthConfig::class).tokenGenMaxTryTimes
        var i = 0
        while (true) {
            val token = createTokenValue()
            if (maxTryTimes == 1) {
                return token
            }
            if (getLoginIdByToken(token) == null) {
                return token
            }
            // 如果已经循环了 maxTryTimes 次，仍然没有创建出可用的 token，那么抛出异常
            if (i >= maxTryTimes) {
                throw AuthLoginException("token生成失败，已尝试" + i + "次，生成算法过于简单或资源池已耗尽")
            }
            i++
        }
    }

    /**
     * 顶人下线
     */
    private suspend fun offline(id: String, offlineType: IMinihErrorCode, device: String = "") {
        val redisApi = MinihAuthRedisManager.instance.getReidApi()
        val session = getSessionByLoginId(id)
        session?.let {
            var tokenSigns = it.tokenSignList.filter { tokenSign -> tokenSign.device == device }
            it.tokenSignList.removeIf { tokenSign -> tokenSign.device == device }
            if (device.isBlank()) {
                tokenSigns = mutableListOf<TokenSign>().apply { addAll(it.tokenSignList) }
                it.tokenSignList.clear()
            }
            updateSession(it)
            tokenSigns.forEach { tokenSign ->
                redisApi.set(
                    listOf(
                        getTokenKey(tokenSign.token),
                        offlineType.code.toString(),
                        "EX", trans(getTokenTimeout(tokenSign.token)).toString()
                    )
                )
                Vertx.currentContext().owner()?.eventBus()?.publish(
                    AUTH_SESSION_OFFLINE,
                    jsonObjectOf(
                        "token" to tokenSign.token,
                        "loginId" to it.loginId,
                        "type" to offlineType.code,
                        "msg" to offlineType.msg
                    )
                )
            }
        }
    }

    private suspend fun replaced(id: String, loginConfig: AuthLoginModel) {
        offline(id, MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_REPLACED_OUT, loginConfig.device)
    }

    suspend fun kickOut(id: String) {
        offline(id, MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_KICK_OUT)
    }

    suspend fun logout(id: String) {
        offline(id, MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_LOGOUT)
    }

    private fun checkLoginId(id: Any) {
        if (!isBasicType(id::class.createType())) {
            log.warn("loginId 应该为简单类型，例如：String | int | long，不推荐使用复杂类型：${id::class}")
        }
    }



    private suspend fun getTokenValueByLoginId(id: String, device: String): String? {
        val session = getSessionByLoginId(id)
        val tokenSigns = session?.tokenSignList?.filter { it1 ->
            it1.device == device
        } ?: emptyList()
        if (tokenSigns.isEmpty()) {
            return null
        }
        return tokenSigns.last().token
    }

    suspend fun getSessionByLoginId(
        id: String,
        isCreate: Boolean = false,
        loginConfig: AuthLoginModel = AuthLoginModel()
    ): AuthSession? {
        val key = getSessionKey(id)
        val redisApi = MinihAuthRedisManager.instance.getReidApi()
        val sessionCache = redisApi.get(key)?.await()?.toString()
        var session: AuthSession? = sessionCache?.let {
            sessionCache.jsonConvertData(AuthSession::class)
        }
        if (sessionCache == null && isCreate) {
            session = AuthSession(id = key, loginId = id)
            redisApi.set(listOf(key, session.toJsonString(), "EX", trans(loginConfig.timeout).toString()))
        }
        return session
    }

    suspend fun getLoginIdByToken(token: String): String? {
        return MinihAuthRedisManager.instance.getReidApi().get(getTokenKey(token)).await()?.toString()
    }

    private fun getTokenKey(token: String): String {

        return "${getConfig("auth", AuthConfig::class).projectName}:auth:token-session:$token"
    }

    private fun getSessionKey(id: String): String {
        return "${getConfig("auth", AuthConfig::class).projectName}:auth:login-session:$id"
    }

    private fun getSessionActiveKey(id: String): String {
        return "${getConfig("auth", AuthConfig::class).projectName}:auth:session-active:$id"
    }

    private fun getLoginErrorKey(key: String): String {
        return "${getConfig("auth", AuthConfig::class).projectName}:auth:login-error:$key"
    }
}