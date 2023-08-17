@file:Suppress("unused")

package cn.minih.database.mysql.enum

/**
 * 数据实体类开关枚举
 * @author hubin
 * @since 2023-07-30 13:56:12
 * @desc
 */
@Suppress("unused")
enum class DataStateType { Y, N }

fun stateToBool(type: DataStateType): Boolean = type == DataStateType.Y
fun boolToState(bl: Boolean): DataStateType = if (bl) DataStateType.Y else DataStateType.N

operator fun DataStateType.not(): DataStateType {
    return if (this == DataStateType.Y) {
        DataStateType.N
    } else {
        DataStateType.Y
    }
}
