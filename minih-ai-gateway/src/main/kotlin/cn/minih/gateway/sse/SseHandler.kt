package cn.minih.gateway.sse

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object SseHandler {
    private val log = LoggerFactory.getLogger(SseHandler::class.java)

    fun handleSse(ctx: RoutingContext, flow: Flow<String>) {
        val response = ctx.response()
        response.setChunked(true)
        response.putHeader("Content-Type", "text/event-stream")
        response.putHeader("Cache-Control", "no-cache")
        response.putHeader("Connection", "keep-alive")

        //绑定到当前请求所在的特定 Context 线程
        val scope = CoroutineScope(ctx.context().dispatcher() + SupervisorJob())

        ctx.request().connection().closeHandler {
            scope.cancel()
            log.debug("SSE Connection closed, scope cancelled")
        }

        scope.launch {
            try {
                flow.collect { data ->
                    // 简化背压逻辑，确保严格挂起
                    if (response.writeQueueFull()) {
                        suspendCancellableCoroutine<Unit> { cont ->
                            response.drainHandler {
                                response.drainHandler(null)
                                if (cont.isActive) cont.resume(Unit)
                            }
                            cont.invokeOnCancellation {
                                response.drainHandler(null)
                            }
                        }
                    }

                    if (!response.closed()) {
                        // 封装 SSE 协议格式
                        response.write("data: $data\n\n")
                    } else {
                        throw CancellationException("Response closed")
                    }
                }
                
                if (!response.closed()) {
                    response.end()
                }
            } catch (e: Exception) {
                if (e is CancellationException || response.closed()) {
                    log.info("SSE Stream ended by client disconnection")
                } else {
                    log.error("SSE unexpected error", e)
                }
            } finally {
                // 确保无论如何最后都能关闭
                if (!response.ended()) response.end()
            }
        }
    }
}
