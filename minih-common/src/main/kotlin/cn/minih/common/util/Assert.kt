@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.minih.common.util

import cn.minih.common.exception.MinihErrorCode
import cn.minih.common.exception.MinihException

/**
 * @author hubin
 * @date 2023/7/11
 * @desc
 */
object Assert {
    fun <T> notBlank(v: T?, fn: () -> Throwable) {
        notNull(v, fn)
        if (v is String) {
            if (v.isBlank() || v == "null") {
                throw fn()
            }
        }
    }

    fun <T> notBlank(v: T?, message: String = "") {
        notBlank(v) { MinihException(message, MinihErrorCode.ERR_CODE_ARGUMENT_ERROR) }
    }

    fun <T> notEmpty(obj: T?, fn: () -> Throwable) {
        notNull(obj, fn)
        if (obj is String) {
            notBlank(obj, fn)
        }
        if (obj is Collection<*>) {
            if (obj.isEmpty()) {
                throw fn()
            }
        }
        if (obj is Map<*, *>) {
            if (obj.isEmpty()) {
                throw fn()
            }
        }
    }

    fun <T> notEmpty(v: T?, message: String = "") {
        notEmpty(v) { MinihException(message, MinihErrorCode.ERR_CODE_ARGUMENT_ERROR) }
    }

    fun <T> notNull(obj: T?, fn: () -> Throwable) {
        notNullOnly(obj, fn)
        if (isNullOrBlankOrZero(obj!!)) {
            throw fn()
        }
    }

    fun <T> notNull(obj: T?, message: String = "") {
        notNull(obj) { MinihException(message, MinihErrorCode.ERR_CODE_ARGUMENT_ERROR) }
    }

    fun <T> notNullOnly(obj: T?, fn: () -> Throwable) {
        if (obj == null) {
            throw fn()
        }
    }

    fun <T> notNullOnly(obj: T?, message: String = "") {
        notNullOnly(obj) { MinihException(message, MinihErrorCode.ERR_CODE_ARGUMENT_ERROR) }
    }

    fun <T> isNull(obj: T?, fn: () -> Throwable) {
        if (obj != null) {
            throw fn()
        }
    }

    fun <T> isNull(obj: T?, message: String = "") {
        isNull(obj) { MinihException(message, MinihErrorCode.ERR_CODE_ARGUMENT_ERROR) }
    }

    fun isTrue(obj: Boolean?, fn: () -> Throwable) {
        if (obj == null || !obj) {
            throw fn()
        }
    }

    fun isTrue(obj: Boolean?, message: String = "") {
        isTrue(obj) { MinihException(message, MinihErrorCode.ERR_CODE_ARGUMENT_ERROR) }
    }

}
