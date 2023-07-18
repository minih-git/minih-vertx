package cn.minih.core.utils

import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.json.JsonObject
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URL
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType


val log: KLogger = KotlinLogging.logger {}


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

    val slashPath = path.replace(".", "/");
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
                log.warn("path: {}, file not exist", filePath);
                continue;
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
