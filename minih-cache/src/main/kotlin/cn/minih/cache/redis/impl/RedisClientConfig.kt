package cn.minih.cache.redis.impl

import cn.minih.core.config.IConfig
import io.vertx.redis.client.RedisClientType


/**
 *  redis客户端配置
 * @author hubin
 * @since 2023-08-11 14:57:15
 */
data class RedisClientConfig(
    var poolSize: Int = 8,
    var connectionString: String = "",
    var host: String = "127.0.0.1",
    var port: Int = 6379,
    var username: String = "",
    var password: String = "",
    var db: Int = 0,
    var type: RedisClientType = RedisClientType.STANDALONE
) : IConfig
