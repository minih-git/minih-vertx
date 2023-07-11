package cn.minih.app.system.exception

import cn.minih.app.system.constants.MinihErrorCode

/**
 * @author hubin
 * @date 2023/7/11
 * @desc
 */
class UserSystemException(msg: String? = null, errorCode: MinihErrorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM) :
    MinihException(msg, errorCode)
