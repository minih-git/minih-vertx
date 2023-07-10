package cn.minih.app.system.config

import cn.minih.app.system.auth.AuthServiceHandler

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
class CommonHandler private constructor() : Handler<RoutingContext> {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            CommonHandler()
        }
    }

    override fun handle(ctx: RoutingContext) {
        ctx.addHeadersEndHandler { _ ->
            val origin = ctx.request().getHeader("Origin")
            if (origin != null && origin.isNotEmpty()) {
                val res = ctx.response()
                res.putHeader("Access-Control-Allow-Origin", origin)
                res.putHeader("Access-Control-Allow-Credentials", "true")
                res.putHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE")
                res.putHeader(
                    "Access-Control-Allow-Headers",
                    "Authorization, Content-Type, If-Match, If-Modified-Since, If-None-Match, If-Unmodified-Since, X-Requested-With"
                )
            }
        }
        val origin = ctx.request().getHeader("Origin")
        if (origin != null && origin.isNotEmpty() && ctx.request().method() == HttpMethod.OPTIONS) {
            ctx.end("")
        } else {
            ctx.next()
        }
    }

}
