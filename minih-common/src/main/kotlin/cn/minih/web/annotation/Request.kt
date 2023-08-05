@file:Suppress("unused")

package cn.minih.web.annotation

/**
 * 请求
 * @author hubin
 * @since 2023-08-05 16:52:45
 * @desc
 */
enum class RequestTypeEnum {
    ALL, POST, GET, PUT, DELETE
}

data class RequestMapping(val url: String = "", val type: Annotation = Request())