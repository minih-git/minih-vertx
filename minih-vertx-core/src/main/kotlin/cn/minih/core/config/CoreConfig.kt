package cn.minih.core.config

/**
 * @author hubin
 * @date 2023/7/25
 * @desc
 */
data class DbConfig(
    val host: String,
    val db: String,
    val user: String,
    val password: String,
    val pollSize: Int? = 8,
)

data class CoreConfig(
    val aliyunApiKey: String,
    val aliyunApiSecret: String,
    val aesSecret: String,
    val mysql: DbConfig
)
