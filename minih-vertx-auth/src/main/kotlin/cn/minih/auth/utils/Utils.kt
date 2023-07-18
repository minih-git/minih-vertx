package cn.minih.auth.utils

import cn.minih.auth.annotation.CheckRoleType
import cn.minih.auth.constants.CONTEXT_LOGIN_ID
import cn.minih.auth.logic.AuthServiceHandler
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
val log: KLogger = KotlinLogging.logger {}

fun getRequestBody(ctx: RoutingContext): JsonObject {
    val body = JsonObject()
    ctx.body()?.asJsonObject()?.map {
        body.put(it.key, it.value)
    }
    ctx.request()?.params()?.map {
        body.put(it.key, it.value)
    }
    return body
}


@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        GlobalScope.launch(v) {
            try {
                fn(ctx)
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineVoidHandler(fn: suspend (Any?) -> Unit) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        GlobalScope.launch(v) {
            try {
                fn(null)
                ctx.end()
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun Route.neeRole(vararg role: String, type: CheckRoleType = CheckRoleType.AND): Route {
    putMetadata("needRoles", role)
        .handler { ctx ->
            GlobalScope.launch {
                try {
                    AuthServiceHandler.instance.checkRole(
                        ctx.get(CONTEXT_LOGIN_ID),
                        role.toList(),
                        type == CheckRoleType.AND
                    )
                    ctx.next()
                } catch (e: Exception) {
                    ctx.fail(e)
                }
            }
        }
    return this
}
