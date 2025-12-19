package cn.minih.system.controller

import cn.minih.web.annotation.Get
import cn.minih.web.annotation.Request
import cn.minih.web.service.Service
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.vertx.core.impl.logging.LoggerFactory
import cn.minih.core.annotation.Service as ServiceAnnotation

@Request("/sse")
interface SseController : Service {
    @Get("/flow")
    suspend fun flowStream(): Flow<String>
}

@ServiceAnnotation("sseController")
class SseControllerImpl : SseController {
    
    private val log = LoggerFactory.getLogger(SseController::class.java)

    override suspend fun flowStream(): Flow<String> = flow {
         log.info("SSE Stream started")
         try {
             for (i in 1..100) {
                 val msg = "event $i"
                 log.info("Emitting: $msg")
                 emit(msg)
                 delay(500)
             }
         } catch (e: Exception) {
             log.warn("SSE Stream cancelled/error: ${e.message}")
             throw e
         } finally {
             log.info("SSE Stream finally block executed")
         }
    }
}
