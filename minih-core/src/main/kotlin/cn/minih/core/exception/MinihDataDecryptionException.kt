package cn.minih.core.exception

/**
 *  数据解密错误
 * @author hubin
 * @since 2023-07-28 09:14:22
 */
@Suppress("unused")
class MinihDataDecryptionException(
    override val msg: String? = null,
    override val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_DATA_DECRYPTION_ERROR
) : MinihException(msg, errorCode) {
}