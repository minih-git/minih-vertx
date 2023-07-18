package cn.minih.core.exception



/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
open class MinihException(open val msg: String? = null, open val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_UNDEFINED) :
    RuntimeException("错误代码:${errorCode.code},${msg ?: errorCode.msg}"){

    }
