package cn.minih.auth.service

import cn.minih.auth.constants.LOGIN_USER_ROLES_CACHE_KEY
import cn.minih.auth.data.AuthLoginModel
import cn.minih.auth.exception.AuthLoginException
import cn.minih.cache.core.CacheManager
import cn.minih.cache.redis.impl.RedisCacheManagerImpl
import cn.minih.common.util.Assert
import io.vertx.kotlin.coroutines.await

/**
 * 权限服务
 * @author hubin
 * @since 2023-08-01 23:58:25
 * @desc
 */
@Suppress("unused")
abstract class AbstractAuthService : AuthService {
    private val cacheManager: CacheManager by lazy {
        RedisCacheManagerImpl()
    }

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
        val cache = cacheManager.getCache(LOGIN_USER_ROLES_CACHE_KEY)
        cache.evict(loginId).await()
        cache.put(loginId, roles)
    }

    override suspend fun getLoginRole(loginId: String): List<String> {
        val cache = cacheManager.getCache(LOGIN_USER_ROLES_CACHE_KEY)
        return cache.members(loginId, String::class).await() ?: emptyList()
    }


    abstract suspend fun getUserRoles(loginId: String): List<String>

}