package cn.minih.core.boot

import io.vertx.core.Context
import kotlin.reflect.KClass

/**
 * 初始化bean之前操作
 */
interface ReplenishInitBeanProcess {
    suspend fun exec(context: Context, clazz: List<KClass<*>>)
}

/**
 * 启动前执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PreStartingProcess {
    suspend fun exec(context: Context)
}

/**
 * 启动后执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PostStartingProcess {
    suspend fun exec(context: Context)
}

/**
 * 部署后后执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PostDeployingProcess {
    suspend fun exec(context: Context, deployId: String)
}

/**
 * 停止前执行操作
 * @author hubin
 * @since 2023-07-30 21:18:12
 * @desc
 */
interface PreStopProcess {
    suspend fun exec(context: Context)
}
