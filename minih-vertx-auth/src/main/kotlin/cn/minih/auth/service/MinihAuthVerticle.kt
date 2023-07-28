package cn.minih.auth.service

import cn.minih.auth.logic.AuthServiceHandler
import cn.minih.core.handler.RouteFailureHandler
import cn.minih.core.web.MinihVerticle
import io.vertx.ext.web.AllowForwardHeaders


/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
abstract class MinihAuthVerticle(port: Int = 8080) : MinihVerticle(port) {
    override suspend fun start() {
        initRouter()
        router.errorHandler(400, RouteFailureHandler.instance)
        router.errorHandler(401, RouteFailureHandler.instance)
        router.errorHandler(404, RouteFailureHandler.instance)
        router.errorHandler(405, RouteFailureHandler.instance)
        router.errorHandler(500, RouteFailureHandler.instance)
        router.allowForward(AllowForwardHeaders.FORWARD)
        router.allowForward(AllowForwardHeaders.X_FORWARD)

        router.route()
            .handler(AuthServiceHandler.instance)
        super.start()
    }

}
