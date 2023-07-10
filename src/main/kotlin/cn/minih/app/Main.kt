package cn.minih.app

import cn.minih.app.system.ConfigVerticle
import cn.minih.app.system.SystemVerticle
import cn.minih.app.system.constants.SYSTEM_CONFIGURATION_FRESH
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await


suspend fun main() {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(ConfigVerticle()).await()
    val config = vertx.eventBus().request<JsonObject>(SYSTEM_CONFIGURATION_FRESH, "").await()
    vertx.deployVerticle(SystemVerticle(8080), DeploymentOptions().setConfig(config.body())).await()
}
