@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.minih.web.response

import cn.minih.common.exception.IMinihErrorCode
import cn.minih.common.exception.MinihErrorCode
import cn.minih.common.util.toJsonString


/**
 * @author hubin
 * @date 2023/3/15
 * @desc
 */
data class R<T>(var code: Int?, var msg: String?, val data: T?, val encrypt: Boolean = false) {
    companion object {
        fun <T> ok(data: T?): R<T> {
            var rce = MinihErrorCode.SUCCESS_CODE_UNDEFINED
            if (data is Boolean && !data) {
                rce = MinihErrorCode.ERR_CODE_UNDEFINED
            }
            return R(rce.code, rce.msg, data)
        }

        fun <T> encryptOk(data: T?): R<T> {
            var rce = MinihErrorCode.SUCCESS_CODE_UNDEFINED
            if (data is Boolean && !data) {
                rce = MinihErrorCode.ERR_CODE_UNDEFINED
            }
            return R(rce.code, rce.msg, data, true)
        }

        fun <T> ok(): R<T> {
            return ok(null)
        }


        fun <T> err(data: T?, errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_UNDEFINED): R<T> {
            return R(errorCode.code, errorCode.msg, data)
        }

        fun err(errorCode: MinihErrorCode = MinihErrorCode.ERR_CODE_UNDEFINED): R<Any> {
            return R(errorCode.code, errorCode.msg, null)
        }

        fun err(code: Int, msg: String): R<String> {
            return R(code, msg, null)
        }

        fun <T> err(): R<T> {
            return err(null)
        }
    }

    override fun toString(): String {
        return this.toJsonString()
    }
}