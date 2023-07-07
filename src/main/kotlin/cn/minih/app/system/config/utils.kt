import io.vertx.core.Vertx
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*

/**
 * An extension method for simplifying coroutines usage with Vert.x Web routers
 */
@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()  // Make intellij happy
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
