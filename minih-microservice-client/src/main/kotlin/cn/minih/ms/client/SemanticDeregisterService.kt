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
        val processedMethods = mutableSetOf<String>()

        beans.forEach { (_, bean) ->
            val classesToScan = mutableListOf<Class<*>>()
            classesToScan.add(bean::class.java)
            classesToScan.addAll(bean::class.java.interfaces)

            classesToScan.forEach { clazz ->
                clazz.methods.forEach { method ->
                    if (method.isAnnotationPresent(AiTool::class.java)) {
                        val className = clazz.simpleName
                        val serviceId = "$projectName:$className:${method.name}"

                        if (processedMethods.contains(serviceId)) {
                            return@forEach
                        }
                        processedMethods.add(serviceId)

                        // Registry Config
                        val registryHost = context.config().getString("minih.semantic.registry.host", "localhost")
                        val registryPort = context.config().getInteger("minih.semantic.registry.port", 8099)

                        val msg = JsonObject().put("id", serviceId)

                        try {
                            val response = WebClient.create(context.owner())
                                .post(registryPort, registryHost, "/semantic/api/unregister")
                                .sendJsonObject(msg)
                                .coAwait()

                            if (response.statusCode() == 200) {
                                log.info("Deregistered AI Tool via HTTP: $serviceId")
                            } else {
                                log.warn("Failed to deregister AI Tool: $serviceId, status: ${response.statusCode()}")
                            }
                        } catch (e: Exception) {
                            log.warn("Failed to deregister AI Tool: $serviceId, error: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}
