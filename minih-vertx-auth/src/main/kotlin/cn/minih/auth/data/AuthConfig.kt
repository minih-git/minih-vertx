package cn.minih.auth.data

import cn.minih.auth.constants.*

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class AuthConfig(
    val projectName: String = DEFAULT_PROJECT_NAME,
    val tokenName: String = DEFAULT_TOKEN_NAME,
    val timeout: Long = DEFAULT_TIME_OUT,
    val isConcurrent: Boolean = false,
    val maxTryTimes: Int = 5,
    val tokenStyle: String = DEFAULT_TOKEN_STYLE,
    val tokenPrefix: String = DEFAULT_TOKEN_PREFIX,
    val loginPath: String = DEFAULT_LOGIN_PATH,
    val kickOutPath: String = DEFAULT_KICK_OUT_PATH,
    val logoutPath: String = DEFAULT_LOGOUT_PATH,
    val isReadHeader: Boolean = true,
    val isReadBody: Boolean = true,
    val isReadParams: Boolean = true,
    val autoKeepSign: Boolean = true,
    val ignoreAuthUri: List<String> = mutableListOf("/authEventbus"),

    )
