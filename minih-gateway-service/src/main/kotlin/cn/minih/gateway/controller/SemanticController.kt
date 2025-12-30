package cn.minih.gateway.controller

import cn.minih.core.annotation.Service
import cn.minih.web.annotation.Post
import cn.minih.web.annotation.Request
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

@Request("/semantic")
interface SemanticController: cn.minih.web.service.Service {
    @Post("/query")
    suspend fun query(query: String): JsonArray

    @Post("/simulate")
    fun simulate(): Flow<String>
}

@Service("semanticController")
class SemanticControllerImpl: SemanticController {

    @Volatile
    private var client: io.vertx.ext.web.client.WebClient? = null

    private fun getClient(): io.vertx.ext.web.client.WebClient {
        return client ?: synchronized(this) {
            client ?: io.vertx.ext.web.client.WebClient.create(Vertx.currentContext().owner()).also { client = it }
        }
    }

    override suspend fun query(query: String): JsonArray {
        val client = getClient()
        
        val envHost = System.getenv("SEMANTIC_REGISTRY_HOST")
        val envPortStr = System.getenv("SEMANTIC_REGISTRY_PORT")

        val registryHost = if (!envHost.isNullOrBlank()) envHost else "localhost"
        val registryPort = if (!envPortStr.isNullOrBlank()) envPortStr.toInt() else 8099
        
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

    override fun simulate(): Flow<String> = flow {
        emit("start")
        // Simulate 10 seconds of work with heartbeats
        for (i in 1..10) {
            delay(1000)
            emit("heartbeat: $i")
        }
        emit("done")
    }
}