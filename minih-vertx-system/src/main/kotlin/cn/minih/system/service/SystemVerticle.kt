package cn.minih.system.service

import cn.minih.auth.logic.coroutineJsonHandlerHasAuth
import cn.minih.auth.service.MinihAuthVerticle
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.system.service.role.RoleServiceHandler
import cn.minih.system.service.user.UserServiceHandler
import io.netty.handler.codec.http.HttpHeaderValues

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
@MinihServiceVerticle(instance = 30)
class SystemVerticle : MinihAuthVerticle(8090) {
    override suspend fun initRouter() {
        router.route()
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())

        router.get("/user/info").coroutineJsonHandlerHasAuth(UserServiceHandler::getUserInfo)
        router.post("/user/page").coroutineJsonHandlerHasAuth(UserServiceHandler::queryUsers)
        router.post("/user/addUser").coroutineJsonHandlerHasAuth(UserServiceHandler::addUser)
        router.get("/user/checkUsername").coroutineJsonHandlerHasAuth(UserServiceHandler::checkUsername)
        router.get("/user/checkPassword").coroutineJsonHandlerHasAuth(UserServiceHandler::checkPassword)
        router.get("/user/checkMobile").coroutineJsonHandlerHasAuth(UserServiceHandler::checkMobile)
        router.get("/user/lock").coroutineJsonHandlerHasAuth(UserServiceHandler::lock)
        router.get("/user/unlock").coroutineJsonHandlerHasAuth(UserServiceHandler::unlock)
        router.post("/user/editUser").coroutineJsonHandlerHasAuth(UserServiceHandler::editUser)


        router.post("/role/page").coroutineJsonHandlerHasAuth(RoleServiceHandler::queryRoles)

    }
}