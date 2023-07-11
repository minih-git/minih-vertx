package cn.minih.app.system.config

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.streams.ReadStream
import io.vertx.ext.mongo.MongoClient
import io.vertx.kotlin.core.json.jsonObjectOf

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
abstract class RepositoryManager(private val tableName: String) {


    private val client by lazy {
        val config = Vertx.currentContext().config().getJsonObject("mongodb")
        val mongoOptions = jsonObjectOf(
            "host" to config.getString("host"),
            "port" to config.getInteger("port", 3717),
            "username" to config.getString("username"),
            "password" to config.getString("password"),
            "authSource" to config.getString("source", "admin"),
            "socketTimeoutMS" to 500000,
            "serverSelectionTimeoutMS" to 50000,
            "maxIdleTimeMS" to 300000,
            "maxLifeTimeMS" to 3600000,
            "db_name" to "minih",
        )
        MongoClient.createShared(Vertx.currentContext().owner(), mongoOptions)
    }


    fun findOne(vararg fields: Pair<String, Any?>): Future<JsonObject>? {
        return client.findOne(tableName, jsonObjectOf(*fields), jsonObjectOf())
    }

    fun findBatch(vararg fields: Pair<String, Any?>): ReadStream<JsonObject>? {
        return client.findBatch(tableName, jsonObjectOf(*fields))
    }

    fun find(vararg fields: Pair<String, Any?>): Future<List<JsonObject>>? {
        return client.find(tableName, jsonObjectOf(*fields))
    }

    fun aggregate(pipeline: JsonArray): ReadStream<JsonObject>? {
        return client.aggregate(tableName, pipeline)
    }



}