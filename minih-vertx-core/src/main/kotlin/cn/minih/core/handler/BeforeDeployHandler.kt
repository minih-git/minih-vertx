package cn.minih.core.handler

import io.vertx.core.Vertx

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
interface BeforeDeployHandler {

    fun exec(vertx: Vertx)
}
