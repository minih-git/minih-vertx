package cn.minih.auth.constants

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
const val TOKEN_STYLE_UUID = "uuid"
const val TOKEN_STYLE_SIMPLE_UUID = "simple-uuid"
const val TOKEN_STYLE_RANDOM_32 = "random-32"
const val TOKEN_STYLE_RANDOM_64 = "random-64"
const val TOKEN_STYLE_RANDOM_128 = "random-128"
const val TOKEN_STYLE_TIK = "random-tik"
const val TOKEN_STYLE_SNOWFLAKE = "snowflake"
const val DEFAULT_DEVICE = "default-device"
const val DEFAULT_LOGIN_TYPE = "default-type"
const val DEFAULT_TOKEN_NAME = "Authorization"
const val DEFAULT_TIME_OUT = 60 * 60 * 2L
const val DEFAULT_TOKEN_STYLE = TOKEN_STYLE_UUID
const val DEFAULT_TOKEN_PREFIX = "Bearer"
const val DEFAULT_LOGIN_PATH = "/auth/login"
const val DEFAULT_KICK_OUT_PATH = "/auth/kickOut"
const val DEFAULT_LOGOUT_PATH = "/auth/logout"
const val TOKEN_CONNECTOR_CHAT = " "
const val NEVER_EXPIRE = -1L
const val CONTEXT_LOGIN_ID = "loginId"
const val CONTEXT_LOGIN_TOKEN = "loginToken"
const val CONTEXT_LOGIN_DEVICE = "loginDevice"
const val CONTEXT_IS_SYSTEM_ADMIN = "isSystemAdmin"
const val CONTEXT_SYSTEM_ADMIN_ROLE_TAG = "role_system_admin"
const val AUTH_SESSION_OFFLINE = "cn.minih.auth.session.offline"
const val AUTH_SESSION_KEEP = "cn.minih.auth.session.keep"
const val AES_SECRET_REDIS_KEY_PREFIX = "minih:core:aes"
const val TOKEN_VALUE_CACHE_KEY = "minih:auth:token-session"
const val LOGIN_SESSION_CACHE_KEY = "minih:auth:login-session"
const val SESSION_ACTIVE_CACHE_KEY = "minih:auth:session-active"
const val LOGIN_ERR_COUNT_CACHE_KEY = "minih:auth:login-error-count"
const val LOGIN_USER_ROLES_CACHE_KEY = "minih:auth:login-role"
