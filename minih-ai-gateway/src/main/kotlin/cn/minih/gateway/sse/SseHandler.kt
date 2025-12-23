package cn.minih.gateway.sse

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object SseHandler {

    /**
     * 处理 SSE 请求，支持三层背压策略和自动取消
     * Layer 1: TCP/Buffer backlog (Netty handles)
     * Layer 2: Vert.x WriteQueue (We check isFull)
     * Layer 3: Application Suspend (We suspend coroutine)
     */
    fun handleSse(ctx: RoutingContext, flow: Flow<String>) {
        val response = ctx.response()
        response.setChunked(true)
        response.putHeader("Content-Type", "text/event-stream")
        response.putHeader("Cache-Control", "no-cache")
        response.putHeader("Connection", "keep-alive")

        // 创建与请求绑定的 CoroutineScope (SupervisorJob)
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)

        // 监听连接关闭，取消协程
        // scope.cancel() 也会触发 finally block
        ctx.request().connection().closeHandler {
            scope.cancel()
        }

        // 用于背压控制的 drain handler 挂起机制
        // 当 writeQueue 满时，suspend 等待 drain 事件
        suspend fun waitForDrain() {
            if (response.writeQueueFull()) {
                suspendCancellableCoroutine<Unit> { cont ->
                    response.drainHandler {
                        // 移除 handler 以免重复触发
                        response.drainHandler(null)
                        if (cont.isActive) {
                            cont.resume(Unit)
                        }
                    }
                }
            }
        }

        scope.launch {
            try {
                flow.collect { data ->
                    // Layer 3 Backpressure Check
                    waitForDrain()

                    // Check validation again after waking up
                    if (!response.closed()) {
                       response.write("data: $data\n\n")
                    } else {
                        throw RuntimeException("Connection closed by client")
                    }
                }
                if (!response.closed()) {
                    response.end()
                }
            } catch (e: Exception) {
                // Log only if it's not a normal cancellation
                // kotlinx.coroutines.JobCancellationException is normal
                if (e !is kotlinx.coroutines.CancellationException) {
                     // client disconnect often shows as "Connection was closed"
                }
            } finally {
                // Cleanup logic if needed
            }
        }
    }
}
