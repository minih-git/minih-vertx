package cn.minih.auth.exception

import cn.minih.auth.constants.MinihAuthErrorCode
import cn.minih.common.exception.IMinihErrorCode

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
class AuthLoginException(msg: String? = null, errorCode: IMinihErrorCode = MinihAuthErrorCode.ERR_CODE_LOGIN) :
    MinihAuthException(msg, errorCode)