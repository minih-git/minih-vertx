package cn.minih.core.exception


/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
open class MinihNotFoundException(
    override val msg: String? = null,
    override val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_NOT_FOUND_ERROR
) :
    MinihException(msg, errorCode)
