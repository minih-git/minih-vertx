package cn.minih.cache.core

/**
 *  缓存管理
 * @author hubin
 * @since 2023-08-11 14:26:32
 */
interface CacheManager {
    fun getCache(cacheName: String): Cache
}
