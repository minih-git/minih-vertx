package cn.minih.auth.service

import cn.minih.auth.components.RouteFailureHandler
import cn.minih.auth.logic.AuthServiceHandler
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSSocket
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
        val sockJSHandler = SockJSHandler.create(vertx)
        val options = SockJSBridgeOptions()
        options.addInboundPermitted(PermittedOptions().setAddressRegex("cn.minih.auth.*"))
        options.addOutboundPermitted(PermittedOptions().setAddressRegex("cn.minih.auth.*"))
        router.route("/ws/authEventbus/*")
            .subRouter(sockJSHandler.bridge(options))
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
