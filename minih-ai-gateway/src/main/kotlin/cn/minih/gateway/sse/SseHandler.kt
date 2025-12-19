package cn.minih.gateway.sse

import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object SseHandler {

    /**
     * 处理 SSE 请求，支持背压和自动取消
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
        ctx.request().connection().closeHandler {
            scope.cancel()
        }

        scope.launch {
            try {
                flow.collect { data ->
                    response.write("data: $data\n\n")
                }
                response.end()
            } catch (e: Exception) {
            }
        }
    }
}
