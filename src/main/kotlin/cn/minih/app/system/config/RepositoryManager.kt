package cn.minih.app.system.config

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
abstract class RepositoryManager(vertx: Vertx, val tableName: String) {

    var client: MongoClient = MongoClient.createShared(
        vertx,
        JsonObject()
            .put("host", System.getenv("mongodb.host"))
            .put("port", 3717)
            .put("username", System.getenv("mongodb.username"))
            .put("password", System.getenv("mongodb.password"))
            .put("authSource", System.getenv("mongodb.source"))
            .put("db_name", "minih")
    )

    suspend fun findOne(vararg fields: Pair<String, Any?>): JsonObject? {
        val document = jsonObjectOf(*fields)
        return client.findOne(tableName, document, jsonObjectOf()).await()
    }


}