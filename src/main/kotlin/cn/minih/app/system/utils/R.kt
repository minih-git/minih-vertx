@file:Suppress("unused")

package cn.minih.app.system.utils

import com.google.gson.Gson
import java.io.Serializable

/**
 * @author hubin
 * @date 2023/3/15
 * @desc
 */
data class R<T>(var code: String?, var msg: String?, val data: T?) : Serializable {
    companion object {
        fun <T> ok(data: T?): R<T> {
            var rce = RCode.SUCCESS
            if (data is Boolean && java.lang.Boolean.FALSE == data) {
                rce = RCode.SYSTEM_EXECUTION_ERROR
            }
            return R(rce.getCode(), rce.getMsg(), data)
        }

        fun <T> ok(): R<T> {
            return ok(null)
        }

        fun <T> err(data: T?): R<T> {
            return R(
                RCode.SYSTEM_EXECUTION_ERROR.getCode(),
                RCode.SYSTEM_EXECUTION_ERROR.getMsg(),
                data
            )
        }

        fun <T> err(respCode: RCode, data: T?): R<T> {
            return R(respCode.getCode(), respCode.getMsg(), data)
        }

        fun <T> err(): R<T> {
            return err(null)
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

interface IRCode {
    fun getCode(): String?
    fun getMsg(): String?
}

enum class RCode(private var code: String, private var msg: String) : IRCode, Serializable {
    SUCCESS("00000", "请求成功"),
    SYSTEM_EXECUTION_ERROR("S0001", "系统异常"),
    CREATE_USER_ACCOUNT_NOT_LEGITIMATE("S0003", "创建的用户帐户不合法"),
    ILLEGAL_ARGUMENT_ERROR("S0004", "非法参数"),
    LOGIN_ERROR("L0001", "登录错误"),
    NOT_LOGIN_ERROR("L0002", "未登录"),
    USER_SYSTEM_ERROR("U0001", "用户系统错误"),
    DEPT_SYSTEM_ERROR("D0001", "部门系统错误"),
    RESOURCE_SYSTEM_ERROR("R0001", "资源系统错误"),
    ROLE_SYSTEM_ERROR("R0002", "角色系统错误"),
    FILE_SYSTEM_ERROR("F001", "文件系统错误");


    override fun getCode(): String? {
        return code
    }

    override fun getMsg(): String? {
        return msg
    }
}