package cn.minih.app

import cn.minih.app.system.SystemVerticle
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await

suspend fun main() {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(SystemVerticle()).await()
}
