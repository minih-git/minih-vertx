package cn.minih.app.system.config

import cn.minih.app.system.exception.MinihException
import cn.minih.app.system.utils.R
import cn.minih.app.system.utils.toJsonObject
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

/**
 * @author hubin
 * @date 2023/7/8
 * @desc
 */
class RouteFailureHandler private constructor() : Handler<RoutingContext> {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RouteFailureHandler()
        }
    }

    override fun handle(ctx: RoutingContext) {
        val ex = ctx.failure()
        var r = R.err(ex.message)
        if (ex is MinihException) {
            r = R.err(null, ex.errorCode)
        }
        ctx.json(r.toJsonObject())
    }


}
