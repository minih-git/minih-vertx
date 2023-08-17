package cn.minih.ms.client

import cn.minih.common.util.findFirstNonLoopBackAddress
import cn.minih.common.util.getProjectName
import cn.minih.common.util.log
import cn.minih.core.annotation.Component
import cn.minih.core.boot.PreStopProcess
import io.vertx.core.Vertx
import io.vertx.ext.consul.ConsulClient
import io.vertx.ext.consul.ConsulClientOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await

/**
 * 取消服务注册
 * @author hubin
 * @since 2023-08-01 08:57:24
 */
@Component
class DeregisterService : PreStopProcess {
    override suspend fun exec(vertx: Vertx) {
        val config = MsClientContext.instance.config
        val opt = ConsulClientOptions(
            jsonObjectOf(
                "host" to config.consulHost,
                "port" to config.consulPort,
                "dc" to config.dcName
            )
        )
        val client = ConsulClient.create(vertx, opt)
        val projectName = getProjectName()
        val ip = findFirstNonLoopBackAddress()
        log.info("projectName:$projectName,ip:${ip?.hostAddress}")
        val shareData = vertx.sharedData().getAsyncMap<String, Any>("share-$projectName").await()
        val serverId = shareData.get("serverId-${ip?.hostAddress}").await().toString()
        client.deregisterService(serverId).await()
        log.info("$serverId deregister done.")
    }
}
