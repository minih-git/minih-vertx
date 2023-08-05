@file:Suppress("unused")

package cn.minih.database.mysql.enum

/**
 * table 主键类型枚举
 * @author hubin
 * @since 2023-07-27 23:00:17
 * @desc
 */
enum class TableIdType {
    AUTO_INCREMENT, SNOWFLAKE, UUID, INPUT
}