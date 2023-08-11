@file:Suppress("unused", "CanBeParameter")

package cn.minih.common.exception


/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
open class MinihException(
    open val msg: String? = null,
    open val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_UNDEFINED
) : RuntimeException("错误代码:${errorCode.code},${msg ?: errorCode.msg}")

open class MinihArgumentErrorException(
    override val msg: String? = null,
    override val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_ARGUMENT_ERROR
) : MinihException(msg, errorCode)

class MinihDataDecryptionException(
    override val msg: String? = null,
    override val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_DATA_DECRYPTION_ERROR
) : MinihException(msg, errorCode)

class MinihDataCovertException(
    override val msg: String? = null,
    override val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_DATA_COVERT_ERROR
) : MinihException(msg, errorCode)
