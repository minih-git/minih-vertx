package cn.minih.database.operation

import kotlin.reflect.KProperty1

/**
 * 更新操作
 * @author hubin
 * @since 2023-07-27 22:35:15
 * @desc
 */
interface Update<Children, R> {
    fun set(key: KProperty1<R, Any>, value: Any?): Children

    fun set(key: String, value: Any?): Children
}