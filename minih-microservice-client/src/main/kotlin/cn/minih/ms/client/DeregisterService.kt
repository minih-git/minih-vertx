package cn.minih.ms.client

import cn.minih.common.util.findFirstNonLoopBackAddress
import cn.minih.common.util.getConfig
import cn.minih.common.util.getProjectName
import cn.minih.common.util.log
import cn.minih.core.annotation.Component
import cn.minih.core.boot.PreStopProcess
import cn.minih.ms.client.config.Config
import cn.minih.ms.client.config.MsEnv
import io.vertx.core.Context
import io.vertx.ext.consul.ConsulClient
import io.vertx.ext.consul.ConsulClientOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.coAwait

/**
 * 取消服务注册
 * @author hubin
 * @since 2023-08-01 08:57:24
 */
@Component
class DeregisterService : PreStopProcess {
    override suspend fun exec(context: Context) {
        val config = getConfig("ms", Config::class, context)
        if (config.msEnv == MsEnv.CONSUL) {
            val opt = ConsulClientOptions(
                jsonObjectOf(
                    "host" to config.consulHost,
                    "port" to config.consulPort,
                    "dc" to config.dcName
                )
            )
            val client = ConsulClient.create(context.owner(), opt)
            val projectName = getProjectName(context)
            val ip = findFirstNonLoopBackAddress()
            log.info("projectName:$projectName,ip:${ip?.hostAddress}")
            val shareData = context.owner().sharedData().getAsyncMap<String, Any>("share-$projectName").coAwait()
            val serverId = shareData.get("serverId-${ip?.hostAddress}").coAwait()?.toString()
            if (serverId != null) {
                client.deregisterService(serverId).coAwait()
                log.info("$serverId deregister done.")
            }
        }
    }
}