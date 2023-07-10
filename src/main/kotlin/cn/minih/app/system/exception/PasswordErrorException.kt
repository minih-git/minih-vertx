package cn.minih.app.system.exception

import cn.minih.app.system.constants.MinihErrorCode

/**
 * @author hubin
 * @date 2023/7/8
 * @desc
 */
class PasswordErrorException : MinihException(errorCode = MinihErrorCode.ERR_CODE_LOGIN_PASSWORD_ERROR)
