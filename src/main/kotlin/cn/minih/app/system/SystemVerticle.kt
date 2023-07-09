package cn.minih.app.system

import cn.minih.app.system.auth.AuthLogic
import cn.minih.app.system.auth.AuthServiceHandler
import cn.minih.app.system.config.MinihVerticle
import cn.minih.app.system.user.UserRepository
import cn.minih.app.system.utils.coroutineJsonHandler
import cn.minih.app.system.utils.coroutineVoidHandler


/**
 * @author hubin
 * @date 2023/7/6
 * @desc
 */
class SystemVerticle(port: Int) : MinihVerticle(port) {
    override suspend fun initRouter() {
        val authHandle = AuthServiceHandler(UserRepository())
        router.post("/auth/login").coroutineJsonHandler { ctx -> authHandle.login(ctx) }
        router.post("/test").coroutineVoidHandler { ctx -> AuthLogic().createTokenValue() }
    }
}