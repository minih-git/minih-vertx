package cn.minih.app.system

import cn.minih.app.system.auth.AuthServiceHandler
import cn.minih.app.system.user.UserRepository
import coroutineHandler
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

/**
 * @author hubin
 * @date 2023/7/6
 * @desc
 */
class SystemVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val userRepository = UserRepository(vertx)
        val authHandle = AuthServiceHandler(userRepository)
        val router = Router.router(vertx)
        router.get("/auth/login").coroutineHandler { ctx -> authHandle.login(ctx) }
        val server = vertx.createHttpServer()
        server.requestHandler(router).listen(8080).await()
    }
}
