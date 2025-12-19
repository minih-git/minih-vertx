package cn.minih.gateway.controller

import cn.minih.core.annotation.Service
import cn.minih.web.annotation.Post
import cn.minih.web.annotation.Request
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait

@Request("/semantic")
interface SemanticController: cn.minih.web.service.Service {
    @Post("/query")
    suspend fun query(query: String): JsonArray
}

@Service("semanticController")
class SemanticControllerImpl: SemanticController {
    override suspend fun query(query: String): JsonArray {
        val vertx = Vertx.currentContext().owner()
        val client = io.vertx.ext.web.client.WebClient.create(vertx)
        
        // Assuming Registry is at localhost:8099. Should be configurable in real app.
        val registryHost = "localhost"
        val registryPort = 8099
        
        val response = client.post(registryPort, registryHost, "/semantic/api/search")
            .sendJsonObject(JsonObject().put("query", query))
            .coAwait()
            
        if (response.statusCode() == 200) {
            return response.bodyAsJsonArray()
        } else {
             // Fallback or empty result on error
             return JsonArray()
        }
    }
}