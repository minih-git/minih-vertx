package cn.minih.system

import cn.minih.auth.logic.coroutineJsonHandlerHasAuth
import cn.minih.auth.service.MinihAuthVerticle
import cn.minih.auth.utils.log
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.core.beans.BeanFactory
import cn.minih.core.components.MinihServiceRun
import cn.minih.core.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.system.service.user.RoleRepository
import cn.minih.system.service.user.UserServiceHandler
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.config.ConfigChange
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Context
import io.vertx.core.Promise
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject


/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
class Main

fun main() {
    MinihServiceRun.run(Main::class)
    val role = BeanFactory.instance.getBean("RoleRepository") as RoleRepository
    role.print()
}

@MinihServiceVerticle(8090)
class SystemVerticle : MinihAuthVerticle() {
    override suspend fun initRouter() {
        router.route()
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())

        router.get("/user/info").coroutineJsonHandlerHasAuth(UserServiceHandler::getUserInfo)
        router.post("/user/page").coroutineJsonHandlerHasAuth(UserServiceHandler::queryUsers)
        router.post("/user/addUser").coroutineJsonHandlerHasAuth(UserServiceHandler::addUser)
        router.post("/user/editUser").coroutineJsonHandlerHasAuth(UserServiceHandler::editUser)

    }
}

class ConfigVerticle : Verticle {

    private lateinit var vertxInstance: Vertx
    private lateinit var context: Context


    override fun getVertx(): Vertx = vertxInstance

    override fun init(vertx: Vertx, context: Context) {
        this.vertxInstance = vertx
        this.context = context
    }

    override fun start(startPromise: Promise<Void>?) {
        val retriever = ConfigRetriever.create(
            vertx, ConfigRetrieverOptions()
                .addStore(
                    ConfigStoreOptions().setType("file").setFormat("yaml")
                        .setConfig(
                            JsonObject().put(
                                "path",
                                "/Users/hubin/IdeaProjects/minih-vertx/src/main/resources/app.yaml"
                            )
                        )
                )
        )
        retriever.listen { change: ConfigChange ->
            vertx.eventBus().publish(SYSTEM_CONFIGURATION_SUBSCRIBE, change.newConfiguration)
        }
        log.info("配置中心启动完毕...")
    }

    override fun stop(stopPromise: Promise<Void>?) {

    }
}
