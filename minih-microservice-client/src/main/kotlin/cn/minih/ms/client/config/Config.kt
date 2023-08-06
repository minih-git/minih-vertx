package cn.minih.ms.client.config

import cn.minih.core.config.DEFAULT_TIMEOUT
import cn.minih.core.config.IConfig

/**
 *  微服务相关配置
 * @author hubin
 * @since 2023-07-31 11:28:31
 */
class Config(
    var rootPath: String = "",
    var consulHost: String = "",
    var consulPort: Int = 8500,
    var dcName: String = "dc1",
    var timeout: Long = DEFAULT_TIMEOUT
) : IConfig