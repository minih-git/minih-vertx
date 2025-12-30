package cn.minih.ms.client

import cn.minih.common.util.getProjectName
import cn.minih.core.annotation.Component
import cn.minih.core.beans.BeanFactory
import cn.minih.core.boot.PreStopProcess
import cn.minih.common.annotation.AiTool
import io.vertx.core.Context
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * 语义注册中心服务下线
 * @author hubin
 * @since 2024-12-18
 */
@Component
class SemanticDeregisterService : PreStopProcess {

    private val log = LoggerFactory.getLogger(SemanticDeregisterService::class.java)

    override suspend fun exec(context: Context) {
        try {
            // 先停止心跳定时器
            stopHeartbeatTimer(context)
            // 再下线服务
            deregisterAiTools(context)
        } catch (e: Exception) {
            log.error("Failed to deregister AI tools from semantic registry", e)
        }
    }

    private fun stopHeartbeatTimer(context: Context) {
        SemanticHeartbeatService.timerId?.let {
            context.owner().cancelTimer(it)
            log.info("Heartbeat timer cancelled")
        }
    }
    private suspend fun deregisterAiTools(context: Context) {
        val projectName = getProjectName(context)
        val beans = BeanFactory.instance.getBeans()
        val serviceIds = mutableSetOf<String>()

        // 1. 预扫描，收集所有需要注销的 ID (避免在 IO 循环中做反射)
        beans.values.forEach { bean ->
            val classes = listOf(bean::class.java) + bean::class.java.interfaces
            classes.forEach { clazz ->
                clazz.methods.forEach { method ->
                    if (method.isAnnotationPresent(AiTool::class.java)) {
                        serviceIds.add("$projectName:${clazz.simpleName}:${method.name}")
                    }
                }
            }
        }

        if (serviceIds.isEmpty()) return

        // 2. 复用 WebClient
        val client = WebClient.create(context.owner())
        val envHost = System.getenv("SEMANTIC_REGISTRY_HOST")
        val envPortStr = System.getenv("SEMANTIC_REGISTRY_PORT")
        val registryHost = if (!envHost.isNullOrBlank()) envHost else context.config().getString("minih.semantic.registry.host", "localhost")
        val registryPort = if (!envPortStr.isNullOrBlank()) envPortStr.toInt() else context.config().getInteger("minih.semantic.registry.port", 8099)
        // 3. 并发注销 (使用 coroutineScope 等待所有注销完成)
        coroutineScope {
            serviceIds.map { id ->
                async {
                    performUnregister(client, registryHost, registryPort, id)
                }
            }.awaitAll() // 并行执行并等待
        }

        client.close() // 扫尾
    }

    private suspend fun performUnregister(client: WebClient, host: String, port: Int, id: String) {
        var retries = 3
        while (retries > 0) {
            try {
                val response = client.post(port, host, "/semantic/api/unregister")
                    .timeout(3000L)
                    .sendJsonObject(JsonObject().put("id", id))
                    .coAwait()

                if (response.statusCode() == 200) {
                    log.info("Deregistered: $id")
                    return
                }
            } catch (e: Exception) {
                log.warn("Retry $id due to ${e.message}")
            }
            retries--
            if (retries > 0) delay(1000L)
        }
    }
}