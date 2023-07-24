package cn.minih.core.repository

import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions
import java.util.concurrent.ThreadLocalRandom

/**
 * @author hubin
 * @date 2023/7/8
 * @desc
 */
class RedisManager private constructor() {

    private val pools: MutableList<RedisAPI> = mutableListOf()
    private var poolSize: Int = 8

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RedisManager()
        }
    }


    init {
        val config = Vertx.currentContext().config()
        poolSize = (config.getInteger("redis.poolSize") ?: 8)

        for (i in 1..poolSize) {
            val redisOption = RedisOptions()
                .addConnectionString(config.getString("redis.connectionString"))
                .setMaxPoolSize(512)
                .setMaxPoolWaiting(1512)

            pools.add(RedisAPI.api(Redis.createClient(Vertx.currentContext().owner(), redisOption)))
        }
    }

    fun getReidApi(): RedisAPI {
        return try {
            pools[ThreadLocalRandom.current().nextInt(poolSize)]
        }catch (_:Exception){
            pools[0]
        }
    }

}
