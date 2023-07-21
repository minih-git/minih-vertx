package cn.minih.system.consumer

import cn.minih.auth.constants.AUTH_SESSION_KEEP
import cn.minih.core.annotation.Component
import cn.minih.core.handler.EventBusConsumer
import cn.minih.core.utils.covertTo
import cn.minih.core.utils.notBlankAndExec
import cn.minih.system.data.user.SysUser
import cn.minih.system.service.user.UserRepository
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
@Component
class TokenSignConsumer : EventBusConsumer(AUTH_SESSION_KEEP) {
    override suspend fun exec(obj: JsonObject) {
        val sysUser =
            UserRepository.instance.findOne("username" to obj.getString("loginId"))?.await()?.covertTo(SysUser::class)
        sysUser?.notBlankAndExec {
            it.lastActive = Date().time
            UserRepository.instance.update("_id" to it.id, data = it)
        }
    }
}
