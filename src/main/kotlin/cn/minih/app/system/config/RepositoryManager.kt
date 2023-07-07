package cn.minih.app.system.config

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
open class RepositoryManager(vertx: Vertx) {
    var client: MongoClient = MongoClient.createShared(
        vertx,
        JsonObject()
            .put("host", "dds-wz9019cf0593d9141601-pub.mongodb.rds.aliyuncs.com")
            .put("port", 3717)
            .put("user", "admin")
            .put("password", "Minih@123")
            .put("db_name", "minih")
    )
}
