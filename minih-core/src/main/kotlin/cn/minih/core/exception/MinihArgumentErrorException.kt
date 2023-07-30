package cn.minih.core.exception


/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
@Suppress("unused")
open class MinihArgumentErrorException(
    override val msg: String? = null,
    override val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_ARGUMENT_ERROR
) :
    MinihException(msg, errorCode)