package cn.minih.auth.components

import cn.minih.core.exception.MinihException
import cn.minih.core.utils.R
import cn.minih.core.utils.toJsonObject
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.ErrorHandler

/**
 * @author hubin
 * @date 2023/7/8
 * @desc
 */
class RouteFailureHandler private constructor() : Handler<RoutingContext>, ErrorHandler {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RouteFailureHandler()
        }
    }

    override fun handle(ctx: RoutingContext) {
        var r: R<String>
        val ex = ctx.failure()
        if (ctx.statusCode() == 200 || ex != null) {
            ex.printStackTrace()
            r = R.err(ex.message)
            if (ex is MinihException) {
                r = R.err(ex.msg, ex.errorCode)
            }
        } else {
            val msg = when (ctx.statusCode()) {
                400 -> "请求出错！"
                401 -> "未授权！"
                404 -> "路径不存在！"
                405 -> "不允许此方法！"
                else -> "未分类错误！"
            }
            r = R.err(ctx.statusCode(), msg)
        }
        ctx.json(r.toJsonObject())
    }


}
