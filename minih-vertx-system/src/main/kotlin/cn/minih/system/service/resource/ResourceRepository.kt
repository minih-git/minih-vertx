package cn.minih.system.service.resource

import cn.minih.core.annotation.Component

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
@Component
class ResourceRepository private constructor() {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            ResourceRepository()
        }
    }
}
