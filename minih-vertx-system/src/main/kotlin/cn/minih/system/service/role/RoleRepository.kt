package cn.minih.system.service.role

import cn.minih.core.annotation.Component

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
@Component
class RoleRepository private constructor() {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RoleRepository()
        }
    }


}
