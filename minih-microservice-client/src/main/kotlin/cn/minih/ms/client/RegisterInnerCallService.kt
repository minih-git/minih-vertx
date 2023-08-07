package cn.minih.ms.client

import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostDeployingProcess
import io.vertx.core.Vertx

/**
 *  注册内部调用服务
 * @author hubin
 * @since 2023-08-07 09:11:53
 */
@Component
class RegisterInnerCallService : PostDeployingProcess {
    override suspend fun exec(vertx: Vertx, deployId: String) {


    }
}
