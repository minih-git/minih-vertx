@file:Suppress("unused")

package cn.minih.database.mysql.annotation

/**
 *  表名
 * @author hubin
 * @since 2023-07-27 11:29:30
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TableName(val value: String)