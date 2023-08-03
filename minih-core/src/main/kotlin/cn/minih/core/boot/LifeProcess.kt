package cn.minih.core.boot

import io.vertx.core.Vertx

/**
 * 启动前执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PreStartingProcess {
    suspend fun exec(vertx: Vertx)
}

/**
 * 启动后执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PostStartingProcess {
    suspend fun exec(vertx: Vertx)
}

/**
 * 部署后后执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PostDeployingProcess {
    suspend fun exec(vertx: Vertx, deployId: String)
}

/**
 * 停止前执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PreStopProcess {
    suspend fun exec(vertx: Vertx)
}