@file:Suppress("unused")

package cn.minih.cache.redis.impl

import cn.minih.cache.config.CacheConfig
import cn.minih.cache.core.Cache
import cn.minih.cache.core.CacheManager
import java.util.concurrent.ConcurrentHashMap

/**
 *  redis缓存管理器实现
 * @author hubin
 * @since 2023-08-11 14:50:40
 */
class RedisCacheManagerImpl : CacheManager {

    private val instanceMap = ConcurrentHashMap<String, Cache>()

    private val configMap = ConcurrentHashMap<String, CacheConfig>()
    override fun getCache(cacheName: String): Cache {
        return instanceMap[cacheName] ?: let {
            var cache: Cache = RedisCacheImpl(cacheName, getConfig(cacheName))
            instanceMap.putIfAbsent(cacheName, cache)?.let {
                cache = it
            }
            cache
        }
    }

    private fun getConfig(cacheName: String): CacheConfig {
        return configMap[cacheName] ?: let {
            var config: CacheConfig = RedisCacheConfig()
            configMap.putIfAbsent(cacheName, config)?.let {
                config = it
            }
            config
        }
    }
}
