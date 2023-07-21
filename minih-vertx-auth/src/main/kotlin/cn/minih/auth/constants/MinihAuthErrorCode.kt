package cn.minih.auth.constants

import cn.minih.core.exception.IMinihErrorCode
import cn.minih.core.exception.MinihErrorCode

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
enum class MinihAuthErrorCode(override val code: Int, override val msg: String) : IMinihErrorCode {

    //鉴权系统错误
    ERR_CODE_LOGIN(-10, "登录时产生错误"),
    ERR_CODE_LOGIN_NO_TOKEN(-11, "未能读取到有效令牌"),
    ERR_CODE_LOGIN_TOKEN_INVALID(-13, "账户令牌无效"),
    ERR_CODE_LOGIN_TOKEN_TIMEOUT(-14, "登录已过期"),
    ERR_CODE_LOGIN_TOKEN_KICK_OUT(-15, "您已被踢下线"),
    ERR_CODE_LOGIN_TOKEN_LOGOUT(-16, "已退出登录"),
    ERR_CODE_LOGIN_TOKEN_REPLACED_OUT(-17, "已在其他地方登录"),
    ERR_CODE_LOGIN_TOKEN_FREEZE(-18, "账户已被冻结"),
    ERR_CODE_LOGIN_TOKEN_NO_AUTH(-19, "未授权!"),
    ERR_CODE_LOGIN_NO_LOGIN_INFO(-20, "未能读取到有效的登录信息"),
    ;
}
