package cn.minih.system

import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.system.controller.RoleController
import cn.minih.web.core.MinihWebVerticle
import cn.minih.system.controller.UserController

import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@MinihServiceVerticle
class SystemVerticle : MinihWebVerticle(8081) {
    override suspend fun initRouterHandler() {
        initRouter()

        register(RoleController::class)
        register(UserController::class)
        // register(cn.minih.system.controller.SseController::class) // Removed, using manual route below

        // Manual SSE handling
        router.get("/sse/flow").handler { ctx ->
            val response = ctx.response()
            response.putHeader("Content-Type", "text/event-stream; charset=utf-8")
                .putHeader("Cache-Control", "no-cache")
                .putHeader("Connection", "keep-alive").isChunked = true

            val log = io.vertx.core.impl.logging.LoggerFactory.getLogger("SseManualTest")
            val scope = kotlinx.coroutines.CoroutineScope(vertx.orCreateContext.dispatcher())

            val job = scope.launch {
                try {
                    log.info("SSE Connection started - Proxying to DashScope LLM via OkHttp")
                    val prompt = ctx.request().getParam("prompt") ?: "你好，请介绍一下你自己"
                    val apiKey = "sk-fdc4a1cd6e714790b400c5a07f6c293c"

                    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val llmPayload = io.vertx.core.json.JsonObject()
                        .put("model", "qwen-plus")
                        .put("stream", true)
                        .put("messages", io.vertx.core.json.JsonArray().add(io.vertx.core.json.JsonObject().put("role", "user").put("content", prompt)))

                    val request = okhttp3.Request.Builder()
                        .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .post(okhttp3.RequestBody.create(jsonMediaType, llmPayload.toString()))
                        .build()

                    val client = okhttp3.OkHttpClient.Builder()
                        .readTimeout(java.time.Duration.ofMinutes(2))
                        .build()

                    callbackFlow {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                client.newCall(request).execute().use { response ->
                                    if (!response.isSuccessful) {
                                        trySend("data: Error ${response.code} ${response.message}\n\n")
                                        close()
                                        return@use
                                    }

                                    val source = response.body?.source()
                                    if (source == null) {
                                        close()
                                        return@use
                                    }

                                    while (!source.exhausted()) {
                                        val line = source.readUtf8Line()
                                        if (line != null) {
                                            if (line.startsWith("data:")) {
                                                val dataStr = line.substring(5).trim()
                                                if (dataStr == "[DONE]") {
                                                    close()
                                                    break
                                                }
                                                try {
                                                    if (dataStr.isNotBlank()) {
                                                        val json = io.vertx.core.json.JsonObject(dataStr)
                                                        val choices = json.getJsonArray("choices")
                                                        if (choices != null && choices.size() > 0) {
                                                            val content = choices.getJsonObject(0)?.getJsonObject("delta")?.getString("content")
                                                            if (!content.isNullOrEmpty()) {
                                                                trySend(content)
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                            }
                                        }
                                    }
                                    close()
                                }
                            } catch (e: Exception) {
                                close(e)
                            }
                        }
                        awaitClose {
                           // Cleanup
                        }
                    }.collect { data ->
                        response.write(data)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    log.info("SSE Coroutine cancelled successfully")
                    throw e
                } catch (e: Exception) {
                    log.error("SSE Error connecting to LLM", e)
                    response.write("data: Error connecting to LLM: ${e.message}\n\n")
                } finally {
                    log.info("SSE resource cleanup")
                     if (!response.ended()) {
                        response.end()
                    }
                }
            }

            response.closeHandler {
                log.info("Client disconnected, cancelling job")
                job.cancel(kotlinx.coroutines.CancellationException("Client disconnected"))
            }
        }
    }
}