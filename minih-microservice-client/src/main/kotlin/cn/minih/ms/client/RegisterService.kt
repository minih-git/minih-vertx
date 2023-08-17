package cn.minih.ms.client

import cn.minih.common.util.findFirstNonLoopBackAddress
import cn.minih.common.util.getEnv
import cn.minih.common.util.getProjectName
import cn.minih.common.util.log
import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostStartingProcess
import cn.minih.core.util.SnowFlakeContext
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
        val projectName = getProjectName()
        val ip = findFirstNonLoopBackAddress()
        val shareData = vertx.sharedData().getAsyncMap<String, Any>("share-$projectName").await()
        val port = shareData.get("port").await()
        val record =
            HttpEndpoint.createRecord(projectName, ip?.hostAddress ?: "", port as Int, ctx.config.rootPath)
        val severId = SnowFlakeContext.instance.currentContext().nextId().toString()
        shareData.put("serverId-${ip?.hostAddress}", severId).await()
        record.registration = severId
        record.setMetadata(jsonObjectOf("env" to getEnv()))
        ctx.discovery.publish(record) {
            log.info("${getProjectName()}  服务注册成功!")
        }
    }
}
