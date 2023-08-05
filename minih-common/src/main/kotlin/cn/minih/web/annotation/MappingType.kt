@file:Suppress("unused")

package cn.minih.web.annotation

/**
 * 请求映射
 * @author hubin
 * @since 2023-08-05 16:48:02
 * @desc
 */

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Request(val value: String = "", val type: RequestTypeEnum = RequestTypeEnum.ALL)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Post(val value: String = "", val type: RequestTypeEnum = RequestTypeEnum.POST)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Get(val value: String = "", val type: RequestTypeEnum = RequestTypeEnum.GET)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Put(val value: String = "", val type: RequestTypeEnum = RequestTypeEnum.PUT)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Delete(val value: String = "", val type: RequestTypeEnum = RequestTypeEnum.DELETE)