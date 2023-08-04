package cn.minih.core.config

import java.util.*

/**
 * @author hubin
 * @date 2023/7/25
 * @desc
 */
interface IConfig


data class CoreConfig(
    var name: String = "p_${UUID.randomUUID().toString().replace("-", "")}",
    var env: String = "prod"
) : IConfig
