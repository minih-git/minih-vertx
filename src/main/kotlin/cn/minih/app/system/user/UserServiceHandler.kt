package cn.minih.app.system.user

import cn.minih.app.system.auth.data.AuthSession
import io.vertx.core.json.JsonObject

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object UserServiceHandler {


    @A
    fun getData(params: JsonObject): AuthSession {
        println(params)
        return AuthSession()
    }


}


@Target(AnnotationTarget.FUNCTION)
annotation class A
