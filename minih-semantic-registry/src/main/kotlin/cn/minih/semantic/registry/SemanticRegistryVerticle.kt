package cn.minih.semantic.registry

import cn.minih.core.annotation.Component
import cn.minih.semantic.core.HnswIndexService
import cn.minih.semantic.core.InstanceTable
import cn.minih.semantic.core.OnnxEmbeddingService
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

@Component
class SemanticRegistryVerticle(
    private val embeddingService: OnnxEmbeddingService,
    private val hnswIndexService: HnswIndexService,
    private val instanceTable: InstanceTable
) : AbstractVerticle() {

    private val log = LoggerFactory.getLogger(SemanticRegistryVerticle::class.java)

    override fun start() {
        // Event Bus Logic (Optional, kept for internal usage)
        val eb = vertx.eventBus()

        // ... (EventBus handlers can stay or be removed. I will keep them for now but add HTTP)
        
        // HTTP Server Logic
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        
        // POST /semantic/api/register
        router.post("/semantic/api/register").handler { ctx ->
            try {
                val body = ctx.bodyAsJson
                val id = body.getString("id")
                val desc = body.getString("desc")
                val payload = body.getString("payload", "{}")

                if (id == null || desc == null) {
                    ctx.response().setStatusCode(400).end("Missing id or desc")
                    return@handler
                }

                log.info("HTTP Registering semantic service: $id - $desc")
                val vector = embeddingService.embed(desc)
                hnswIndexService.add(id, vector, payload)
                instanceTable.register(id)
                ctx.json(JsonObject().put("status", "ok"))
            } catch (e: Exception) {
                log.error("HTTP Registration failed", e)
                ctx.response().setStatusCode(500).end(e.message)
            }
        }
        
        // POST /semantic/api/search
        router.post("/semantic/api/search").handler { ctx ->
             try {
                val body = ctx.bodyAsJson
                val query = body.getString("query")
                val k = body.getInteger("k", 5)

                if (query == null) {
                    ctx.response().setStatusCode(400).end("Missing query")
                    return@handler
                }

                val vector = embeddingService.embed(query)
                val results = hnswIndexService.search(vector, k)
                
                val jsonResults = results.map { 
                    JsonObject()
                        .put("id", it.id)
                        .put("distance", it.distance)
                        .put("payload", JsonObject(it.payload))
                }
                
                ctx.json(JsonArray(jsonResults))
            } catch (e: Exception) {
                log.error("HTTP Search failed", e)
                ctx.response().setStatusCode(500).end(e.message)
            }
        }

        // POST /semantic/api/unregister
        router.post("/semantic/api/unregister").handler { ctx ->
            try {
                val body = ctx.bodyAsJson
                val id = body.getString("id")

                if (id == null) {
                    ctx.response().setStatusCode(400).end("Missing id")
                    return@handler
                }

                log.info("HTTP Unregistering semantic service: $id")
                hnswIndexService.remove(id)
                instanceTable.unregister(id)
                ctx.json(JsonObject().put("status", "ok"))
            } catch (e: Exception) {
                log.error("HTTP Unregistration failed", e)
                ctx.response().setStatusCode(500).end(e.message)
            }
        }

        // POST /semantic/api/heartbeat
        router.post("/semantic/api/heartbeat").handler { ctx ->
            try {
                val body = ctx.bodyAsJson
                val id = body.getString("id")

                if (id == null) {
                    ctx.response().setStatusCode(400).end("Missing id")
                    return@handler
                }

                if (instanceTable.heartbeat(id)) {
                    ctx.json(JsonObject().put("status", "ok"))
                } else {
                    ctx.response().setStatusCode(404).end("Instance not found: $id")
                }
            } catch (e: Exception) {
                log.error("HTTP Heartbeat failed", e)
                ctx.response().setStatusCode(500).end(e.message)
            }
        }
        
        // Default Port 8099
        val port = config().getInteger("semantic.registry.port", 8099)
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port) { ar ->
                if (ar.succeeded()) {
                    log.info("Semantic Registry HTTP Server started on port $port")
                } else {
                    log.error("Failed to start Semantic Registry HTTP Server", ar.cause())
                }
            }

        /**
         * 启动超时检查定时器: 每30秒检查一次
         * 
         * 并发安全设计说明 
         * 1. 核心挑战：心跳检测线程(Worker/HTTP)与索引清理线程(Periodic Timer)对同一实例状态的竞争。
         * 2. 解决方案：通过 InstanceTable.removeAndGetExpired() 中的 ConcurrentHashMap.computeIfPresent 实现。
         * 3. 逻辑闭环：即便清理线程判定实例已超时，但在执行原子删除前若收到了心跳并更新了 timestamp，
         *    computeIfPresent 内部的二次时间校验将判定为“有效”并保留实例。
         * 4. 索引一致性：只有在从 InstanceTable 原子移除成功后，才执行 HnswIndexService.remove，
         *    确保了语义索引与存活状态的最终一致性。
         */
        vertx.setPeriodic(30_000) {
            val expiredInstances = instanceTable.removeAndGetExpired()
            expiredInstances.forEach { id ->
                log.warn("Instance $id expired (TTL exceeded), removing from index")
                hnswIndexService.remove(id)
            }
            if (expiredInstances.isNotEmpty()) {
                log.info("Removed ${expiredInstances.size} expired instances atomically.")
            }
        }

        // 注册服务: { "id": "serviceId", "desc": "description text", "payload": "json metadata" }
        eb.consumer<JsonObject>("minih.semantic.register") { message ->
            try {
                val body = message.body()
                val id = body.getString("id")
                val desc = body.getString("desc")
                val payload = body.getString("payload", "{}")

                if (id == null || desc == null) {
                    message.fail(400, "Missing id or desc")
                    return@consumer
                }

                log.info("Registering semantic service: $id - $desc")
                val vector = embeddingService.embed(desc)
                hnswIndexService.add(id, vector, payload)
                instanceTable.register(id)
                message.reply(JsonObject().put("status", "ok"))
            } catch (e: Exception) {
                log.error("Registration failed", e)
                message.fail(500, e.message)
            }
        }

        // 语义搜素: { "query": "text", "k": 5 }
        eb.consumer<JsonObject>("minih.semantic.search") { message ->
            try {
                val body = message.body()
                val query = body.getString("query")
                val k = body.getInteger("k", 5)

                if (query == null) {
                    message.fail(400, "Missing query")
                    return@consumer
                }

                val vector = embeddingService.embed(query)
                val results = hnswIndexService.search(vector, k)
                
                val jsonResults = results.map { 
                    JsonObject()
                        .put("id", it.id)
                        .put("distance", it.distance)
                        .put("payload", JsonObject(it.payload))
                }
                
                message.reply(io.vertx.core.json.JsonArray(jsonResults))
            } catch (e: Exception) {
                log.error("Search failed", e)
                message.fail(500, e.message)
            }
        }

        // 取消注册服务: { "id": "serviceId" }
        eb.consumer<JsonObject>("minih.semantic.unregister") { message ->
            try {
                val body = message.body()
                val id = body.getString("id")

                if (id == null) {
                    message.fail(400, "Missing id")
                    return@consumer
                }

                log.info("Unregistering semantic service: $id")
                hnswIndexService.remove(id)
                instanceTable.unregister(id)
                message.reply(JsonObject().put("status", "ok"))
            } catch (e: Exception) {
                log.error("Unregistration failed", e)
                message.fail(500, e.message)
            }
        }
        
        log.info("SemanticRegistryVerticle deployed. Listening on minih.semantic.*")
    }
}
