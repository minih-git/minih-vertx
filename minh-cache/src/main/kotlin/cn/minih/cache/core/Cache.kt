@file:Suppress("unused")

package cn.minih.cache.core

import io.vertx.core.Future
import io.vertx.redis.client.Response
import kotlin.reflect.KClass

/**
 *  缓存
 * @author hubin
 * @since 2023-08-11 14:27:19
 */
interface Cache {
    fun get(key: String): Future<ValueWrapper?>

    fun <T : Any> get(key: String, clazz: KClass<T>): Future<T?>

    fun put(key: String, valueRaw: Any?): Future<Response?>

    fun putIfAbsent(key: String, value: Any?): Future<Void> {
        return this.get(key).compose {
            if (it != null) {
                put(key, value).compose {
                    Future.succeededFuture()
                }
            } else {
                Future.succeededFuture()
            }
        }
    }

    fun evict(key: String): Future<Response?>

    fun evictIfPresent(key: String): Future<Void> {
        return this.get(key).compose {
            if (it != null) {
                evict(key).compose {
                    Future.succeededFuture()
                }
            } else {
                Future.succeededFuture()
            }
        }

    }

    fun clear(): Future<Void>

    fun lock(): Future<Boolean>

    fun unlock(): Future<Response?>

    interface ValueWrapper {
        fun get(): Any
    }

    class DefaultValueWrapper(private val data: Any) : ValueWrapper {
        override fun get(): Any {
            return data
        }
    }


}
