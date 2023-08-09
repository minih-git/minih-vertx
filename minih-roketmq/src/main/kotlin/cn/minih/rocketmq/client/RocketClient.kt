package cn.minih.rocketmq.client

import cn.minih.common.exception.MinihException
import cn.minih.common.util.Assert
import cn.minih.common.util.getConfig
import cn.minih.rocketmq.config.RocketConfig
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder
import org.apache.rocketmq.client.apis.producer.Producer
import org.apache.rocketmq.client.java.impl.producer.ProducerBuilderImpl

/**
 * rocketmq客户端
 * @author hubin
 * @since 2023-08-09 21:49:48
 * @desc
 */
object RocketClient {

    val producer: Producer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val config = getConfig("rocketmq", RocketConfig::class)
        Assert.notBlank(config.endpoints) { MinihException("请设置rocketmq的地址") }
        val configBuilder = ClientConfigurationBuilder().setEndpoints(config.endpoints)
        ProducerBuilderImpl().setClientConfiguration(configBuilder.build())
            .setTopics(*config.topics.toTypedArray()).build()
    }


}