package cn.minih.core.exception

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
interface IMinihErrorCode {
    val code: Int
    val msg: String
}
enum class MinihErrorCode(override val code: Int, override val msg: String):IMinihErrorCode {
    SUCCESS_CODE_UNDEFINED(0, "成功"),
    ERR_CODE_UNDEFINED(-1, "未指定错误类型"),
}
