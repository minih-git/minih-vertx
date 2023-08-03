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
import kotlin.reflect.KProperty
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
    if (this is JsonObject) {
        return this
    }
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
    val configObj = configRaw.getJsonObject(PROJECT_NAME) ?: jsonObjectOf()
    val config = configObj.getJsonObject(configName) ?: jsonObjectOf()
    return fillObject(config, configClass)
}

fun <T : Any> fillObject(jsonObject: JsonObject, clazz: KClass<T>): T {
    var pojo = "{}".jsonConvertData(clazz)
    try {
        val cons = clazz.primaryConstructor!!
        val values = cons.parameters.filterNot { it.isOptional }.associateWith { null }
        pojo = cons.callBy(values)
    } catch (e: Exception) {
        e.printStackTrace()
        log.warn("创建${clazz.simpleName}对象失败，未设置的默认数据将被覆盖为null，请给所有字段赋予默认值,或提供无参数构造方法！")
    }
    val fields = clazz.memberProperties
    fields.forEach {
        if (it is KMutableProperty1<*, *>) {
            val value = jsonObject.getValue(it.name)
            value.notNullAndExec { value1 -> setField(it, pojo, value1) }
        } else {
            log.warn("${clazz.simpleName}.${it.name} 无法初始化！请将字段设置为可变类型！")
        }
    }
    return pojo
}

private fun setField(filed: KMutableProperty1<*, *>, pojo: Any, value: Any) {
    val type = filed.returnType.classifier as KClass<*>
    var valueTmp = value
    if (value is JsonArray) valueTmp = value.toList()
    val v = when {
        type.simpleName === List::class.simpleName -> fillObjectHandleList(valueTmp, filed)
        type.superclasses.contains(Enum::class) -> type.functions.first { it.name == "valueOf" }.call(valueTmp)!!
        !isBasicType(filed.returnType) -> fillObject(valueTmp.toJsonObject(), type)
        isBasicType(filed.returnType) -> covertBasic(valueTmp, filed.returnType)
        else -> valueTmp
    }
    filed.setter.call(pojo, v)
}

fun fillObjectHandleList(value: Any, it: KProperty<*>): Any {
    if (value is List<*> && value.isNotEmpty()) {
        val first = value.first() ?: return value
        return if (isBasicType(first::class.createType())) {
            value.map { vt -> vt?.let { covertBasic(vt, first::class.createType()) } }
        } else {
            val childType = it.returnType.arguments.first().type?.classifier as KClass<*>
            value.map { vt -> vt?.let { fillObject(vt.toJsonObject(), childType) } }
        }
    }
    return value
}


fun covertBasic(value: Any, typeTmp: KType): Any {
    var type = typeTmp
    if (typeTmp.isMarkedNullable) {
        type = typeTmp.classifier?.createType()!!
    }
    return when (type) {
        String::class.createType() -> value.toString()
        Int::class.createType() -> when (value) {
            is Int -> value
            is String -> value.toString().toInt()
            else -> value
        }

        Short::class.createType() -> when (value) {
            is Short -> value
            is String -> value.toString().toShort()
            else -> value
        }

        Long::class.createType() -> when (value) {
            is Long -> value
            is String -> value.toString().toLong()
            else -> value
        }

        Byte::class.createType() -> when (value) {
            is Byte -> value
            is String -> value.toString().toByte()
            else -> value
        }

        Float::class.createType() -> when (value) {
            is Float -> value
            is String -> value.toString().toFloat()
            else -> value
        }

        Double::class.createType() -> when (value) {
            is Double -> value
            is String -> value.toString().toDouble()
            else -> value
        }

        Boolean::class.createType() -> when (value) {
            is Boolean -> value
            is String -> value.toString().toBoolean()
            is Int -> value == 1
            is Long -> value == 1L
            else -> value
        }

        Char::class.createType() -> when (value) {
            is Char -> value
            else -> value
        }

        else -> value.toString()
    }

}


fun isBasicType(cs: KType?): Boolean {
    if (cs == null) return false
    var cst = cs
    if (cs.isMarkedNullable) {
        cst = cs.classifier?.createType()!!
    }
    return cst is SimpleType || isWrapper(cst) || cst == String::class.createType()
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
    if (this != null && !isNullOrBlankOrZero(this)) {
        fn(this)
    }
}

fun <T> T?.notNullAndExec(fn: (T) -> Unit) {
    if (this != null && !isNullOrBlankOrZero(this)) {
        fn(this)
    }
}

fun <T : Any> T.updateData(source: Any, ignoredNull: Boolean = true): T {
    val targetClazz = this::class.createType().classifier as KClass<*>
    val targetFields = targetClazz.memberProperties
    val sourceData = source.toJsonObject()
    targetFields.forEach {
        if (it is KMutableProperty1<*, *>) {
            val value = sourceData.getValue(it.name)

            if ((value != null && !isNullOrBlankOrZero(value)) || !ignoredNull) {
                setField(it, this, value)
            }
        }
    }
    return this
}

fun isNullOrBlankOrZero(v: Any): Boolean {
    return when (v) {
        is String -> v.isBlank()
        is Int -> v == 0
        is Double -> v == 0.0
        is Float -> v == 0.0f
        is Long -> v == 0L
        is List<*> -> v.isEmpty()
        is Map<*, *> -> v.isEmpty()
        else -> false
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
                        val loadClass =
                            ClassLoader.getSystemClassLoader().loadClass(filePath.replace('/', '.')).kotlin
                        result.add(loadClass)
                    }
                }
            }
        }
        return result
    }


}