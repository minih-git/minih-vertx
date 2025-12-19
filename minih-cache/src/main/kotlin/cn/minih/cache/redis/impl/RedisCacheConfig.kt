package cn.minih.cache.redis.impl

import cn.minih.cache.config.CacheConfig
import java.time.Duration

/**
 *  redis缓存配置
 * @author hubin
 * @since 2023-08-11 16:16:25
 */
data class RedisCacheConfig(
    override var ttl: Duration = Duration.ofSeconds(0),
    override var prefix: String = ""
) : CacheConfig