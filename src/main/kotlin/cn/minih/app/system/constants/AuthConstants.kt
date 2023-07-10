package cn.minih.app.system.constants

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
const val DEVICE_KEY = "device"
const val DEFAULT_DEVICE = "default-device"
const val DEFAULT_TOKEN_NAME = "Authorization"
const val DEFAULT_TIME_OUT = 60 * 60 * 24 * 30L
const val DEFAULT_TOKEN_STYLE = TOKEN_STYLE_UUID
const val DEFAULT_TOKEN_PREFIX = "Bearer"
const val DEFAULT_LOGIN_PATH = "/auth/login"
const val TOKEN_CONNECTOR_CHAT = " "
const val NEVER_EXPIRE = -1L
const val CONTEXT_LOGIN_ID = "loginId"