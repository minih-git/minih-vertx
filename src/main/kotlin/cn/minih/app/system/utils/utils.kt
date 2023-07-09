package cn.minih.app.system.utils

import com.google.gson.Gson
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*


@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineJsonHandler(fn: suspend (JsonObject) -> Any) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        GlobalScope.launch(v) {
            try {
                val body = ctx.body().asJsonObject()
                ctx.json(JsonObject(Gson().toJson(fn(body))))
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }
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
                print(fn(null))
                ctx.end()
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }
}