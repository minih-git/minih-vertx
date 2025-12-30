package cn.minih.ms.client

import cn.minih.common.util.getProjectName
import cn.minih.core.annotation.Component
import cn.minih.core.beans.BeanFactory
import cn.minih.core.boot.PostStartingProcess
import cn.minih.common.annotation.AiTool
import io.vertx.core.Context
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.core.impl.logging.LoggerFactory

/**
 * 语义注册中心心跳服务
 * 定时向语义注册中心发送心跳，保持实例存活
 * @author hubin
 * @since 2024-12-19
 */
@Component
class SemanticHeartbeatService : PostStartingProcess {

    private val log = LoggerFactory.getLogger(SemanticHeartbeatService::class.java)

    companion object {
        /**
         * 心跳定时器 ID，用于在服务关闭时取消
         */
        var timerId: Long? = null
            private set

        /**
         * 已注册的服务 ID 列表
         */
        val registeredServiceIds = mutableSetOf<String>()
    }

    /**
     * 心跳间隔: 30秒
     */
    private val HEARTBEAT_INTERVAL_MS = 30_000L

    override suspend fun exec(context: Context) {
        // 收集所有需要心跳的服务 ID
        collectServiceIds(context)

        if (registeredServiceIds.isEmpty()) {
            log.info("No AI tools registered, skipping heartbeat service")
            return
        }

        // 启动心跳定时器
        timerId = context.owner().setPeriodic(HEARTBEAT_INTERVAL_MS) {
            sendHeartbeats(context)
        }

        log.info("Heartbeat service started for ${registeredServiceIds.size} services, interval: ${HEARTBEAT_INTERVAL_MS}ms")
    }

    /**
     * 收集所有 @AiTool 注解的服务 ID
     */
    private fun collectServiceIds(context: Context) {
        val projectName = getProjectName(context)
        val beans = BeanFactory.instance.getBeans()

        beans.forEach { (_, bean) ->
            val classesToScan = mutableListOf<Class<*>>()
            classesToScan.add(bean::class.java)
            classesToScan.addAll(bean::class.java.interfaces)

            classesToScan.forEach { clazz ->
                clazz.methods.forEach { method ->
                    if (method.isAnnotationPresent(AiTool::class.java)) {
                        val className = clazz.simpleName
                        val serviceId = "$projectName:$className:${method.name}"
                        registeredServiceIds.add(serviceId)
                    }
                }
            }
        }
    }

    /**
     * 发送心跳到语义注册中心
     */
    private fun sendHeartbeats(context: Context) {
        val envHost = System.getenv("SEMANTIC_REGISTRY_HOST")
        val envPortStr = System.getenv("SEMANTIC_REGISTRY_PORT")

        val registryHost = if (!envHost.isNullOrBlank()) envHost else context.config().getString("minih.semantic.registry.host", "localhost")
        val registryPort = if (!envPortStr.isNullOrBlank()) envPortStr.toInt() else context.config().getInteger("minih.semantic.registry.port", 8099)
        val client = WebClient.create(context.owner())

        registeredServiceIds.forEach { serviceId ->
            val msg = JsonObject().put("id", serviceId)

            client.post(registryPort, registryHost, "/semantic/api/heartbeat")
                .timeout(5000L)  // 5秒超时
                .sendJsonObject(msg) { ar ->
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        log.debug("Heartbeat sent: $serviceId")
                    } else {
                        val cause = ar.cause()?.message ?: ar.result()?.statusMessage() ?: "Unknown error"
                        log.warn("Heartbeat failed for $serviceId: $cause")
                    }
                }
        }
    }

    /**
     * 停止心跳定时器（由 SemanticDeregisterService 调用）
     */
    fun stopHeartbeat(context: Context) {
        timerId?.let {
            context.owner().cancelTimer(it)
            log.info("Heartbeat timer cancelled")
        }
        timerId = null
        registeredServiceIds.clear()
    }
}