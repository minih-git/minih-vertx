package cn.minih.database.mysql.annotation

import cn.minih.database.mysql.enum.TableIdType

/**
 *  表主键
 * @author hubin
 * @since 2023-07-27 11:29:30
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TableId(val value: TableIdType = TableIdType.INPUT)