package cn.minih.rocketmq.config

import cn.minih.core.config.IConfig

/**
 *  rocketmq配置
 * @author hubin
 * @since 2023-08-09 17:36:40
 */
data class RocketConfig(
    var endpoints: String = "",
    var accessKey: String = "",
    var secretKey: String = "",
    var topics: List<String> = mutableListOf(),
) : IConfig