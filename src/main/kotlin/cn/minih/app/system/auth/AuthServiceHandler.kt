package cn.minih.app.system.auth

import cn.minih.app.system.user.UserRepository
import io.vertx.ext.web.RoutingContext

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class AuthServiceHandler(private val userRepository: UserRepository) {
    suspend fun login(ctx: RoutingContext) {
        val body = ctx.body().asJsonObject()
        val user = userRepository.getUserByUsername(body.getString("username"))




        ctx.json(user)

    }
}