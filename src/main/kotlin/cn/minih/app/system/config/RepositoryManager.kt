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
abstract class RepositoryManager(private val tableName: String) {


    private val client by lazy {
        val config = Vertx.currentContext().config()
        val mongoOptions = jsonObjectOf(
            "host" to config.getString("mongodb.host"),
            "port" to 3717,
            "username" to config.getString("mongodb.username"),
            "password" to config.getString("mongodb.password"),
            "authSource" to (config.getString("mongodb.source") ?: "admin"),
            "socketTimeoutMS" to 500000,
            "serverSelectionTimeoutMS" to 50000,
            "maxIdleTimeMS" to 300000,
            "maxLifeTimeMS" to 3600000,
            "db_name" to "minih",
        )
        MongoClient.createShared(Vertx.currentContext().owner(), mongoOptions)
    }


    suspend fun findOne(vararg fields: Pair<String, Any?>): JsonObject? {
        val document = jsonObjectOf(*fields)
        return client.findOne(tableName, document, jsonObjectOf()).await()

    }


}