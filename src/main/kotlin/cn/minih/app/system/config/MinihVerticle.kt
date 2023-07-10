package cn.minih.app.system.config

import cn.minih.app.system.auth.AuthServiceHandler
import cn.minih.app.system.constants.SYSTEM_CONFIGURATION_FRESH
import cn.minih.app.system.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.app.system.utils.log
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await


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
            it.body().forEach { it1 ->
                if (it1.value is JsonObject) {
                    sharedData.getLocalAsyncMap<String, Any>(it1.key).onSuccess { data ->
                        (it1.value as JsonObject).forEach { it2 ->
                            data.put(it2.key, it2.value)
                        }
                    }
                }
            }
        }
        vertx.eventBus().send(SYSTEM_CONFIGURATION_FRESH, "")
        routerInstance.route()
            .handler(ResponseContentTypeHandler.create())
            .handler(BodyHandler.create())
            .handler(CommonHandler.instance)
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
