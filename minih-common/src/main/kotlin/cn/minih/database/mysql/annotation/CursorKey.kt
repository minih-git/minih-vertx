@file:Suppress("unused")

package cn.minih.database.mysql.annotation

import cn.minih.database.mysql.enum.OrderByType

/**
 *  分页排序字段
 * @author hubin
 * @since 2023-08-18 13:50:26
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CursorKey(val type: OrderByType = OrderByType.ASC)
