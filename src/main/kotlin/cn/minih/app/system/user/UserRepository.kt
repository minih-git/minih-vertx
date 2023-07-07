package cn.minih.app.system.user

import cn.minih.app.system.config.RepositoryManager
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class UserRepository(vertx: Vertx) : RepositoryManager(vertx) {
    private val tableName = "sys_user"
    fun getUserByUsername(username: String): Future<JsonObject> {
        val document = JsonObject().put("username", username)
        return client.findOne(tableName, document, document)
    }
}
