package cn.minih.app.system

import cn.minih.app.system.constants.SYSTEM_CONFIGURATION_FRESH
import cn.minih.app.system.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.app.system.utils.log
import io.vertx.config.ConfigChange
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
class ConfigVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val retriever = ConfigRetriever.create(
            vertx, ConfigRetrieverOptions()
                .addStore(ConfigStoreOptions().setType("env"))
                .addStore(
                    ConfigStoreOptions().setType("file").setFormat("yaml")
                        .setConfig(JsonObject().put("path", "app.yaml"))
                )
        )
        vertx.eventBus().consumer<JsonObject>(SYSTEM_CONFIGURATION_FRESH) {
            retriever.config.onSuccess { it1 ->
                it.reply(it1)
                vertx.eventBus().publish(SYSTEM_CONFIGURATION_SUBSCRIBE, it1)
            }
        }
        retriever.listen { change: ConfigChange ->
            val json: JsonObject = change.newConfiguration
            vertx.eventBus().publish(SYSTEM_CONFIGURATION_SUBSCRIBE, json)
        }
        log.info("配置中心启动完毕...")
    }
}
