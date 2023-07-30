@file:Suppress("unused")

package cn.minih.core.utils

import cn.minih.core.config.CoreConfig
import cn.minih.core.config.IConfig
import cn.minih.core.constants.PROJECT_NAME
import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance

/**
 *
 * @author hubin
 * @since 2023-07-30 20:57:17
 * @desc
 */
val log: KLogger = KotlinLogging.logger {}

fun <T : Any> JsonObject.covertTo(clazz: KClass<T>): T {
    return Gson().fromJson(this.toString(), clazz.java)
}

fun JsonObject.covertTo(kType: KType): Any {
    return this.covertTo(kType.classifier as KClass<*>)
}

fun Any.toJsonObject(): JsonObject {
    return JsonObject(Gson().toJson(this))
}

fun Any.toJsonString(): String {
    return Gson().toJson(this)
}

fun <T : Any> String.jsonConvertData(clazz: KClass<T>): T {
    return Gson().fromJson(this, clazz.java)
}

fun getConfig(): CoreConfig {
    return getConfig(configName = "core", configClass = CoreConfig::class)
}

fun <T : IConfig> getConfig(configName: String, configClass: KClass<T>): T {
    val sharedData = Vertx.currentContext().config()
    val config = sharedData.getJsonObject(PROJECT_NAME).getJsonObject(configName)
        ?: configClass.createInstance().toJsonObject()
    return config.covertTo(configClass)
}

suspend fun <T> T?.notNullAndExecSuspend(fn: suspend (T) -> Unit) {
    if (this != null) {
        if (this is String && this.isBlank()) {
            return
        }
        if (this is Collection<*> && this.isNotEmpty()) {
            return
        }
        if (this is Map<*, *> && this.isNotEmpty()) {
            return
        }
        fn(this)
    }
}

fun <T> T?.notNullAndExec(fn: (T) -> Unit) {
    if (this != null) {
        if (this is String && this.isBlank()) {
            return
        }
        if (this is Collection<*> && this.isNotEmpty()) {
            return
        }
        if (this is Map<*, *> && this.isNotEmpty()) {
            return
        }
        fn(this)
    }
}


object Utils {

    fun getClassesByPath(path: String): MutableSet<KClass<*>> {

        val slashPath = path.replace(".", "/")

        val result = mutableSetOf<KClass<*>>()
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


}