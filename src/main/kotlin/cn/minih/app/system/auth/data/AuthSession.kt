package cn.minih.app.system.auth.data

import cn.minih.app.system.constants.DEFAULT_TOKEN_NAME
import cn.minih.app.system.constants.DEFAULT_TOKEN_PREFIX
import io.vertx.core.json.JsonObject
import java.util.*

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class AuthSession(
    val id: String = "",
    val loginId: Any = "",
    val createTime: Long = Date().time,
    val tokenSignList: MutableList<TokenSign> = mutableListOf()
)

data class TokenSign(
    var token: String,
    var device: String
)

data class TokenInfo(
    val tokenValue: String,
    val tokenName: String = DEFAULT_TOKEN_NAME,
    val loginId: String,
    val expired: Long,
    val loginDevice: String,
    val tokenPrefix: String = DEFAULT_TOKEN_PREFIX,
)
