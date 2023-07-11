package cn.minih.app.system.user

import cn.minih.app.system.auth.AuthUtil
import cn.minih.app.system.constants.MinihErrorCode
import cn.minih.app.system.exception.UserSystemException
import cn.minih.app.system.user.data.UserInfo
import cn.minih.app.system.utils.Assert

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object UserServiceHandler {

    suspend fun getUserInfo(): UserInfo? {
        val username = AuthUtil.getCurrentLoginId()
        val sysUser = UserRepository.instance.getUserByUsername(username)
        Assert.notNull(sysUser) { UserSystemException(errorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM_DATA_UN_FIND) }
        return sysUser?.let { UserInfo(it) }
    }



}
