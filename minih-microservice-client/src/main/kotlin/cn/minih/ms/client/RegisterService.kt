package cn.minih.ms.client

import cn.minih.common.util.*
import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostStartingProcess
import cn.minih.ms.client.config.Config
import cn.minih.ms.client.config.MsEnv
import io.vertx.core.Context
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.servicediscovery.types.HttpEndpoint


/**
 *  注册服务
 * @author hubin
 * @since 2023-07-31 11:25:55
 */
@Component
class RegisterService : PostStartingProcess {
    override suspend fun exec(context: Context) {
        val config = getConfig("ms", Config::class, context)
        if (config.msEnv == MsEnv.CONSUL) {
            val ctx = MsClientContext.instance.initContext(context)
            val projectName = getProjectName(context)
            val ip = findFirstNonLoopBackAddress()
            val shareData = context.owner().sharedData().getAsyncMap<String, Any>("share-$projectName").coAwait()
            val port = shareData.get("port").coAwait()
            val record =
                HttpEndpoint.createRecord(projectName, ip?.hostAddress ?: "", (port as? Int) ?: 8080, ctx.config.rootPath)
            val severId = SnowFlake.instance.nextId(0).toString()
            shareData.put("serverId-${ip?.hostAddress}", severId).coAwait()
            record.registration = severId
            record.setMetadata(jsonObjectOf("env" to getEnv()))
            ctx.discovery.publish(record) {
                log.info("${getProjectName(context)}  服务注册成功!")
            }
        }
    }
}