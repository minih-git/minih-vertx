@file:Suppress("unused")

package cn.minih.auth.utils

import cn.minih.auth.constants.AES_SECRET_REDIS_KEY_PREFIX
import cn.minih.auth.constants.CONTEXT_LOGIN_ID
import cn.minih.auth.logic.AuthServiceHandler
import cn.minih.cache.redis.impl.RedisCacheManagerImpl
import cn.minih.common.exception.MinihDataDecryptionException
import cn.minih.common.util.log
import cn.minih.core.annotation.CheckRoleType
import com.google.gson.Gson
import com.google.gson.LongSerializationPolicy
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val cache = RedisCacheManagerImpl().getCache(AES_SECRET_REDIS_KEY_PREFIX)
    var aesSecret = cache.get("secret", String::class).coAwait()
    if (aesSecret == null) {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(128, SecureRandom())
        aesSecret = keyGenerator.generateKey().encoded.joinToString("") { "%02x".format(it) }
        cache.put("secret", aesSecret)
    }
    return aesSecret
}

fun Any.jsToJsonString(): JsonObject {
    return JsonObject(
        Gson().newBuilder().serializeNulls().setLongSerializationPolicy(LongSerializationPolicy.STRING).create()
            .toJson(this)
    )
}


fun encrypt(strToEncrypt: String, secret: String): String {
    try {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(secret.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)))
    } catch (e: Throwable) {
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
    } catch (e: Throwable) {
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


fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        CoroutineScope(v).launch {
            try {
                fn(ctx)
            } catch (e: Throwable) {
                ctx.fail(e)
            }
        }
    }
}

fun Route.coroutineVoidHandler(fn: suspend (Any?) -> Unit) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        CoroutineScope(v).launch {
            try {
                fn(null)
                ctx.end()
            } catch (e: Throwable) {
                ctx.fail(e)
            }
        }
    }
}

fun Route.neeRole(vararg role: String, type: CheckRoleType = CheckRoleType.AND): Route {
    putMetadata("needRoles", role)
        .handler { ctx ->
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    AuthServiceHandler.instance.checkRole(
                        ctx.get(CONTEXT_LOGIN_ID),
                        role.toList(),
                        type == CheckRoleType.AND
                    )
                    ctx.next()
                } catch (e: Throwable) {
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