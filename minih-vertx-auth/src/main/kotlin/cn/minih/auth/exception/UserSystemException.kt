package cn.minih.auth.exception

import cn.minih.auth.constants.MinihAuthErrorCode
import cn.minih.core.exception.IMinihErrorCode


/**
 * @author hubin
 * @date 2023/7/11
 * @desc
 */
class UserSystemException(msg: String? = null, errorCode: IMinihErrorCode = MinihAuthErrorCode.ERR_CODE_USER_SYSTEM) :
    MinihAuthException(msg, errorCode)
