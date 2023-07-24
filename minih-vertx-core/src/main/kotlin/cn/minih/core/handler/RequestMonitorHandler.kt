package cn.minih.core.handler

import cn.minih.core.repository.RedisManager
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import java.util.*

/**
 * @author hubin
 * @date 2023/7/24
 * @desc
 */
class RequestMonitorHandler : Handler<RoutingContext> {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RequestMonitorHandler()
        }
    }

    override fun handle(ctx: RoutingContext) {
        val redisApi = RedisManager.instance.getReidApi()
        val path = ctx.request().path()?.replace("/", ":")
        val time = Date().time / 1000 / 60
        path?.let {
            if (path.contains("ws")) {
                return
            }
            redisApi.incr(getMonitorKey(time.toString(), path))
        }
        ctx.next()
    }

    private fun getMonitorKey(time: String, path: String): String {
        return "minih:core:monitor::$time:$path"
    }
}
