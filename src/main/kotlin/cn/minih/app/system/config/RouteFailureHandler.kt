package cn.minih.app.system.config

import cn.minih.app.system.exception.MinihException
import cn.minih.app.system.utils.R
import cn.minih.app.system.utils.toJsonObject
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

        var r = R.err("")
        if (ctx.statusCode() == 200) {
            val ex = ctx.failure()
            ex.printStackTrace()
            r = R.err(ex.message)
            if (ex is MinihException) {
                r = R.err(ex.msg, ex.errorCode)
            }
        } else {
            val msg = when(ctx.statusCode()){
                400 -> "请求出错！"
                401 -> "未授权！"
                404 -> "路径不存在！"
                405 -> "不允许此方法！"
                else ->"未分类错误！"
            }

            r = R.err(ctx.statusCode(),"")
        }
        ctx.json(r.toJsonObject())
    }


}
