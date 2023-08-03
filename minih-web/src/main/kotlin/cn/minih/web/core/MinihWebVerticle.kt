@file:Suppress("unused")

package cn.minih.web.core

import cn.minih.core.boot.MinihVerticle
import cn.minih.core.utils.getConfig
import cn.minih.core.utils.notNullAndExec
import cn.minih.web.config.WebConfig
import cn.minih.web.handler.RouteFailureHandler
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * web服务启动器
 * @author hubin
 * @since 2023-07-30 22:08:04
 * @desc
 */
@Suppress("unused")
abstract class MinihWebVerticle(private val port: Int = 8080) : MinihVerticle, CoroutineVerticle() {

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
        var bodyHandler = BodyHandler.create().setDeleteUploadedFilesOnEnd(true)
        getConfig("web", WebConfig::class).tmpFilePath.notNullAndExec {
            bodyHandler = bodyHandler.setUploadsDirectory(it).setDeleteUploadedFilesOnEnd(false)
        }
        routerInstance.route()
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())
            .handler(ResponseContentTypeHandler.create())
            .handler(bodyHandler)
            .failureHandler(RouteFailureHandler.instance)
        routerInstance.route("/ws/minihEventbus/*").subRouter(sockJSHandler.bridge(options))
    }

    override suspend fun start() {
        initRouterHandler()
        val server = vertx.createHttpServer()
        val shareData = vertx.sharedData().getAsyncMap<String, Int>("share")
        shareData.await().put("port", port).await()
        server.requestHandler(routerInstance).listen(port)
    }

    abstract suspend fun initRouterHandler()
}

@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineHandler(fn: suspend (ctx: RoutingContext) -> Unit) {
    handler { ctx ->
        val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
        GlobalScope.launch(v) {
            fn(ctx)
        }
    }
}