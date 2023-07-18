package cn.minih.auth.service

import cn.minih.auth.components.RouteFailureHandler
import cn.minih.auth.logic.AuthServiceHandler
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle


/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
abstract class MinihAuthVerticle(private val port: Int = 8080) : CoroutineVerticle() {
    val router: Router
        get() {
            return this.routerInstance
        }
    private lateinit var routerInstance: Router


    override suspend fun start() {
        this.routerInstance = Router.router(vertx)
        routerInstance.errorHandler(400, RouteFailureHandler.instance)
        routerInstance.errorHandler(401, RouteFailureHandler.instance)
        routerInstance.errorHandler(404, RouteFailureHandler.instance)
        routerInstance.errorHandler(405, RouteFailureHandler.instance)
        routerInstance.errorHandler(500, RouteFailureHandler.instance)
        routerInstance.route()
            .handler(ResponseContentTypeHandler.create())
            .handler(BodyHandler.create())
            .failureHandler(RouteFailureHandler.instance)
            .handler(AuthServiceHandler.instance)
        initRouter()
        val server = vertx.createHttpServer()
        server.requestHandler(routerInstance).listen(port)
    }

    abstract suspend fun initRouter()
}