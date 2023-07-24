package cn.minih.system.service

import cn.minih.auth.logic.AuthUtil
import cn.minih.auth.logic.coroutineJsonHandlerHasAuth
import cn.minih.auth.logic.coroutineJsonHandlerNoAuth
import cn.minih.auth.service.MinihAuthVerticle
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.system.service.resource.ResourceServiceHandler
import cn.minih.system.service.role.RoleServiceHandler
import cn.minih.system.service.user.UserServiceHandler
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
@MinihServiceVerticle(instance = 8)
class SystemVerticle : MinihAuthVerticle(8090) {
    override suspend fun initRouterHandler() {
        createSystemEventBusListener()
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
        router.post("/role/addRole").coroutineJsonHandlerHasAuth(RoleServiceHandler::addRole)
        router.post("/role/editRole").coroutineJsonHandlerHasAuth(RoleServiceHandler::editRole)
        router.get("/role/checkRoleTag").coroutineJsonHandlerHasAuth(RoleServiceHandler::checkRoleTag)
        router.post("/resource/page").coroutineJsonHandlerHasAuth(ResourceServiceHandler::queryResources)
        router.post("/resource/addResource").coroutineJsonHandlerHasAuth(ResourceServiceHandler::addResource)
        router.post("/resource/editResource").coroutineJsonHandlerHasAuth(ResourceServiceHandler::editResource)
        router.post("/system/vCode").coroutineJsonHandlerNoAuth(SystemServiceHandler::smsSendVCode)

    }

    @OptIn(DelicateCoroutinesApi::class)
    fun createSystemEventBusListener() {
        val sockJSHandler = SockJSHandler.create(vertx)
        val options = SockJSBridgeOptions()
        options.addInboundPermitted(PermittedOptions().setAddress("cn.minih.system.event.bus"))
        options.addOutboundPermitted(PermittedOptions().setAddress("cn.minih.system.event.bus"))
        val bridge = sockJSHandler.bridge(options) {
            GlobalScope.launch(Vertx.currentContext().dispatcher()) {
                if (it.rawMessage == null || it.rawMessage.isEmpty) {
                    it.complete(true)
                    return@launch
                }
                val token = it.rawMessage.getJsonObject("headers")?.getString("token")
                if (token.isNullOrEmpty()) {
                    it.complete(false)
                    return@launch
                }
                AuthUtil.checkLogin(token)
                it.complete(true)
            }
        }
        router.route("/ws/systemEvent/*").subRouter(bridge)
    }

}