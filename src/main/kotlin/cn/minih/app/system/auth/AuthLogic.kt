package cn.minih.app.system.auth

import cn.minih.app.system.auth.AuthUtil.getRandomString
import cn.minih.app.system.utils.SnowFlake
import io.vertx.kotlin.coroutines.await
import java.util.*


/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
class AuthLogic {


    suspend fun createTokenValue(): String {
        val config = AuthUtil.config.await()
        val token  =  when (config["tokenStyle"].result()) {
            TOKEN_STYLE_UUID -> UUID.randomUUID().toString()
            TOKEN_STYLE_SIMPLE_UUID -> UUID.randomUUID().toString().replace("-".toRegex(), "")
            TOKEN_STYLE_RANDOM_32 -> getRandomString(32)
            TOKEN_STYLE_RANDOM_64 -> getRandomString(64)
            TOKEN_STYLE_RANDOM_128 -> getRandomString(128)
            TOKEN_STYLE_TIK -> "${getRandomString(2)}_${getRandomString(14)}_${getRandomString(16)}__"
            TOKEN_STYLE_SNOWFLAKE -> SnowFlake.nextId().toString()
            else -> UUID.randomUUID().toString()
        }
        println("token:${token}")
        return token
    }
}