package cn.minih.app.system.user

import cn.minih.app.system.config.RepositoryManager
import cn.minih.app.system.config.RouteFailureHandler
import io.vertx.core.json.JsonObject

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class UserRepository private constructor() : RepositoryManager("sys_user") {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UserRepository()
        }
    }

    suspend fun getUserByUsername(username: String): JsonObject? {
        return findOne("username" to username)
    }
}
