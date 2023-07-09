package cn.minih.app.system.config

import cn.minih.app.system.exception.PasswordErrorException
import cn.minih.app.system.utils.R
import cn.minih.app.system.utils.RCode
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

/**
 * @author hubin
 * @date 2023/7/8
 * @desc
 */
class RouteFailureHandler : Handler<RoutingContext> {
    companion object {
        fun create(): RouteFailureHandler {
            return RouteFailureHandler()
        }
    }

    override fun handle(ctx: RoutingContext) {
        val ex = ctx.failure()
        ex.printStackTrace()
        var r = R.err(ex.message)
        if (ex is PasswordErrorException) {
            r = R.err(RCode.LOGIN_ERROR, ex.message)
        }
        ctx.json(r.toString())
    }


}