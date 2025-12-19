package cn.minih.ms.client

import cn.minih.common.util.getProjectName
import cn.minih.core.annotation.Component
import cn.minih.core.beans.BeanFactory
import cn.minih.core.boot.PostStartingProcess
import cn.minih.common.annotation.AiTool
import cn.minih.gateway.schema.SchemaGenerator
import io.vertx.core.Context
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.vertx.core.impl.logging.LoggerFactory
import kotlin.reflect.jvm.kotlinFunction

@Component
class SemanticRegisterService : PostStartingProcess {

    private val log = LoggerFactory.getLogger(SemanticRegisterService::class.java)

    override suspend fun exec(context: Context) {
        // 在新协程中执行，避免阻塞启动流程
        CoroutineScope(context.dispatcher()).launch {
            try {
                registerAiTools(context)
            } catch (e: Exception) {
                log.error("Failed to register AI tools", e)
            }
        }
    }

    private fun registerAiTools(context: Context) {
        val projectName = getProjectName(context)
        val beans = BeanFactory.instance.getBeans()
        val processedMethods = mutableSetOf<String>() // Avoid duplicates

        beans.forEach { (name, bean) ->
            // Scan the bean class and all its interfaces
            val classesToScan = mutableListOf<Class<*>>()
            classesToScan.add(bean::class.java)
            classesToScan.addAll(bean::class.java.interfaces)

            classesToScan.forEach { clazz ->
                // Extract parent path from class level @Request
                val parentMapping = try {
                   cn.minih.common.util.findRequestMapping(clazz.kotlin)
                } catch (e: Exception) { null }

                var parentPath = ""
                parentMapping?.let { parentPath = cn.minih.common.util.formatPath(it.url) }

                clazz.methods.forEach { method ->
                    if (method.isAnnotationPresent(AiTool::class.java)) {
                        val annotation = method.getAnnotation(AiTool::class.java)
                        val description = annotation.description

                        // 构造唯一 Service ID
                        val className = clazz.simpleName
                        val serviceId = "$projectName:$className:${method.name}"

                        if (processedMethods.contains(serviceId)) {
                            return@forEach
                        }
                        processedMethods.add(serviceId)

                        // 生成 Schema
                        val schema = SchemaGenerator.generateSchema(method)

                        // 修复: 使用 Gson 序列化 schema，避免 Jackson Databind 依赖缺失问题
                        val gson = com.google.gson.Gson()
                        val schemaJson = gson.toJson(schema)

                        val payload = JsonObject()
                            .put("projectName", projectName)
                            .put("serviceId", serviceId)
                            .put("method", method.name)
                            .put("className", clazz.name)
                            .put("description", description)
                            .put("schema", JsonObject(schemaJson))



// ... (keep existing code, update logic below)

                        // Extract URL and HTTP Method
                        try {
                            val kFunc = method.kotlinFunction
                            if (kFunc != null) {
                                val mapping = cn.minih.common.util.findRequestMapping(kFunc)
                                mapping?.let {
                                    var realPath = parentPath
                                    if (it.url.isNotBlank() && it.url != "/") {
                                        realPath = realPath + cn.minih.common.util.formatPath(it.url)
                                    }
                                    payload.put("url", realPath)

                                    val httpMethod = when (it.type) {
                                        is cn.minih.web.annotation.Get -> "GET"
                                        is cn.minih.web.annotation.Post -> "POST"
                                        is cn.minih.web.annotation.Put -> "PUT"
                                        is cn.minih.web.annotation.Delete -> "DELETE"
                                        else -> "UNKNOWN"
                                    }
                                    payload.put("httpMethod", httpMethod)
                                }
                            }
                        } catch (e: Exception) {
                            log.warn("Failed to extract HTTP info for $serviceId", e)
                        }


                        val msg = JsonObject()
                            .put("id", serviceId)
                            .put("desc", description)
                            .put("payload", payload.toString())

                        // Registry Config
                        val registryHost = context.config().getString("minih.semantic.registry.host", "localhost")
                        val registryPort = context.config().getInteger("minih.semantic.registry.port", 8099)

                        WebClient.create(context.owner())
                            .post(registryPort, registryHost, "/semantic/api/register")
                            .sendJsonObject(msg) { ar ->
                                if (ar.succeeded() && ar.result().statusCode() == 200) {
                                    log.info("Registered AI Tool via HTTP: $serviceId")
                                } else {
                                    log.error("Failed to register AI Tool via HTTP: $serviceId. Cause: ${ar.cause()?.message ?: ar.result()?.statusMessage()}")
                                }
                            }
                    }
                }
            }
        }
    }
}