@file:Suppress("unused")

package cn.minih.auth.utils

import cn.minih.auth.annotation.CheckRoleType
import cn.minih.auth.cache.MinihAuthRedisManager
import cn.minih.auth.constants.AES_SECRET_REDIS_KEY_PREFIX
import cn.minih.auth.constants.CONTEXT_LOGIN_ID
import cn.minih.auth.logic.AuthServiceHandler
import cn.minih.core.exception.MinihDataDecryptionException
import cn.minih.core.utils.log
import com.google.gson.Gson
import com.google.gson.LongSerializationPolicy
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */

suspend fun generateAesSecret(): String {
    val redisAPi = MinihAuthRedisManager.instance.getReidApi()
    val aesSecret = redisAPi.get(AES_SECRET_REDIS_KEY_PREFIX)?.await()?.toString()
    if (aesSecret == null) {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(128, SecureRandom())
        val secretKey = keyGenerator.generateKey().encoded.joinToString("") { "%02x".format(it) }
        redisAPi.set(listOf(AES_SECRET_REDIS_KEY_PREFIX, secretKey))
        return secretKey
    }
    return aesSecret
}

fun Any.jsToJsonString(): JsonObject {
    return JsonObject(
        Gson().newBuilder().setLongSerializationPolicy(LongSerializationPolicy.STRING).create().toJson(this)
    )
}

fun encrypt(strToEncrypt: String, secret: String): String {
    try {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(secret.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)))
    } catch (e: Exception) {
        println("Error while encrypting: $e")
    }
    return ""
}

fun decrypt(strToDecrypt: String, secret: String): String {
    try {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
        val secretKey = SecretKeySpec(secret.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)))
    } catch (e: Exception) {
        log.warn("解密密钥错误,secret:$secret")
        throw MinihDataDecryptionException("秘钥错误，请刷新页面重试")
    }
}


fun getRequestBody(ctx: RoutingContext): JsonObject {
    val body = JsonObject()
    ctx.body()?.asJsonObject()?.map {
        body.put(it.key, it.value)
    }
    ctx.request()?.params()?.map {
        body.put(it.key, it.value)
    }
    return body
}


@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        GlobalScope.launch(v) {
            try {
                fn(ctx)
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineVoidHandler(fn: suspend (Any?) -> Unit) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        GlobalScope.launch(v) {
            try {
                fn(null)
                ctx.end()
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun Route.neeRole(vararg role: String, type: CheckRoleType = CheckRoleType.AND): Route {
    putMetadata("needRoles", role)
        .handler { ctx ->
            GlobalScope.launch {
                try {
                    AuthServiceHandler.instance.checkRole(
                        ctx.get(CONTEXT_LOGIN_ID),
                        role.toList(),
                        type == CheckRoleType.AND
                    )
                    ctx.next()
                } catch (e: Exception) {
                    ctx.fail(e)
                }
            }
        }
    return this
}


fun getRemoteIp(request: HttpServerRequest): String {
    var ip: String? = request.getHeader("x-forwarded-for")
    if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
        ip = request.getHeader("Proxy-Client-IP")
    }
    if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
        ip = request.getHeader("WL-Proxy-Client-IP")
    }
    if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
        ip = request.getHeader("HTTP_CLIENT_IP")
    }
    if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
        ip = request.getHeader("HTTP_X_FORWARDED_FOR")
    }
    if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
        ip = request.remoteAddress().host()
    }
    return ip ?: ""
}