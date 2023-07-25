package cn.minih.core.web

import cn.minih.core.handler.RequestMonitorHandler
import cn.minih.core.handler.RouteFailureHandler
import cn.minih.core.utils.R
import cn.minih.core.utils.getConfig
import cn.minih.core.utils.toJsonObject
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * @author hubin
 * @date 2023/7/24
 * @desc
 */
abstract class MinihVerticle(private val port: Int = 8080) : CoroutineVerticle() {
    val router: Router
        get() {
            return this.routerInstance
        }
    private lateinit var routerInstance: Router

    fun initRouter() {
        this.routerInstance = Router.router(vertx)
        val sockJSHandler = SockJSHandler.create(vertx)
        val options = SockJSBridgeOptions()
        options.addInboundPermitted(PermittedOptions().setAddressRegex("cn.minih.*"))
        options.addOutboundPermitted(PermittedOptions().setAddressRegex("cn.minih.*"))
        routerInstance.route("/ws/minihEventbus/*")
            .subRouter(sockJSHandler.bridge(options))
        routerInstance.route("/options").handler {
            it.json(R.ok(mapOf("se" to getConfig().aesSecret)).toJsonObject())
        }
        routerInstance.route()
            .handler(ResponseContentTypeHandler.create())
            .handler(BodyHandler.create())
            .handler(RequestMonitorHandler.instance)
            .failureHandler(RouteFailureHandler.instance)
    }

    override suspend fun start() {
        initRouterHandler()
        val server = vertx.createHttpServer()
        server.requestHandler(routerInstance).listen(port)
    }

    abstract suspend fun initRouterHandler()

}
