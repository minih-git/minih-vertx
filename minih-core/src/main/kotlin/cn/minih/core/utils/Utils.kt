@file:Suppress("unused")

package cn.minih.core.utils

import cn.minih.core.config.CoreConfig
import cn.minih.core.config.IConfig
import cn.minih.core.constants.PROJECT_NAME
import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import java.io.File
import java.io.IOException
import java.net.*
import java.util.*
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.internal.impl.types.SimpleType

/**
 *
 * @author hubin
 * @since 2023-07-30 20:57:17
 * @desc
 */
val log: KLogger = KotlinLogging.logger {}


fun findFirstNonLoopBackAddress(): InetAddress? {
    //todo  限制特定ip地址
    var result: InetAddress? = null
    var lowest = Int.MAX_VALUE
    val nics = NetworkInterface.getNetworkInterfaces()
    while (nics.hasMoreElements()
    ) {
        val ifc = nics.nextElement()
        if (ifc.isUp) {
            if (ifc.index < lowest || result == null) {
                lowest = ifc.index
            } else {
                continue
            }
            val addresses = ifc.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is Inet4Address && !address.isLoopbackAddress()) {
                    result = address
                }
            }
        }
    }
    return result
}


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

fun <T : IConfig> getConfig(
    configName: String,
    configClass: KClass<T>,
    vertx: Vertx = Vertx.currentContext().owner()
): T {
    val configRaw = vertx.orCreateContext.config()
    val configObj = configRaw.getJsonObject(PROJECT_NAME)
    val config = configObj.getJsonObject(configName) ?: jsonObjectOf()
    return fillObject(config, configClass)
}

fun <T : Any> fillObject(jsonObject: JsonObject, clazz: KClass<T>): T {
    var configPojo = "{}".jsonConvertData(clazz)
    try {
        val cons = clazz.primaryConstructor!!
        val values = cons.parameters.filterNot { it.isOptional }.associate { it to null }
        configPojo = cons.callBy(values)
    } catch (e: Exception) {
        e.printStackTrace()
        log.warn("创建${clazz.simpleName}对象失败，未设置的默认数据将被覆盖为null，请给所有字段赋予默认值,或提供无参数构造方法！")
    }
    val fields = clazz.memberProperties
    fields.forEach { it ->
        if (it is KMutableProperty1<*, *>) {
            val value = jsonObject.getValue(it.name)
            value.notNullAndExec { value1 ->
                val type = it.returnType.classifier as KClass<*>
                var v = value1
                if (type.simpleName === List::class.simpleName) {
                    val valueTmp = jsonObject.getJsonArray(it.name, JsonArray()).toList()
                    val first = valueTmp.first()
                    v = if (isBasicType(first::class.createType())) {
                        valueTmp
                    } else {
                        valueTmp.map { vt -> fillObject(vt.toJsonObject(), first::class) }
                    }
                }
                if (type.superclasses.contains(Enum::class)) {
                    val fn = type.functions.first { it.name == "valueOf" }
                    v = fn.call(v)!!
                }
                it.setter.call(configPojo, v)

            }

        } else {
            log.warn("${clazz.simpleName}.${it.name} 无法初始化！请将字段设置为可变类型！")
        }
    }
    return configPojo
}

enum class aas { aaa, bbb }
data class aaa(
    var c: aas = aas.aaa
)



fun isBasicType(cs: KType?): Boolean {
    return cs is SimpleType || isWrapper(cs) || cs == String::class.createType()
}

private fun isWrapper(cs: KType?): Boolean {
    return cs == Int::class.createType() ||
            cs == Short::class.createType() ||
            cs == Long::class.createType() ||
            cs == Byte::class.createType() ||
            cs == Float::class.createType() ||
            cs == Double::class.createType() ||
            cs == Boolean::class.createType() ||
            cs == Char::class.createType()
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
            } else if ("jar" == fileType) {
                var jar: JarFile? = null
                try {
                    jar = (dirOrFile.openConnection() as JarURLConnection)
                        .jarFile
                } catch (e: IOException) {
                    log.warn("load classes failed... path -> {}", path, e)
                }

                if (jar == null) continue
                val itemsForJar = jar.entries()
                while (itemsForJar.hasMoreElements()) {
                    val jarEntry = itemsForJar.nextElement()
                    var fileName = jarEntry.name
                    //目录
                    if (fileName.endsWith("/")) continue
                    if (fileName.first() == '/') {
                        fileName = fileName.substring(1)
                    }
                    //jar中文件或目录的路径，不与需要解析的路径匹配
                    if (!fileName.startsWith(slashPath)) continue
                    //class文件
                    if (fileName.endsWith(".class") && !jarEntry.isDirectory) {
                        //去掉 .class 结尾
                        val filePath = fileName.substring(0, fileName.length - 6)
                        val loadClass = ClassLoader.getSystemClassLoader().loadClass(filePath.replace('/', '.')).kotlin
                        result.add(loadClass)
                    }
                }
            }
        }
        return result
    }


}