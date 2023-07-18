package cn.minih.auth.constants

import cn.minih.core.exception.IMinihErrorCode
import cn.minih.core.exception.MinihErrorCode

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
enum class MinihAuthErrorCode(override val code: Int, override val msg: String): IMinihErrorCode {

    //鉴权系统错误
    ERR_CODE_LOGIN(-10, "登录时产生错误"),
    ERR_CODE_LOGIN_NO_TOKEN(-11, "未能读取到有效 token"),
    ERR_CODE_LOGIN_PASSWORD_ERROR(-12, "密码错误"),
    ERR_CODE_LOGIN_TOKEN_INVALID(-13, "token 无效"),
    ERR_CODE_LOGIN_TOKEN_TIMEOUT(-14, "token 已过期"),
    ERR_CODE_LOGIN_TOKEN_KICK_OUT(-15, "token 已被踢下线"),
    ERR_CODE_LOGIN_TOKEN_FREEZE(-16, "token 已被冻结"),
    ERR_CODE_LOGIN_TOKEN_NO_AUTH(-17, "未授权!"),
    ERR_CODE_LOGIN_NO_LOGIN_INFO(-18, "未能读取到有效的登录信息"),


    //用户系统错误
    ERR_CODE_USER_SYSTEM(-20, "用户系统产生错误"),
    ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT(-21, "非法参数"),
    ERR_CODE_USER_SYSTEM_DATA_UN_FIND(-22, "未找到数据")
    ;
}
