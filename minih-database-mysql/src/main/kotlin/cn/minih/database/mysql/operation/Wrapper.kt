package cn.minih.database.mysql.operation

/**
 * 包装类
 * @author hubin
 * @since 2023-07-27 22:31:56
 * @desc
 */
abstract class Wrapper<T> {
    val updateItems: MutableList<UpdateItem> = mutableListOf()
    val condition: MutableList<QueryCondition> = mutableListOf()
    val orderByItems: MutableList<OrderByItem> = mutableListOf()
}
