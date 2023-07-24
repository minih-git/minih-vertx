package cn.minih.auth.service

import cn.minih.auth.logic.AuthServiceHandler
import cn.minih.core.handler.RouteFailureHandler
import cn.minih.core.web.MinihVerticle
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.AllowForwardHeaders
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler


/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
abstract class MinihAuthVerticle(port: Int = 8080) : MinihVerticle(port) {
    override suspend fun start() {
        initRouter()
        val sockJSHandler = SockJSHandler.create(vertx)
        val options = SockJSBridgeOptions()
        options.addInboundPermitted(PermittedOptions().setAddressRegex("cn.minih.auth.*"))
        options.addOutboundPermitted(PermittedOptions().setAddressRegex("cn.minih.auth.*"))
        router.route("/ws/authEventbus/*")
            .subRouter(sockJSHandler.bridge(options))
        router.errorHandler(400, RouteFailureHandler.instance)
        router.errorHandler(401, RouteFailureHandler.instance)
        router.errorHandler(404, RouteFailureHandler.instance)
        router.errorHandler(405, RouteFailureHandler.instance)
        router.errorHandler(500, RouteFailureHandler.instance)
        router.allowForward(AllowForwardHeaders.FORWARD)
        router.allowForward(AllowForwardHeaders.X_FORWARD)

        router.route()
            .handler(AuthServiceHandler.instance)
        initRouterHandler()
        super.start()
    }

}