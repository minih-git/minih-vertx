package cn.minih.app.system

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
                .addStore(
                    ConfigStoreOptions().setType("file").setFormat("yaml")
                        .setConfig(
                            JsonObject().put(
                                "path",
                                "/Users/hubin/IdeaProjects/minih-vertx/src/main/resources/app.yaml"
                            )
                        )
                )
        )
        retriever.listen { change: ConfigChange ->
            vertx.eventBus().publish(SYSTEM_CONFIGURATION_SUBSCRIBE, change.newConfiguration)
        }
        log.info("配置中心启动完毕...")
    }
}
