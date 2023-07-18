package cn.minih.auth.exception

import cn.minih.auth.constants.MinihAuthErrorCode


/**
 * @author hubin
 * @date 2023/7/8
 * @desc
 */
class PasswordErrorException : MinihAuthException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_PASSWORD_ERROR)
