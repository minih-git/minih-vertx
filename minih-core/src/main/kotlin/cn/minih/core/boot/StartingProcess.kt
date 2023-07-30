package cn.minih.core.boot

import io.vertx.core.Vertx

/**
 * 启动前执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PreStartingProcess {
    fun exec(vertx: Vertx)
}

/**
 * 启动后执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PostStartingProcess {
    fun exec(vertx: Vertx)
}