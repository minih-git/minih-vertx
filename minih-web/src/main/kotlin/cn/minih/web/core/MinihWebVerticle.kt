@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.minih.web.core

import cn.minih.common.util.getConfig
import cn.minih.common.util.notNullAndExec
import cn.minih.core.boot.MinihVerticle
import cn.minih.web.config.WebConfig
import cn.minih.web.handler.RouteFailureHandler
import cn.minih.web.service.Service
import io.vertx.core.Vertx
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
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
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

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

    private val serviceList = mutableListOf<KClass<*>>()

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
            .handler(ResponseContentTypeHandler.create())
            .handler(bodyHandler)
            .failureHandler(RouteFailureHandler.instance)
        routerInstance.route("/ws/minihEventbus/*").subRouter(sockJSHandler.bridge(options))
    }


    override suspend fun start() {
        initRouterHandler()
        RegisterService.registerService(serviceList, vertx) {
            if (it.second == null) {
                registerRouterHandler(router.route(it.first), it.third)
            } else {
                registerRouterHandler(router.route(it.second, it.first), it.third)

            }
        }
        val server = vertx.createHttpServer()
        val shareData = vertx.sharedData().getAsyncMap<String, Int>("share")
        shareData.await().put("port", port).await()
        server.requestHandler(routerInstance).listen(port)
    }

    open fun registerRouterHandler(route: Route, fn: KFunction<Any?>) {
        route.coroutineHandler(fn)
    }

    abstract suspend fun initRouterHandler()

    fun <T : Service> register(clazz: KClass<T>): MinihWebVerticle {
        this.serviceList.add(clazz)
        return this
    }

}

@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineHandler(fn: KFunction<Any?>) {
    handler { ctx ->
        val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
        GlobalScope.launch(v) {
            fn.call(ctx)
        }
    }
}