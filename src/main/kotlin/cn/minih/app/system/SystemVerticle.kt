package cn.minih.app.system

import cn.minih.app.system.auth.AuthServiceHandler
import cn.minih.app.system.config.Log
import cn.minih.app.system.config.Log.Companion.log
import cn.minih.app.system.user.UserRepository
import coroutineHandler
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle


/**
 * @author hubin
 * @date 2023/7/6
 * @desc
 */
@Log
class SystemVerticle(private val port: Int) : CoroutineVerticle() {

    override suspend fun start() {
        val userRepository = UserRepository(vertx)
        val authHandle = AuthServiceHandler(userRepository)
        val router = Router.router(vertx)
        router.route()
            .handler(ResponseContentTypeHandler.create())
            .handler(BodyHandler.create())
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())

        router.route().handler {
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

        router.post("/auth/login").produces("application/json").coroutineHandler { ctx -> authHandle.login(ctx) }
        val server = vertx.createHttpServer()
        server.requestHandler(router).listen(port) {
            if (it.succeeded()) {
                log.info{"服务已启动，端口：$port"}
            }
        }
    }
}