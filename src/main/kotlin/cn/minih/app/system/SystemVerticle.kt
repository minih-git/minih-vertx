package cn.minih.app.system

import cn.minih.app.system.config.MinihVerticle
import cn.minih.app.system.user.UserServiceHandler
import cn.minih.app.system.utils.coroutineJsonHandler
import io.netty.handler.codec.http.HttpHeaderValues


/**
 * @author hubin
 * @date 2023/7/6
 * @desc
 */
class SystemVerticle(port: Int) : MinihVerticle(port) {
    override suspend fun initRouter() {
        router.post("/test")
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())
            .coroutineJsonHandler(UserServiceHandler::getData)
    }
}
