package cn.minih.system.service.user

import cn.minih.core.repository.RepositoryManager
import cn.minih.core.repository.conditions.QueryWrapper
import cn.minih.core.repository.conditions.UpdateWrapper
import cn.minih.system.data.user.SysUser
import io.vertx.core.Future

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class UserRepository private constructor() {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UserRepository()
        }
    }

    fun getUserByUsername(username: String): Future<SysUser?> {
        return RepositoryManager.findOne(QueryWrapper<SysUser>().eq(SysUser::username, username))
    }

    fun findOne(queryWrapper: QueryWrapper<SysUser>): Future<SysUser?> {
        return RepositoryManager.findOne(queryWrapper)
    }

    fun list(queryWrapper: QueryWrapper<SysUser>): Future<List<SysUser>?> {
        return RepositoryManager.list(queryWrapper)
    }

    fun insert(sysUser: SysUser) {
        RepositoryManager.insert(sysUser)
    }

    fun update(sysUser: SysUser) {
        RepositoryManager.update(sysUser)
    }

    fun update(wrapper: UpdateWrapper<SysUser>) {
        RepositoryManager.update(wrapper)
    }


}