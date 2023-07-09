package cn.minih.app.system.exception

/**
 * @author hubin
 * @date 2023/7/8
 * @desc
 */
class PasswordErrorException : RuntimeException() {
    override val message: String
        get() = "密码错误"
}