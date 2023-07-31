package cn.minih.ms.client

import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostStartingProcess
import cn.minih.core.exception.MinihException
import cn.minih.core.utils.Assert
import cn.minih.core.utils.getConfig
import cn.minih.core.utils.log
import cn.minih.ms.client.constants.MICROSERVICE_ADDRESS
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.ServiceDiscoveryOptions
import io.vertx.servicediscovery.types.HttpEndpoint


/**
 *  注册服务
 * @author hubin
 * @since 2023-07-31 11:25:55
 */
@Component
class RegisterService : PostStartingProcess {
    override suspend fun exec(vertx: Vertx) {
        val config = getConfig("ms", Config::class, vertx)
        Assert.notBlank(config.serverName) {
            throw MinihException("请设置服务名称！")
        }
        Assert.notBlank(config.rootPath) {
            throw MinihException("请设置根路径！")
        }
        val discovery = ServiceDiscovery.create(
            vertx,
            ServiceDiscoveryOptions()
                .setBackendConfiguration(
                    JsonObject()
                        .put("connectionString", "redis://:Minih123@db.minih.cn:6379/0")
                        .put("key", "cn.minih.discovery")
                )
                .setAnnounceAddress(MICROSERVICE_ADDRESS)
                .setName("minih-ms")
        )
        val shareData = vertx.sharedData().getAsyncMap<String, Int>("share")
        val port = shareData.await().get("port").await()
        val record = HttpEndpoint.createRecord(config.serverName, "0.0.0.0", port, config.rootPath)
        discovery.publish(record) {
            log.info("${config.serverName}  publish success!")
        }
    }

}
