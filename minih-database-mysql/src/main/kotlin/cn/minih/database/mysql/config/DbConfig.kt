package cn.minih.database.mysql.config

import cn.minih.core.config.IConfig

/**
 * mysql链接配置
 * @author hubin
 * @since 2023-07-30 23:05:49
 * @desc
 */
data class DbConfig(
    val host: String,
    val db: String,
    val user: String,
    val password: String,
    val pollSize: Int?,
) : IConfig