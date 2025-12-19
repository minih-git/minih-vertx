package cn.minih.auth.data

import cn.minih.auth.constants.DEFAULT_TOKEN_NAME
import cn.minih.auth.constants.DEFAULT_TOKEN_PREFIX
import java.util.*

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class AuthSession(
    val id: String = "",
    val loginId: String = "",
    val createTime: Long = Date().time,
    val tokenSignList: MutableList<TokenSign> = mutableListOf()
)

data class TokenSign(
    var token: String,
    var device: String,
    var timeout: Long,
)

data class TokenInfo(
    val tokenValue: String,
    val tokenName: String = DEFAULT_TOKEN_NAME,
    val loginId: String,
    val expired: Long,
    val loginDevice: String,
    val tokenPrefix: String = DEFAULT_TOKEN_PREFIX,
)