package cn.minih.auth.constants

import cn.minih.common.exception.IMinihErrorCode


/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
enum class MinihAuthErrorCode(override val code: Int, override val msg: String) : IMinihErrorCode {

    //鉴权系统错误
    ERR_CODE_LOGIN(-10, "登录时产生错误"),
    ERR_CODE_LOGIN_NO_TOKEN(-11, "未能读取到有效令牌"),
    ERR_CODE_LOGIN_TOKEN_INVALID(-12, "账户令牌无效"),
    ERR_CODE_LOGIN_TOKEN_TIMEOUT(-13, "登录已过期"),
    ERR_CODE_LOGIN_TOKEN_KICK_OUT(-14, "您已被踢下线"),
    ERR_CODE_LOGIN_TOKEN_LOGOUT(-15, "已退出登录"),
    ERR_CODE_LOGIN_TOKEN_REPLACED_OUT(-16, "已在其他地方登录"),
    ERR_CODE_LOGIN_TOKEN_FREEZE(-17, "账户已被冻结"),
    ERR_CODE_LOGIN_TRY_MAX_TIMES(-18, "错误次数过多，账户已锁定"),
    ERR_CODE_LOGIN_TOKEN_NO_AUTH(-19, "未授权!"),
    ;
}