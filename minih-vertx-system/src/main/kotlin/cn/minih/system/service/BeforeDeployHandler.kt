package cn.minih.system.service

import cn.minih.auth.logic.AuthServiceHandler
import cn.minih.core.annotation.Component
import cn.minih.core.handler.BeforeDeployHandler
import io.vertx.core.Vertx

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
@Component
class BeforeDeployHandler : BeforeDeployHandler {
    override fun exec(vertx: Vertx) {
        AuthServiceHandler.instance.setAuthService(AuthServiceImpl.instance)





    }
}
