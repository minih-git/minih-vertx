package cn.minih.web.annotation

import kotlin.reflect.KClass

/**
 * 远程服务
 * @author hubin
 * @since 2023-08-06 12:45:22
 * @desc
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class RemoteService(
    val name: String = "",
    val remote: String = "",
    val remoteType: RemoteType = RemoteType.EVENT_BUS,
    val errorCallBack: KClass<Any> = Any::class
)

enum class RemoteType {
    HTTP_CLIENT, EVENT_BUS
}