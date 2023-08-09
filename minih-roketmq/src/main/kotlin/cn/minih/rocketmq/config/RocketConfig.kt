package cn.minih.rocketmq.config

import cn.minih.core.config.IConfig

/**
 *  rocketmq配置
 * @author hubin
 * @since 2023-08-09 17:36:40
 */
data class RocketConfig(
    var nameServer: String = "",
    var producer: RocketmqProducerConfig = RocketmqProducerConfig(),
) : IConfig

data class RocketmqProducerConfig(
    var group: String = "",
    var sendMessageTimeout: Long = 3000,
    var retryTimesWhenSendFailed: Int = 3,
    var retryTimesWhenSendAsyncFailed: Int = 3,
)
