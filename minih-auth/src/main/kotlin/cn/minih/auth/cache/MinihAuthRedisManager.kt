package cn.minih.auth.cache

import cn.minih.auth.config.AuthConfig
import cn.minih.auth.exception.MinihAuthException
import cn.minih.core.utils.Assert
import cn.minih.core.utils.getConfig
import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions
import java.util.concurrent.ThreadLocalRandom

/**
 * 权限缓存redis管理
 * @author hubin
 * @since 2023-07-30 22:35:12
 * @desc
 */
class MinihAuthRedisManager private constructor() {

    private val pools: MutableList<RedisAPI> = mutableListOf()
    private var poolSize: Int = 8

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            MinihAuthRedisManager()
        }
    }


    init {
        val config = getConfig("auth", AuthConfig::class)
        poolSize = (config.cache.poolSize ?: 8)
        Assert.notBlank(config.cache.connectionString) {
            throw MinihAuthException("请设置权限系统的缓存！")
        }

        for (i in 1..poolSize) {
            val redisOption = RedisOptions()
                .addConnectionString(config.cache.connectionString)
                .setMaxPoolSize(512)
                .setMaxPoolWaiting(1512)

            pools.add(RedisAPI.api(Redis.createClient(Vertx.currentContext().owner(), redisOption)))
        }
    }

    fun getReidApi(): RedisAPI {
        return try {
            pools[ThreadLocalRandom.current().nextInt(poolSize)]
        } catch (_: Exception) {
            pools[0]
        }
    }

}