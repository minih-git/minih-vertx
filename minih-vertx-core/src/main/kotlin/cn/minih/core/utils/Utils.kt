package cn.minih.core.utils

import cn.minih.core.config.CoreConfig
import cn.minih.core.constants.PROJECT_NAME
import cn.minih.core.constants.SMS_REDIS_EXPIRE
import cn.minih.core.constants.SMS_REDIS_KEY_PREFIX
import cn.minih.core.exception.MinihException
import cn.minih.core.repository.RedisManager
import com.aliyun.auth.credentials.Credential
import com.aliyun.auth.credentials.provider.StaticCredentialProvider
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest
import com.google.gson.Gson
import darabonba.core.client.ClientOverrideConfiguration
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URL
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow
import kotlin.reflect.KClass
import kotlin.reflect.KType


val log: KLogger = KotlinLogging.logger {}

fun getConfig(): CoreConfig {
    val sharedData = Vertx.currentContext().config()
    return sharedData.getJsonObject(PROJECT_NAME).getJsonObject("core").covertTo(CoreConfig::class)
}

suspend fun <T> T?.notBlankAndExec(fn: suspend (T) -> Unit) {
    if (this != null) {
        if (this is String && this.isBlank()) {
            return
        }
        fn(this)
    }
}

fun Any.toJsonObject(): JsonObject {
    return JsonObject(Gson().toJson(this))
}

fun Any.toJsonString(): String {
    return Gson().toJson(this)
}

fun <T : Any> JsonObject.covertTo(clazz: KClass<T>): T {
    return Gson().fromJson(this.toString(), clazz.java)
}

fun JsonObject.covertTo(kType: KType): Any {
    return this.covertTo(kType.classifier as KClass<*>)
}

fun <T : Any> String.jsonConvertData(clazz: KClass<T>): T {
    return Gson().fromJson(this, clazz.java)
}

fun getClassesByPath(path: String): MutableList<KClass<*>> {

    val slashPath = path.replace(".", "/")
    val result = mutableListOf<KClass<*>>()
    var dirOrFiles: Enumeration<URL>? = null
    try {
        dirOrFiles = Thread.currentThread().contextClassLoader.getResources(slashPath)
    } catch (e: IOException) {
        log.error("load class failed, path = {}", path, e)
    }
    if (dirOrFiles == null) return result
    while (dirOrFiles.hasMoreElements()) {
        val dirOrFile = dirOrFiles.nextElement()
        val fileType = dirOrFile.protocol
        if ("file" == fileType) {
            val filePath = dirOrFile.file
            val file = File(filePath)
            if (!file.exists()) {
                log.warn("path: {}, file not exist", filePath)
                continue
            }
            if (file.isDirectory) {
                val files = file.listFiles { f -> f.isDirectory || f.name.endsWith(".class") } ?: continue
                for (f in files) {
                    var fileName = f.name
                    if (f.isDirectory) {
                        result.addAll(getClassesByPath("$path.$fileName"))
                    } else if (f.name.endsWith(".class")) {
                        //去掉 .class 结尾
                        fileName = fileName.substring(0, fileName.length - 6)
                        result.add(ClassLoader.getSystemClassLoader().loadClass("$path.$fileName").kotlin)
                    }
                }
                continue
            }
        }
    }
    return result
}

suspend fun smsSendVerifyCode(phone: String) {
    val redisApi = RedisManager.instance.getReidApi()
    val config = getConfig()
    val provider = StaticCredentialProvider.create(
        Credential.builder()
            .accessKeyId(config.aliyunApiKey)
            .accessKeySecret(config.aliyunApiSecret)
            .build()
    )
    val client = AsyncClient.builder().region("cn-hangzhou")
        .credentialsProvider(provider).overrideConfiguration(
            ClientOverrideConfiguration.create()
                .setEndpointOverride("dysmsapi.aliyuncs.com")
        )
        .build()
    var code = redisApi.get(SMS_REDIS_KEY_PREFIX + phone)?.await()?.toString()
    if (code.isNullOrBlank()) {
        code = ((Math.random() * 9 + 1) * 10.0.pow(5.0)).toInt().toString()
        redisApi.set(listOf(SMS_REDIS_KEY_PREFIX + phone, code, "EX", SMS_REDIS_EXPIRE.toString()))
    }
    val sendSmsRequest = SendSmsRequest.builder()
        .signName("阿里云短信测试")
        .templateCode("SMS_154950909")
        .phoneNumbers("15999603031")
        .templateParam(jsonObjectOf("code" to code).toString())
        .build()
    client.sendSms(sendSmsRequest)
    client.close()
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

fun generateAesSecret(): String {
    val keyGenerator = KeyGenerator.getInstance("AES")
    keyGenerator.init(128, SecureRandom())
    val secretKey = keyGenerator.generateKey()
    return secretKey.encoded.joinToString("") { "%02x".format(it) }
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
        throw MinihException(e.message)
    }
}

fun main() {
    print(decrypt("j2iTIIShca5N3kDFJOT+Vw==", "2185c344b99547f82d9948c7b68ee11c"))
}


object SnowFlake {
    private val workerId = getWorkId(InetAddress.getLocalHost().hostAddress)
    private val datacenterId = getCenterId(InetAddress.getLocalHost().hostAddress)
    private var sequence = 0
    private const val sp = 1585644268888L
    private const val workerIdBits = 5L
    private const val datacenterIdBits = 5L
    private const val sequenceBits = 12L
    private const val workerIdShift = sequenceBits
    private const val datacenterIdShift = sequenceBits + workerIdBits
    private const val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits
    private const val sequenceMask = -1L xor (-1L shl sequenceBits.toInt())
    private var lastTimestamp = -1L


    @Synchronized
    fun nextId(): Long {
        var timestamp: Long = System.currentTimeMillis()
        if (timestamp < lastTimestamp) {
            log.error { "系统时间不正确" }
            throw RuntimeException("系统时间不正确")
        }
        if (lastTimestamp == timestamp) {
            sequence = ((sequence + 1).toLong() and sequenceMask).toInt()
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp)
            }

        } else {
            sequence = 0
        }
        lastTimestamp = timestamp
        return timestamp - sp shl timestampLeftShift.toInt() or
                (datacenterId shl datacenterIdShift.toInt()) or
                (workerId shl workerIdShift.toInt()) or sequence.toLong()
    }

    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp: Long = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }

    private fun getCenterId(ip: String): Long {
        val s: List<String> = ip.split(".")
        return ((s[0].toLong() shl 24)
                + (s[1].toLong() shl 16) +
                (0 shl 8) + 0)


    }

    private fun getWorkId(ip: String): Long {
        val s: List<String> = ip.split(".")
        return ((s[0].toLong() shl 24)
                + (s[1].toLong() shl 16) +
                (s[2].toLong() shl 8)
                + s[3].toLong())
    }
}
