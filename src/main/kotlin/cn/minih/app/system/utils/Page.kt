package cn.minih.app.system.utils

/**
 * @author hubin
 * @date 2023/7/14
 * @desc
 */
open class IPage(open val nextCursor: Long) {

}

data class Page<T>(
    override val nextCursor: Long,
    val data: List<T>
) : IPage(nextCursor)
