package cn.minih.cache.core

import cn.minih.cache.config.CacheConfig

/**
 *  缓存管理
 * @author hubin
 * @since 2023-08-11 14:26:32
 */
interface CacheManager {
    fun getCache(cacheName: String, config: CacheConfig? = null): Cache
}