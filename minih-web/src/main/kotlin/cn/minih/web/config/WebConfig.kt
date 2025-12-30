package cn.minih.web.config

import cn.minih.core.config.IConfig

/**
 * web相关配置
 * @author hubin
 * @since 2023-07-30 22:16:45
 * @desc
 */
data class WebConfig(
    var tmpFilePath: String = "file-uploads"
) : IConfig