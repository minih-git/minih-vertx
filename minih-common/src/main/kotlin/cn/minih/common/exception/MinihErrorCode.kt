package cn.minih.common.exception

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
interface IMinihErrorCode {
    val code: Int
    val msg: String
}

@Suppress("unused")
enum class MinihErrorCode(override val code: Int, override val msg: String) : IMinihErrorCode {
    SUCCESS_CODE_UNDEFINED(0, "成功"),
    ERR_CODE_UNDEFINED(-1, "未指定错误类型"),
    ERR_CODE_ARGUMENT_ERROR(-2, "参数错误"),
    ERR_CODE_NOT_FOUND_ERROR(-3, "未找到数据"),
    ERR_CODE_DATA_DECRYPTION_ERROR(-4, "数据解密出现错误"),
    ERR_CODE_REMOTE_CALL_ERROR(-5, "远程接口调用出错"),
    ERR_CODE_DATA_COVERT_ERROR(-6, "数据转换出现错误"),

}
