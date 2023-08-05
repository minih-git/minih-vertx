@file:Suppress("unused")

package cn.minih.common.util

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

    fun <T> notNull(obj: T?, fn: () -> Throwable) {
        if (obj == null) {
            throw fn()
        }
    }

    fun <T> isNull(obj: T?, fn: () -> Throwable) {
        if (obj != null) {
            throw fn()
        }
    }

    fun isTrue(obj: Boolean?, fn: () -> Throwable) {
        if (obj == null || !obj) {
            throw fn()
        }

    }


}