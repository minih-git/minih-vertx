@file:Suppress("unused")

package cn.minih.database.mysql.annotation

/**
 *  逻辑删除字段
 * @author hubin
 * @since 2023-08-15 16:34:11
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogicKey
