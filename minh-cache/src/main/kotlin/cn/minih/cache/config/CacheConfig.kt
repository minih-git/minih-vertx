package cn.minih.cache.config

/**
 *  缓存配置
 * @author hubin
 * @since 2023-08-11 14:55:13
 */
interface CacheConfig {
    var ttl: Long
    var prefix: String
}
