package cn.minih.app.system.config

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.FindOptions
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.mongo.MongoClientUpdateResult
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

    fun findOne(queryOption: JsonObject): Future<JsonObject>? {
        return client.findOne(tableName, queryOption, jsonObjectOf())
    }

    fun find(
        queryOption: JsonObject,
        options: FindOptions = FindOptions().setBatchSize(100)
    ): Future<List<JsonObject>>? {
        return client.findWithOptions(tableName, queryOption, options)
    }

    fun insert(vararg fields: Pair<String, Any?>): Future<String> {
        return client.save(tableName, jsonObjectOf(*fields))
    }

    fun delete(vararg fields: Pair<String, Any?>): Future<JsonObject> {
        return client.findOneAndDelete(tableName, jsonObjectOf(*fields))
    }

    fun update(vararg fields: Pair<String, Any?>, updateData: JsonObject): Future<MongoClientUpdateResult> {
        return client.updateCollection(tableName, jsonObjectOf(*fields), updateData)
    }


}
