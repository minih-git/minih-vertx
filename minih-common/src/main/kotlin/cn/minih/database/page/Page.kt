@file:Suppress("unused")

package cn.minih.database.page

/**
 * @author hubin
 * @date 2023/7/14
 * @desc
 */


@Suppress("unused")
data class Page<T>(
    var nextCursor: Long = 0,
    var data: List<T> = mutableListOf(),
    var pageSize: Int = 10,
    var pageType: PageType = PageType.CURSOR
)

enum class PageType {
    CURSOR, OFFSET
}