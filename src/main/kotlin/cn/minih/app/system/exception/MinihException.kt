package cn.minih.app.system.exception

import cn.minih.app.system.constants.MinihErrorCode

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
open class MinihException(val msg: String? = null, val errorCode: MinihErrorCode = MinihErrorCode.ERR_CODE_UNDEFINED) :
    RuntimeException("错误代码:${errorCode.code},${msg ?: errorCode.msg}"){

    }