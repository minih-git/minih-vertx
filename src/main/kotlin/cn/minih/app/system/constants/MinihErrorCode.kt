package cn.minih.app.system.constants

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
enum class MinihErrorCode(val code: Int, val msg: String) {
    SUCCESS_UNDEFINED(0, "成功"),
    ERR_CODE_UNDEFINED(-1, "未指定错误类型"),
    ERR_CODE_LOGIN(-2, "登录时产生错误"),
    ERR_CODE_LOGIN_NO_TOKEN(-3, "未能读取到有效 token"),
    ERR_CODE_LOGIN_PASSWORD_ERROR(-4, "密码错误"),
    ERR_CODE_LOGIN_TOKEN_INVALID(-5, "token 无效"),
    ERR_CODE_LOGIN_TOKEN_TIMEOUT(-6, "token 已过期"),
    ERR_CODE_LOGIN_TOKEN_KICK_OUT(-7, "token 已被踢下线"),
    ERR_CODE_LOGIN_TOKEN_FREEZE(-7, "token 已被冻结"),
    ERR_CODE_LOGIN_TOKEN_NO_AUTH(-8, "无权访问")
    ;
}