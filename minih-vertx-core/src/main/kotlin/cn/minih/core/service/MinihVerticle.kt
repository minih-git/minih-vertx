package cn.minih.core.service

import cn.minih.core.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.await

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
interface MinihVerticle : Verticle {
    public suspend fun initConfig() {
        val retriever = ConfigRetriever.create(
            vertx, ConfigRetrieverOptions()
                .addStore(ConfigStoreOptions().setType("env").setFormat("json"))
                .addStore(
                    ConfigStoreOptions().setType("file").setFormat("yaml")
                        .setConfig(JsonObject().put("path", "app.yaml"))
                )
        )

        retriever.config.await().forEach {
            val config = Vertx.currentContext().config()
            if (it.key.contains(".")) {
                val key = it.key.substring(0, it.key.indexOf("."))
                val subKey = it.key.substring(it.key.indexOf(".") + 1)
                val value = it.value
                val map = config.get<JsonObject>(key) ?: JsonObject()
                map.put(subKey, value)
                config.put(key, map)
            }
            config.put(it.key, it.value)
        }
        vertx.eventBus().consumer<JsonObject>(SYSTEM_CONFIGURATION_SUBSCRIBE).handler { config ->
            config.body().forEach {
                Vertx.currentContext().config().put(it.key, it.value)
            }
        }
    }
}
