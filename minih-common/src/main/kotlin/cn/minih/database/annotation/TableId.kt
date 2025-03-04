@file:Suppress("unused")

package cn.minih.database.annotation

import cn.minih.database.enums.TableIdType

/**
 *  表主键
 * @author hubin
 * @since 2023-07-27 11:29:30
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TableId(val value: TableIdType = TableIdType.INPUT, val sfBusId: Long = 0L)