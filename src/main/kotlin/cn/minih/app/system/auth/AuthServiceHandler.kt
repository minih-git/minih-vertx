package cn.minih.app.system.auth

import cn.minih.app.system.user.UserRepository
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class AuthServiceHandler(private val userRepository: UserRepository) {
    suspend fun login(ctx : RoutingContext) {
        val user =  userRepository.getUserByUsername("admin").await()
        println(user)

    }
}
