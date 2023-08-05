package cn.minih.web.util

import cn.minih.web.annotation.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * web工具类
 * @author hubin
 * @since 2023-08-05 18:09:49
 * @desc
 */


fun findRequestMapping(t: KAnnotatedElement): RequestMapping? {
    val requestType = when {
        t.findAnnotation<Post>() != null -> t.findAnnotation<Post>()
        t.findAnnotation<Get>() != null -> t.findAnnotation<Get>()
        t.findAnnotation<Put>() != null -> t.findAnnotation<Put>()
        t.findAnnotation<Delete>() != null -> t.findAnnotation<Delete>()
        t.findAnnotation<Request>() != null -> t.findAnnotation<Request>()
        else -> null
    } ?: return null
    val fields = requestType::class.memberProperties
    var url = ""
    fields.forEach { field ->
        if (field.name == "value") {
            url = field.getter.call(requestType).toString()
        }
    }

    return RequestMapping(url, requestType)
}

fun formatPath(pathTmp: String): String {
    var path = pathTmp
    if (!path.startsWith("/")) {
        path = "/$path"
    }
    if (path.endsWith("/")) {
        path = path.substring(0, path.length - 1)
    }
    return path
}