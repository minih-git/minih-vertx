package cn.minih.app.system.user

import cn.minih.app.system.config.RepositoryManager
import io.vertx.core.json.JsonObject

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class UserRepository : RepositoryManager( "sys_user") {
    suspend fun getUserByUsername(username: String): JsonObject? {
        return findOne("username" to username)
    }
}