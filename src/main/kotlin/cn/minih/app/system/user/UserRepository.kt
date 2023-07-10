package cn.minih.app.system.user

import cn.minih.app.system.config.RepositoryManager
import cn.minih.app.system.user.data.SysUser
import cn.minih.app.system.utils.covertTo

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

    suspend fun getUserByUsername(username: String): SysUser? {
        val a = findOne("username" to username)
        println(a.toString())
        return a?.covertTo(SysUser::class)
    }
}