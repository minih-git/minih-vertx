package cn.minih.auth.config

import cn.minih.auth.constants.*
import cn.minih.core.config.IConfig
import cn.minih.core.constants.PROJECT_NAME

/**
 * 权限系统配置
 * @author hubin
 * @since 2023-07-30 22:37:51
 * @desc
 */
class AuthConfig(
    var cache: AuthCacheConfig,
    var encryptData: Boolean = false,
    var projectName: String = PROJECT_NAME,
    var tokenName: String = DEFAULT_TOKEN_NAME,
    var timeout: Long = DEFAULT_TIME_OUT,
    var isConcurrent: Boolean = false,
    var tokenGenMaxTryTimes: Int = 5,
    var loginMaxTryTimes: Int = 5,
    var loginMaxTryLockTimes: Int = 5 * 60,
    var loginMaxTryLockType: LockType = LockType.ACCOUNT,
    var tokenStyle: String = DEFAULT_TOKEN_STYLE,
    var tokenPrefix: String = DEFAULT_TOKEN_PREFIX,
    var loginPath: String = DEFAULT_LOGIN_PATH,
    var kickOutPath: String = DEFAULT_KICK_OUT_PATH,
    var logoutPath: String = DEFAULT_LOGOUT_PATH,
    var isReadHeader: Boolean = true,
    var isReadBody: Boolean = true,
    var isReadParams: Boolean = true,
    var autoKeepSign: Boolean = true,
    var ignoreAuthUri: List<String> = listOf(),
) : IConfig

data class AuthCacheConfig(
    var type: String = "redis",
    var poolSize: Int? = 8,
    var connectionString: String?
)

enum class LockType(val code: Int) {
    ACCOUNT(1), IP(2)
}