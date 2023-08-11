package cn.minih.cache.config

import java.time.Duration

/**
 *  缓存配置
 * @author hubin
 * @since 2023-08-11 14:55:13
 */
interface CacheConfig {
    var ttl: Duration
    var prefix: String
}