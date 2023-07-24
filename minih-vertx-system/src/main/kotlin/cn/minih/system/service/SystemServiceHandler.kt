package cn.minih.system.service

import cn.minih.core.utils.Assert
import cn.minih.core.utils.smsSendVerifyCode
import cn.minih.system.exception.MinihSystemErrorCode
import cn.minih.system.exception.SystemException

/**
 * @author hubin
 * @date 2023/7/24
 * @desc
 */
object SystemServiceHandler {
    suspend fun smsSendVCode(mobile: String) {
        Assert.notBlank(mobile) {
            SystemException(
                msg = "待发送手机号不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        val pattern = "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}\$"
        Assert.isTrue(mobile.matches(Regex(pattern))) {
            SystemException(
                msg = "待发送手机号格式不正确！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        smsSendVerifyCode(mobile)
    }
}