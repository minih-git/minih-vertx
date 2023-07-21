package cn.minih.system.service.resource

import cn.minih.core.annotation.Component
import cn.minih.core.repository.RepositoryManager
import cn.minih.system.data.resource.SysResource

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
@Component
class ResourceRepository private constructor() : RepositoryManager<SysResource>("sysResource") {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            ResourceRepository()
        }
    }
}