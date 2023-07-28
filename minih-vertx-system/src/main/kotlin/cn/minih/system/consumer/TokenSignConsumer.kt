package cn.minih.system.consumer

import cn.minih.auth.constants.AUTH_SESSION_KEEP
import cn.minih.core.annotation.Component
import cn.minih.core.handler.EventBusConsumer
import cn.minih.core.repository.conditions.UpdateWrapper
import cn.minih.system.data.user.SysUser
import cn.minih.system.service.user.UserRepository
import io.vertx.core.json.JsonObject
import java.util.*

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
@Component
class TokenSignConsumer : EventBusConsumer(AUTH_SESSION_KEEP) {
    override suspend fun exec(obj: JsonObject) {
        UserRepository.instance.update(
            UpdateWrapper<SysUser>().eq(SysUser::username, obj.getString("loginId"))
                .set(SysUser::lastActive, Date().time)
        )
    }
}
