package cn.minih.ms.client

import cn.minih.core.config.IConfig

/**
 *  微服务相关配置
 * @author hubin
 * @since 2023-07-31 11:28:31
 */
class Config(
    var serverName: String = "",
    var rootPath: String = "",
    var consulHost: String = "",
    var consulPort: Int = 8500,
    var dcName: String = "dc1"
) : IConfig
