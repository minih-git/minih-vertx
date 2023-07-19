package cn.minih.system.service.role

import cn.minih.core.annotation.Component
import cn.minih.core.repository.RepositoryManager
import cn.minih.system.data.role.SysRole

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
@Component
class RoleRepository private constructor() : RepositoryManager<SysRole>("sysRole") {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RoleRepository()
        }
    }
}
