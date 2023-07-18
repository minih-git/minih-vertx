package cn.minih.auth.service

import cn.minih.auth.components.RouteFailureHandler
import cn.minih.auth.logic.AuthServiceHandler
import cn.minih.auth.utils.log
import cn.minih.core.service.MinihVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlin.properties.Delegates


/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
abstract class MinihAuthVerticle : CoroutineVerticle(), MinihVerticle {
    private var port = 8080
    val router: Router
        get() {
            return this.routerInstance
        }
    private lateinit var routerInstance: Router

    fun setPort(port: Int) {
        this.port = port
    }

    override suspend fun start() {
        this.routerInstance = Router.router(vertx)
        initConfig()
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
        server.requestHandler(routerInstance).listen(port) {
            if (it.succeeded()) {
                log.info { "服务已启动，端口：$port" }
            }
        }
    }

    abstract suspend fun initRouter()
}
