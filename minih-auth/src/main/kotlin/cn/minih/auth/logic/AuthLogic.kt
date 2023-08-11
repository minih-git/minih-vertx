package cn.minih.auth.logic

import cn.minih.auth.config.AuthConfig
import cn.minih.auth.constants.*
import cn.minih.auth.data.AuthLoginModel
import cn.minih.auth.data.AuthSession
import cn.minih.auth.data.TokenSign
import cn.minih.auth.exception.AuthLoginException
import cn.minih.auth.logic.AuthUtil.getRandomString
import cn.minih.cache.core.CacheManager
import cn.minih.cache.redis.impl.RedisCacheConfig
import cn.minih.cache.redis.impl.RedisCacheManagerImpl
import cn.minih.common.exception.IMinihErrorCode
import cn.minih.common.util.getConfig
import cn.minih.common.util.notNullAndExecSuspend
import cn.minih.core.util.SnowFlakeContext
import io.vertx.core.Vertx
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import java.time.Duration
import java.util.*

/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
object AuthLogic {

    private val cacheManager: CacheManager by lazy {
        RedisCacheManagerImpl().defaultConfig(RedisCacheConfig(ttl = Duration.ofSeconds(7200)))
    }

    private fun createTokenValue(): String {
        val config = getConfig("auth", AuthConfig::class)
        val token = when (config.tokenStyle) {
            TOKEN_STYLE_UUID -> UUID.randomUUID().toString()
            TOKEN_STYLE_SIMPLE_UUID -> UUID.randomUUID().toString().replace("-".toRegex(), "")
            TOKEN_STYLE_RANDOM_32 -> getRandomString(32)
            TOKEN_STYLE_RANDOM_64 -> getRandomString(64)
            TOKEN_STYLE_RANDOM_128 -> getRandomString(128)
            TOKEN_STYLE_TIK -> "${getRandomString(2)}_${getRandomString(14)}_${getRandomString(16)}__"
            TOKEN_STYLE_SNOWFLAKE -> SnowFlakeContext.instance.currentContext().nextId().toString()
            else -> UUID.randomUUID().toString()
        }
        return token
    }

    suspend fun createLoginSession(id: String, loginConfig: AuthLoginModel): String {
        val tokenValue = distUsableToken(id, loginConfig)
        val session = getSessionByLoginId(id, true, loginConfig)!!
        updateSessionTimeout(session, loginConfig)
        val tokenSign = TokenSign(tokenValue, loginConfig.device, loginConfig.timeout)
        addTokenSign(session, tokenSign)
        val cache = cacheManager.getCache(TOKEN_VALUE_CACHE_KEY)
        cache.put(tokenValue, id, Duration.ofSeconds(loginConfig.timeout)).await()
        return tokenValue
    }

    suspend fun keepSign(token: String) {
        val loginId = getLoginIdByToken(token)
        loginId?.notNullAndExecSuspend {
            val sessionRaw = getSessionByLoginId(loginId)
            sessionRaw?.let { session ->
                val tokenSign = session.tokenSignList.first { tokenSign -> tokenSign.token == token }
                val cache = cacheManager.getCache(TOKEN_VALUE_CACHE_KEY)
                cache.setExpire(token, Duration.ofSeconds(tokenSign.timeout))
                val cache1 = cacheManager.getCache(LOGIN_SESSION_CACHE_KEY)
                cache1.setExpire(session.loginId, Duration.ofSeconds(tokenSign.timeout))
                val cache2 = cacheManager.getCache(SESSION_ACTIVE_CACHE_KEY)
                cache2.put(session.loginId, Date().time, Duration.ofSeconds(tokenSign.timeout))
                Vertx.currentContext().owner()?.eventBus()
                    ?.publish(AUTH_SESSION_KEEP, jsonObjectOf("token" to token, "loginId" to loginId))
            }
        }

    }

    suspend fun checkLoginState(key: String) {
        val cache = cacheManager.getCache(LOGIN_ERR_COUNT_CACHE_KEY)
        val count = cache.incr(key).await()
        val config = getConfig("auth", AuthConfig::class)
        if (count > config.loginMaxTryTimes) {
            cache.setExpire(key, Duration.ofSeconds(config.loginMaxTryLockTimes))
            throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TRY_MAX_TIMES)
        }
    }

    private suspend fun updateSessionTimeout(session: AuthSession, loginConfig: AuthLoginModel) {
        val cache = cacheManager.getCache(LOGIN_SESSION_CACHE_KEY)
        val curr = trans(cache.getExpire(session.loginId).await().toSeconds())
        val timeout = trans(loginConfig.timeout)
        if (curr < timeout) {
            cache.setExpire(session.loginId, Duration.ofSeconds(timeout))
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
        val cache = cacheManager.getCache(LOGIN_SESSION_CACHE_KEY)
        val curr = trans(cache.getExpire(session.loginId).await().toSeconds())
        cache.put(session.loginId, session, Duration.ofSeconds(curr))
    }

    suspend fun getTokenTimeout(token: String): Long {
        val cache = cacheManager.getCache(TOKEN_VALUE_CACHE_KEY)
        val curr = trans(cache.getExpire(token).await().toSeconds())
        return trans(curr)
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
        val cache = cacheManager.getCache(TOKEN_VALUE_CACHE_KEY)
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
                cache.putIfAbsent(tokenSign.token, offlineType.code)
                Vertx.currentContext().owner()?.eventBus()?.publish(
                    AUTH_SESSION_OFFLINE, jsonObjectOf(
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
        val sessionCache = cacheManager.getCache(LOGIN_SESSION_CACHE_KEY)
        var session = sessionCache.get(id, AuthSession::class).await()
        if (session == null && isCreate) {
            session = AuthSession(id = "session_$id", loginId = id)
            sessionCache.put(id, session, Duration.ofSeconds(loginConfig.timeout))
        }
        return session
    }

    suspend fun getLoginIdByToken(token: String): String? {
        val cache = cacheManager.getCache(TOKEN_VALUE_CACHE_KEY)
        return cache.get(token, String::class).await()
    }
}