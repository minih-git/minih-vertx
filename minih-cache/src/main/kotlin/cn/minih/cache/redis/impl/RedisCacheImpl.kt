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

    override fun lock(duration: Duration?): Future<Boolean> {
        return RedisClient.instance.setnx(getCacheKey("~lock"), "1").compose {
            if (duration != null || config.ttl.toSeconds() != 0L) {
                setExpire("~lock", duration ?: config.ttl)
            }
            Future.succeededFuture(it.toBoolean())
        }
    }

    override fun unlock(): Future<Response?> {
        return RedisClient.instance.del(listOf(getCacheKey("~lock")))

    }

    private fun convertData(data: Any?): String {
        return when {
            data == null -> ""
            isBasicType(data::class.createType()) -> data.toString()
            else -> data.toJsonString()
        }

    }

}
