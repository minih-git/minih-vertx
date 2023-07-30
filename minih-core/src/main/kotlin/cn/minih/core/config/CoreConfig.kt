package cn.minih.core.config

/**
 * @author hubin
 * @date 2023/7/25
 * @desc
 */
interface IConfig


data class CoreConfig(
    var serviceName: String
) : IConfig