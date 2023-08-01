package cn.minih.database.mysql.page

/**
 * @author hubin
 * @date 2023/7/14
 * @desc
 */


@Suppress("unused")
data class Page<T>(
    val nextCursor: Long,
    var data: List<T>?,
    val pageSize:Int = 10,
)