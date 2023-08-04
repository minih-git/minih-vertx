package cn.minih.ms.client

import cn.minih.core.exception.MinihException
import cn.minih.core.utils.Assert
import cn.minih.core.utils.getConfig
import cn.minih.ms.client.constants.MICROSERVICE_ADDRESS
import io.vertx.core.Vertx
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.ServiceDiscoveryOptions

/**
 *  微服务客户端上下文
 * @author hubin
 * @since 2023-08-01 13:42:42
 */
class MsClientContext {

    lateinit var discovery: ServiceDiscovery
    lateinit var config: Config


    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            MsClientContext()
        }
    }

    fun initContext(vertx: Vertx): MsClientContext {
        config = getConfig("ms", Config::class, vertx)
        Assert.notBlank(config.rootPath) {
            throw MinihException("请设置根路径！")
        }
        Assert.notBlank(config.consulHost) {
            throw MinihException("请设置consul地址！")
        }
        discovery = ServiceDiscovery.create(
            vertx, ServiceDiscoveryOptions().setBackendConfiguration(
                jsonObjectOf("host" to config.consulHost, "port" to config.consulPort, "dc" to config.dcName)
            ).setAnnounceAddress(MICROSERVICE_ADDRESS).setName("minih-ms")
        )
        return instance
    }

}
