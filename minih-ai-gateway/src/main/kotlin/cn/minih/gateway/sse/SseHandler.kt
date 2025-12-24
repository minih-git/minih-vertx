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
import java.util.concurrent.atomic.AtomicBoolean
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

        // 背压控制标志位，防止 drainHandler 重复触发或丢失
        val isWaitingForDrain = AtomicBoolean(false)
        
        // 用于背压控制的 drain handler 挂起机制
        // 三层背压核心逻辑:
        // Layer 1: 操作系统级 TCP 拥塞控制 (底层 Netty 自动处理)
        // Layer 2: Vert.x WriteQueue 水位检查 (writeQueueFull判定缓存溢出)
        // Layer 3: 协程挂起 (suspend 从而抑制上游数据生产)
        suspend fun waitForDrain() {
            if (response.writeQueueFull()) {
                if (isWaitingForDrain.compareAndSet(false, true)) {
                    suspendCancellableCoroutine<Unit> { cont ->
                        response.drainHandler {
                            isWaitingForDrain.set(false)
                            // 移除 handler 以免重复触发逻辑冲突
                            response.drainHandler(null)
                            if (cont.isActive) {
                                cont.resume(Unit)
                            }
                        }
                        
                        // 注册取消回调，防止泄露
                        cont.invokeOnCancellation {
                             response.drainHandler(null)
                             isWaitingForDrain.set(false)
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
