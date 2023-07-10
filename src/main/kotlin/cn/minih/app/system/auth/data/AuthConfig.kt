package cn.minih.app.system.auth.data

import cn.minih.app.system.constants.*

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class AuthConfig(
    val tokenName: String = DEFAULT_TOKEN_NAME,
    val timeout: Long = DEFAULT_TIME_OUT,
    val isConcurrent: Boolean = true,
    val maxTryTimes: Int = 5,
    val tokenStyle: String = DEFAULT_TOKEN_STYLE,
    val tokenPrefix: String = DEFAULT_TOKEN_PREFIX,
    val loginPath: String = DEFAULT_LOGIN_PATH,
    val isReadHeader: Boolean = true,
    val isReadBody: Boolean = true,
    val isReadParams: Boolean = true,
    val ignoreAuthUri: List<String> = mutableListOf(),

    )
