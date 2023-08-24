package cn.minih.cache.redis.impl

import cn.minih.common.exception.MinihException
import cn.minih.common.util.getConfig
import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions

/**
 * 权限缓存redis管理
 * @author hubin
 * @since 2023-07-30 22:35:12
 * @desc
 */
class RedisClient private constructor() {
    companion object {
        val instance: RedisAPI by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            val config = getConfig("redis", RedisClientConfig::class)
            if (config.connectionString.isBlank() && config.host.isBlank()) {
                throw MinihException("请配置redis连接信息！")
            }
            var connectionString = config.connectionString
            if (connectionString.isBlank()) {
                if (config.username.isNotBlank()) {
                    connectionString = connectionString.plus(config.username)
                }
                if (config.password.isNotBlank()) {
                    connectionString = connectionString.plus(":").plus(config.password)
                }
                connectionString = connectionString.plus("@").plus(config.host)
                connectionString = connectionString.plus(":").plus(config.port)
                connectionString = connectionString.plus("/").plus(config.db)
            }
            val redisOption = RedisOptions()
                .addConnectionString(connectionString)
                .setMaxPoolSize(config.poolSize)
                .setMaxPoolWaiting(config.poolSize * 100)
                .setType(config.type)
            RedisAPI.api(Redis.createClient(Vertx.currentContext().owner(), redisOption))
        }
    }


}
