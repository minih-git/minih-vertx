package cn.minih.system.service.user

import cn.minih.core.repository.RepositoryManager
import cn.minih.system.data.user.UserExtra

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class UserExtraRepository private constructor() : RepositoryManager<UserExtra>("userExtra") {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UserExtraRepository()
        }
    }

}
