package cn.minih.app

import cn.minih.app.system.UserVerticle
import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(UserVerticle())


}