package cn.minih.system.consumer

import cn.minih.auth.constants.AUTH_SESSION_KEEP
import cn.minih.core.annotation.Component
import cn.minih.core.handler.EventBusConsumer
import cn.minih.core.utils.notBlankAndExec
import cn.minih.system.service.user.UserRepository
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
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
            UserRepository.instance.getUserByUsername(obj.getString("loginId")).await()
        sysUser?.notBlankAndExec {
            it.lastActive = Date().time
            //todo 注释代码记得
//            UserRepository.instance.update("_id" to it.id, data = it)
        }
    }
}
