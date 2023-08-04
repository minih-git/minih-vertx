package cn.minih.ms.client

import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostStartingProcess
import cn.minih.core.utils.*
import io.vertx.core.Vertx
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import io.vertx.servicediscovery.types.HttpEndpoint


/**
 *  注册服务
 * @author hubin
 * @since 2023-07-31 11:25:55
 */
@Component
class RegisterService : PostStartingProcess {
    override suspend fun exec(vertx: Vertx) {
        val ctx = MsClientContext.instance.initContext(vertx)
        val shareData = vertx.sharedData().getAsyncMap<String, Any>("share").await()
        val port = shareData.get("port").await()
        val ip = findFirstNonLoopBackAddress()
        val record =
            HttpEndpoint.createRecord(getProjectName(), ip?.hostAddress ?: "", port as Int, ctx.config.rootPath)
        val severId = SnowFlakeContext.instance.currentContext().nextId().toString()
        shareData.put("serverId", severId).await()
        record.registration = severId
        record.setMetadata(jsonObjectOf("env" to getEnv()))
        ctx.discovery.publish(record) {
            log.info("${getProjectName()}  服务注册成功!")
        }
    }
}
