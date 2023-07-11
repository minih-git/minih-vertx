package cn.minih.app.system.user

import cn.minih.app.system.config.RepositoryManager
import cn.minih.app.system.user.data.SysUser
import cn.minih.app.system.utils.covertTo

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class UserRepository private constructor() : RepositoryManager("sysUser") {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UserRepository()
        }
    }

    suspend fun getUserByUsername(username: String): SysUser? {
        return findOne("username" to username)?.covertTo(SysUser::class)
    }
}
