package cn.minih.app.system.user

import cn.minih.app.system.config.RepositoryManager

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class UserExtraRepository private constructor() : RepositoryManager("userExtra") {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UserExtraRepository()
        }
    }

}
