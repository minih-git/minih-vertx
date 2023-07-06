package cn.minih.app.system

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router

/**
 * @author hubin
 * @date 2023/7/6
 * @desc
 */
class UserVerticle : AbstractVerticle() {
    override fun start() {
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        router.get("/user").respond {
            it.response().end("hello user")
        }
        server.requestHandler(router).listen(8080)
    }
}