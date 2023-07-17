package cn.minih.app.system.config

import cn.minih.app.system.auth.AuthServiceHandler
import cn.minih.app.system.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.app.system.utils.log
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.ext.web.handler.impl.ErrorHandlerImpl
import io.vertx.kotlin.core.json.get
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
        routerInstance.errorHandler(400,RouteFailureHandler.instance)
        routerInstance.errorHandler(401,RouteFailureHandler.instance)
        routerInstance.errorHandler(404,RouteFailureHandler.instance)
        routerInstance.errorHandler(405,RouteFailureHandler.instance)
        routerInstance.errorHandler(500,RouteFailureHandler.instance)
        initConfig()
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

    private suspend fun initConfig() {
        val retriever = ConfigRetriever.create(
            vertx, ConfigRetrieverOptions()
                .addStore(ConfigStoreOptions().setType("env").setFormat("json"))
                .addStore(
                    ConfigStoreOptions().setType("file").setFormat("yaml")
                        .setConfig(JsonObject().put("path", "app.yaml"))
                )
        )

        retriever.config.await().forEach {
            val config = Vertx.currentContext().config()
            if (it.key.contains(".")) {
                val key = it.key.substring(0, it.key.indexOf("."))
                val subKey = it.key.substring(it.key.indexOf(".") + 1)
                val value = it.value
                val map = config.get<JsonObject>(key) ?: JsonObject()
                map.put(subKey, value)
                config.put(key, map)
            }
            config.put(it.key, it.value)
        }
        vertx.eventBus().consumer<JsonObject>(SYSTEM_CONFIGURATION_SUBSCRIBE).handler { config ->
            config.body().forEach {
                Vertx.currentContext().config().put(it.key, it.value)
            }
        }
    }

    abstract suspend fun initRouter()
}