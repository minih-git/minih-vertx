package cn.minih.core.util

import cn.minih.common.util.SnowFlake
import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostDeployingProcess
import io.vertx.core.Context
import io.vertx.core.Vertx

/**
 * 雪花算法实现
 * @author hubin
 * @since 2023-07-30 22:33:13
 * @desc
 */
@Suppress("unused")
@Component
class InitSnowFlake : PostDeployingProcess {
    override suspend fun exec(context: Context, deployId: String) {
        SnowFlakeContext.instance.putContext(deployId, SnowFlake(deployId))

    }
}

class SnowFlakeContext {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SnowFlakeContext() }
    }

    private val context = mutableMapOf<String, SnowFlake>()

    fun putContext(dId: String, sk: SnowFlake) {
        context[dId] = sk
    }

    @Suppress("unused")
    fun currentContext(): SnowFlake {
        return context[Vertx.currentContext().deploymentID()] ?: SnowFlake("1")
    }


}
