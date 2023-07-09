package cn.minih.app.system.config

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle

val log: KLogger = KotlinLogging.logger {}

/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
abstract class MinihVerticle(private val port: Int = 8080) : CoroutineVerticle() {
    val router: Router
        get() {
            return this.routerInstance
        }
    private lateinit var routerInstance: Router


    override suspend fun start() {
        this.routerInstance = Router.router(vertx)
        val sharedData = vertx.sharedData()
        vertx.eventBus().consumer<JsonObject>(SYSTEM_CONFIGURATION_SUBSCRIBE).handler {
            sharedData.getLocalAsyncMap<String, String>("config").onSuccess { it1 ->
                it1.put("mongodb.host", it.body().getString("mongodb.host") ?: "")
            }
            sharedData.getLocalAsyncMap<String, String>("auth").onSuccess { it1 ->
                it1.put("tokenStyle", it.body().getString("tokenStyle") ?: "")
            }
        }
        vertx.eventBus().send(SYSTEM_CONFIGURATION_FRESH, "")
        routerInstance.route()
            .handler(ResponseContentTypeHandler.create())
            .handler(BodyHandler.create())
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())
            .failureHandler(RouteFailureHandler.create())
        routerInstance.route().handler {
            it.addHeadersEndHandler { _ ->
                val origin = it.request().getHeader("Origin")
                if (origin != null && origin.isNotEmpty()) {
                    val res = it.response()
                    res.putHeader("Access-Control-Allow-Origin", origin)
                    res.putHeader("Access-Control-Allow-Credentials", "true")
                    res.putHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE")
                    res.putHeader(
                        "Access-Control-Allow-Headers",
                        "Authorization, Content-Type, If-Match, If-Modified-Since, If-None-Match, If-Unmodified-Since, X-Requested-With"
                    )
                }
            }
            val origin = it.request().getHeader("Origin")
            if (origin != null && origin.isNotEmpty() && it.request().method() == HttpMethod.OPTIONS) {
                it.end("")
            } else {
                it.next()
            }
        }
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