package cn.minih.cache.redis.impl

import cn.minih.cache.config.CacheConfig
import cn.minih.cache.core.Cache
import cn.minih.common.util.covertBasic
import cn.minih.common.util.isBasicType
import cn.minih.common.util.jsonConvertData
import cn.minih.common.util.toJsonString
import io.vertx.core.Future
import io.vertx.redis.client.Response
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 *  redis缓存
 * @author hubin
 * @since 2023-08-11 14:50:06
 */
class RedisCacheImpl(private val cacheName: String, private val config: CacheConfig) : Cache {


    private fun getCacheKey(name: String): String {
        return config.prefix.plus(":").plus(cacheName).plus("::").plus(name)
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

    override fun put(key: String, valueRaw: Any?): Future<Response?> {
        val value = when {
            valueRaw == null -> ""
            isBasicType(valueRaw::class.createType()) -> valueRaw.toString()
            else -> valueRaw.toJsonString()
        }
        val args = mutableListOf(getCacheKey(key), value)
        if (config.ttl != 0L) {
            args.add("EX")
            args.add(config.ttl.toString())
        }
        return RedisClient.instance.set(args)
    }

    override fun evict(key: String): Future<Response?> {
        return RedisClient.instance.del(listOf(getCacheKey(key)))
    }

    override fun clear(): Future<Void> {
        return RedisClient.instance.keys(getCacheKey("*")).compose {
            RedisClient.instance.del(it.map { k -> k.toString() })
            Future.succeededFuture()
        }
    }

    override fun lock(): Future<Boolean> {
        return RedisClient.instance.setnx(getCacheKey("~lock"), "").compose { Future.succeededFuture(it.toBoolean()) }
    }

    override fun unlock(): Future<Response?> {
        return RedisClient.instance.del(listOf(getCacheKey("~lock")))

    }


}
