package cn.minih.app.system.user

import cn.minih.app.system.config.RepositoryManager
import cn.minih.app.system.user.data.UserExtra

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