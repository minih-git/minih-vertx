package cn.minih.app.system.user

import cn.minih.app.system.config.RepositoryManager
import cn.minih.app.system.user.data.SysUser
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class UserRepository private constructor() : RepositoryManager<SysUser>("sysUser") {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UserRepository()
        }
    }

    fun getUserByUsername(username: String): Future<JsonObject>? {
        return findOne("username" to username)
    }
}