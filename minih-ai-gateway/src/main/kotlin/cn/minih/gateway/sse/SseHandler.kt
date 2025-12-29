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

    fun handleSse(ctx: RoutingContext, flow: Flow<String>) {
        val response = ctx.response()
        response.setChunked(true)
        response.putHeader("Content-Type", "text/event-stream")
        response.putHeader("Cache-Control", "no-cache")
        response.putHeader("Connection", "keep-alive")

        // 创建与请求绑定的 CoroutineScope (SupervisorJob)
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)

        ctx.request().connection().closeHandler {
            scope.cancel()
        }

        val isWaitingForDrain = AtomicBoolean(false)
        
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
                    waitForDrain()

                    // 唤醒后再次检查验证
                    try {
                        if (!response.closed()) {
                           response.write("data: $data\n\n")
                        } else {
                            throw RuntimeException("Connection closed by client during write check")
                        }
                    } catch (e: Exception) {
                        // 忽略连接已关闭异常，这是正常断开流程
                        val msg = e.message ?: ""
                        if (response.closed() || msg.contains("Connection closed") || msg.contains("Broken pipe")) {
                            log.info("Connection closed by client")
                            return@collect
                        }
                        throw e
                    }
                }
                if (!response.closed()) {
                    response.end()
                }
            } catch (e: Exception) {
                log.error("SSE error: ", e)
            } 
        }
    }
}
