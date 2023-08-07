package cn.minih.auth.core

import cn.minih.auth.logic.AuthServiceHandler
import cn.minih.auth.logic.coroutineJsonHandlerHasAuth
import cn.minih.web.core.MinihWebVerticle
import cn.minih.web.handler.RouteFailureHandler
import io.vertx.ext.web.AllowForwardHeaders
import io.vertx.ext.web.Route
import kotlin.reflect.KFunction


/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
@Suppress("unused")
abstract class MinihAuthVerticle(port: Int = 8080) : MinihWebVerticle(port) {
    override suspend fun start() {
        initRouter()
        router.errorHandler(400, RouteFailureHandler.instance)
        router.errorHandler(401, RouteFailureHandler.instance)
        router.errorHandler(404, RouteFailureHandler.instance)
        router.errorHandler(405, RouteFailureHandler.instance)
        router.errorHandler(500, RouteFailureHandler.instance)
        router.allowForward(AllowForwardHeaders.FORWARD)
        router.allowForward(AllowForwardHeaders.X_FORWARD)
        router.route().handler(AuthServiceHandler.instance)
        super.start()
    }

    override fun registerRouterHandler(route: Route, fn: KFunction<Any?>) {

        route.coroutineJsonHandlerHasAuth(fn)
    }

}
