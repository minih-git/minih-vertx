package cn.minih.app.system.utils

/**
 * @author hubin
 * @date 2023/7/14
 * @desc
 */


data class Page<T>(
    val nextCursor: Long,
    val data: List<T>
)