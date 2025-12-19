package cn.minih.database.config

import cn.minih.core.config.IConfig

/**
 * mysql链接配置
 * @author hubin
 * @since 2023-07-30 23:05:49
 * @desc
 */
data class DbConfig(
    var type: String = "mysql",
    var host: String = "",
    var port: Int = 3306,
    var db: String = "",
    var user: String = "",
    var password: String = "",
    var pollSize: Int = 20,
) : IConfig