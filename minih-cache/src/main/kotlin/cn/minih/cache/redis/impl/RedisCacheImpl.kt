package cn.minih.cache.redis.impl

import cn.minih.cache.config.CacheConfig
import cn.minih.cache.core.Cache
import cn.minih.common.util.*
import io.vertx.core.Future
import io.vertx.redis.client.Response
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 *  redis缓存
 *  redis缓存
 *
 *  缓存更新策略 (Cache Update Strategy):
 *  1. **写入模式**: 直接覆盖 (Overwrite).
 *     每次 put/sAdd/lPush 等写操作都会直接更新 Redis 中的数据，遵循 Last-Write-Wins 原则。
 *  2. **过期策略**: 被动 TTL (Time-To-Live).
 *     写入时支持指定过期时间。如果未指定，将使用全局配置的 TTL。
 *     数据过期后由 Redis 自动删除，以此保证数据的最终一致性。
 *  3. **一致性**: 最终一致性.
 *     依赖 TTL 机制处理陈旧数据。对于强一致性要求的很多场景，建议结合数据库事务或主动失效机制(evict)使用。
 *
 * @author hubin
 * @since 2023-08-11 14:50:06
 */
class RedisCacheImpl(private val cacheName: String, private val config: CacheConfig) : Cache {


    private fun getCacheKey(name: String): String {
        var key = cacheName.plus("::").plus(name)
        config.prefix.notNullAndExec {
            key = it.plus(":").plus(key)
        }
        return key
    }


    override fun get(key: String): Future<Cache.ValueWrapper?> {
        return RedisClient.instance.get(getCacheKey(key)).compose {
            Future.succeededFuture(it?.let { Cache.DefaultValueWrapper(it) })
        }
    }


    override fun <T : Any> get(key: String, clazz: KClass<T>): Future<T?> {
        return get(key).compose {
            Future.succeededFuture(it?.let {
                when {
                    isBasicType(clazz.createType()) -> covertBasic(it.get(), clazz)
                    else -> it.get().toString().jsonConvertData(clazz)
                }
            })
        }
    }

    override fun members(key: String): Future<List<Cache.ValueWrapper>?> {
        return RedisClient.instance.smembers(getCacheKey(key)).compose {
            Future.succeededFuture(it?.let { it.map { v -> Cache.DefaultValueWrapper(v) } })
        }
    }

    override fun <T : Any> members(key: String, clazz: KClass<T>): Future<List<T>?> {
        return members(key).compose {
            Future.succeededFuture(it?.let {
                it.map { v ->
                    when {
                        isBasicType(clazz.createType()) -> covertBasic(v.get(), clazz)
                        else -> v.get().toString().jsonConvertData(clazz)
                    }
                }
            })
        }
    }

    override fun put(key: String, valueRaw: Any?, duration: Duration?): Future<Boolean> {
        if (valueRaw is List<*>) return rPush(key, valueRaw, duration)
        val value = convertData(valueRaw)
        val args = mutableListOf(getCacheKey(key), value)
        if (duration != null || config.ttl.toSeconds() != 0L) {
            args.add("EX")
            val d = duration ?: config.ttl
            args.add(d.seconds.toString())
        }
        return RedisClient.instance.set(args)
            .onFailure { Future.succeededFuture(false) }
            .compose { Future.succeededFuture(true) }
    }

    override fun sAdd(key: String, valueRaw: Any?, duration: Duration?): Future<Boolean> {
        val args = mutableListOf(getCacheKey(key))
        if (valueRaw is List<*>) {
            args.addAll(valueRaw.map(::convertData))
        } else {
            args.add(convertData(valueRaw))
        }
        return RedisClient.instance.sadd(args)
            .onFailure { Future.succeededFuture(false) }
            .compose {
                if (duration != null || config.ttl.toSeconds() != 0L) {
                    setExpire(key, duration ?: config.ttl)
                }
                Future.succeededFuture(true)
            }
    }

    override fun lPush(key: String, valueRaw: Any?, duration: Duration?): Future<Boolean> {
        val args = mutableListOf(getCacheKey(key))
        if (valueRaw is List<*>) {
            args.addAll(valueRaw.map(::convertData))
        } else {
            args.add(convertData(valueRaw))
        }
        return RedisClient.instance.lpush(args)
            .onFailure { Future.succeededFuture(false) }
            .compose {
                if (duration != null || config.ttl.toSeconds() != 0L) {
                    setExpire(key, duration ?: config.ttl)
                }
                Future.succeededFuture(true)
            }
    }

    override fun rPush(key: String, valueRaw: Any?, duration: Duration?): Future<Boolean> {
        val args = mutableListOf(getCacheKey(key))
        if (valueRaw is List<*>) {
            args.addAll(valueRaw.map(::convertData))
        } else {
            args.add(convertData(valueRaw))
        }
        return RedisClient.instance.rpush(args)
            .onFailure { Future.succeededFuture(false) }
            .compose {
                if (duration != null || config.ttl.toSeconds() != 0L) {
                    setExpire(key, duration ?: config.ttl)
                }
                Future.succeededFuture(true)
            }
    }

    override fun lRange(key: String): Future<List<Cache.ValueWrapper>?> {
        return RedisClient.instance.lrange(getCacheKey(key), "0", "-1").compose {
            Future.succeededFuture(it?.let { it.map { v -> Cache.DefaultValueWrapper(v) } })
        }
    }

    override fun <T : Any> lRange(key: String, clazz: KClass<T>): Future<List<T>?> {
        return lRange(key).compose {
            Future.succeededFuture(it?.let {
                it.map { v ->
                    when {
                        isBasicType(clazz.createType()) -> covertBasic(v.get(), clazz)
                        else -> v.get().toString().jsonConvertData(clazz)
                    }
                }
            })
        }
    }


    override fun incr(key: String): Future<Int> {
        return RedisClient.instance.incr(getCacheKey(key))
            .onFailure { Future.succeededFuture(0) }
            .compose { Future.succeededFuture(it.toInteger()) }
    }

    override fun setExpire(key: String, duration: Duration): Future<Boolean> {
        return RedisClient.instance.expire(listOf(getCacheKey(key), duration.seconds.toString()))
            .compose { Future.succeededFuture(true) }
    }

    override fun getExpire(key: String): Future<Duration> {
        return RedisClient.instance.ttl(getCacheKey(key))
            .compose { Future.succeededFuture(Duration.ofSeconds(it.toLong())) }
    }

    override fun evict(key: String): Future<Boolean> {
        return RedisClient.instance.del(listOf(getCacheKey(key)))
            .onFailure { Future.succeededFuture(false) }
            .compose { Future.succeededFuture(true) }
    }

    override fun clear(): Future<Void> {
        return RedisClient.instance.keys(getCacheKey("*")).compose {
            RedisClient.instance.del(it.map { k -> k.toString() }).compose {
                Future.succeededFuture()
            }
        }
    }

    override fun lock(key: String, duration: Duration?): Future<Boolean> {
        return RedisClient.instance.setnx(getCacheKey("$key~lock"), "1").compose {
            if (duration != null || config.ttl.toSeconds() != 0L) {
                setExpire("$key~lock", duration ?: config.ttl)
            }
            Future.succeededFuture(it.toBoolean())
        }
    }

    override fun unlock(key: String): Future<Response?> {
        return RedisClient.instance.del(listOf(getCacheKey("$key~lock")))

    }

    private fun convertData(data: Any?): String {
        return when {
            data == null -> ""
            isBasicType(data::class.createType()) -> data.toString()
            else -> data.toJsonString()
        }

    }

}
