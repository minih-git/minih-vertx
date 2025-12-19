@file:Suppress("unused")

package cn.minih.cache.core

import io.vertx.core.Future
import io.vertx.redis.client.Response
import java.time.Duration
import kotlin.reflect.KClass

/**
 *  缓存
 * @author hubin
 * @since 2023-08-11 14:27:19
 */
interface Cache {
    fun get(key: String): Future<ValueWrapper?>
    fun <T : Any> get(key: String, clazz: KClass<T>): Future<T?>
    fun members(key: String): Future<List<ValueWrapper>?>
    fun <T : Any> members(key: String, clazz: KClass<T>): Future<List<T>?>
    fun put(key: String, valueRaw: Any?, duration: Duration? = null): Future<Boolean>
    fun sAdd(key: String, valueRaw: Any?, duration: Duration? = null): Future<Boolean>
    fun lPush(key: String, valueRaw: Any?, duration: Duration? = null): Future<Boolean>
    fun rPush(key: String, valueRaw: Any?, duration: Duration? = null): Future<Boolean>
    fun lRange(key: String): Future<List<ValueWrapper>?>
    fun <T : Any> lRange(key: String, clazz: KClass<T>): Future<List<T>?>
    fun incr(key: String): Future<Int>
    fun setExpire(key: String, duration: Duration): Future<Boolean>
    fun getExpire(key: String): Future<Duration>
    fun evict(key: String): Future<Boolean>
    fun clear(): Future<Void>
    fun lock(key: String = "", duration: Duration? = null): Future<Boolean>
    fun unlock(key: String = ""): Future<Response?>

    fun putIfAbsent(key: String, value: Any?, duration: Duration? = null): Future<Void> {
        return this.get(key).compose {
            if (it == null) {
                put(key, value, duration).compose {
                    Future.succeededFuture()
                }
            } else {
                Future.failedFuture("exist")
            }
        }
    }

    fun updateOrPut(key: String, value: Any?, duration: Duration? = null): Future<Void> {
        return this.get(key).compose {
            if (it != null) {
                getExpire(key).compose { d ->
                    put(key, value, duration ?: d).compose {
                        Future.succeededFuture()
                    }
                }
            } else {
                put(key, value, duration).compose {
                    Future.succeededFuture()
                }
            }
        }
    }

    fun evictIfPresent(key: String): Future<Boolean> {
        return this.get(key).compose {
            if (it != null) {
                evict(key)
            } else {
                Future.succeededFuture()
            }
        }

    }

    interface ValueWrapper {
        fun get(): Any
    }

    class DefaultValueWrapper(private val data: Any) : ValueWrapper {
        override fun get(): Any {
            return data
        }
    }


}
