package cn.minih.app.system.exception

import cn.minih.app.system.constants.MinihErrorCode

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
class AuthLoginException(msg: String? = null, errorCode: MinihErrorCode = MinihErrorCode.ERR_CODE_LOGIN) :
    MinihException(msg, errorCode)
