package cn.minih.ms.client

import cn.minih.core.annotation.Component
import cn.minih.core.boot.PreStopProcess
import cn.minih.core.utils.log
import io.vertx.core.Vertx
import io.vertx.ext.consul.ConsulClient
import io.vertx.ext.consul.ConsulClientOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await

/**
 *  服务状态监听
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
        val shareData = vertx.sharedData().getAsyncMap<String, Any>("share").await()
        val serverId = shareData.get("serverId").await().toString()
        client.deregisterService(serverId).await()
        log.info("$serverId deregister done.")
    }
}
