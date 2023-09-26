@file:Suppress("unused")

package cn.minih.common.util

import cn.minih.common.exception.MinihArgumentErrorException
import cn.minih.common.exception.MinihDataCovertException
import cn.minih.common.exception.MinihException
import cn.minih.core.config.CoreConfig
import cn.minih.core.config.IConfig
import cn.minih.core.config.PROJECT_NAME
import cn.minih.web.annotation.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.jsonObjectOf
import java.io.File
import java.io.IOException
import java.net.*
import java.util.*
import java.util.jar.JarFile
import kotlin.reflect.*
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

fun getGson(): Gson {
    return GsonBuilder().serializeNulls().create()
}

fun <T : Any> JsonObject.covertTo(clazz: KClass<T>): T {
    return getGson().fromJson(this.toString(), clazz.java)
}

fun JsonObject.covertTo(kType: KType): Any {
    return this.covertTo(kType.classifier as KClass<*>)
}

fun Any.toJsonObject(): JsonObject {
    if (this is JsonObject) {
        return this
    }
    return JsonObject(getGson().toJson(this))
}

fun Any.toJsonString(): String {
    return getGson().toJson(this)
}

fun <T : Any> String.jsonConvertData(clazz: KClass<T>): T {
    return getGson().fromJson(this, clazz.java)
}

private fun getRootConfigRaw(context: Context = Vertx.currentContext()): JsonObject {
    val configRaw = context.config()
    val c = configRaw.getValue(PROJECT_NAME)
    if (c is JsonObject) {
        return c
    }
    return JsonObject()
}

fun getConfig(): CoreConfig {
    return getRootConfig()
}


fun getRootConfig(context: Context = Vertx.currentContext()): CoreConfig {
    val configRaw = getRootConfigRaw(context)
    return fillObject(configRaw, CoreConfig::class)
}

fun getProjectName(context: Context = Vertx.currentContext()): String {
    return getRootConfig(context).name
}

fun getEnv(): String {
    return getRootConfig().env
}

fun <T : IConfig> getConfig(
    configName: String,
    configClass: KClass<T>,
    context: Context = Vertx.currentContext()
): T {
    val config = getRootConfigRaw(context).getJsonObject(configName) ?: jsonObjectOf()
    return fillObject(config, configClass)
}

fun <T : Any> fillObject(jsonObject: JsonObject, clazz: KClass<T>): T {
    var pojo = "{}".jsonConvertData(clazz)
    try {
        val cons = clazz.primaryConstructor!!
        val values = cons.parameters.filterNot { it.isOptional }.associateWith { null }
        pojo = cons.callBy(values)
    } catch (e: Exception) {
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
        val first = value.firstOrNull() ?: return value
        return if (isBasicType(first::class.createType())) {
            value.map { vt -> vt?.let { covertBasic(vt, first::class.createType()) } }
        } else {
            val childType = it.returnType.arguments.first().type?.classifier as KClass<*>
            value.map { vt -> vt?.let { fillObject(vt.toJsonObject(), childType) } }
        }
    }
    return value
}

fun covertTypeData(value: Any, type: KType): Any {
    val clazz = type.classifier as KClass<*>
    return when {
        isBasicType(type) -> covertBasic(value, type)
        clazz.simpleName === List::class.simpleName -> covertListData(
            value,
            type.arguments.first().type!!.classifier as KClass<*>
        )

        clazz.superclasses.contains(Enum::class) -> clazz.functions.first { f -> f.name == "valueOf" }.call(value)!!
        else -> value.toJsonObject().covertTo(type)
    }
}

fun covertListData(value: Any, clazz: KClass<*>? = null): Any {
    if ((value is Iterable<*>)) {
        if (value.iterator().hasNext()) {
            val first = value.firstOrNull() ?: return value
            var firstClass = first::class
            clazz?.let {
                if (!getSuperClassRecursion(it).contains(Iterable::class)) {
                    firstClass = clazz
                }
            }
            return when {
                isBasicType(firstClass.createType()) -> value.map { vt ->
                    vt?.let {
                        covertBasic(
                            vt,
                            firstClass.createType()
                        )
                    }
                }

                firstClass == JsonObject::class -> value
                else -> value.map { vt -> vt?.let { fillObject(vt.toJsonObject(), firstClass) } }
            }
        }
        return emptyList<Any>()
    }
    return value
}


fun covertBasic(value: Any, typeTmp: KType, tryString: Boolean = true): Any {
    var type = typeTmp
    if (typeTmp.isMarkedNullable) {
        type = typeTmp.classifier?.createType()!!
    }
    return when (type) {
        String::class.createType() -> value.toString()
        Int::class.createType() -> when {
            value is Int -> value
            tryString && value is String -> if (value.isBlank()) 0 else value.toString().toInt()
            else -> throw MinihDataCovertException("非string类数据")
        }

        Short::class.createType() -> when {
            value is Short -> value
            tryString && value is String -> if (value.isBlank()) 0 else value.toString().toShort()
            else -> throw MinihDataCovertException("非short类数据")
        }

        Long::class.createType() -> when {
            value is Long -> value
            value is Int -> value.toLong()
            tryString && value is String -> if (value.isBlank()) 0 else value.toString().toLong()
            else -> throw MinihDataCovertException("非long类数据")
        }

        Byte::class.createType() -> when {
            value is Byte -> value
            tryString && value is String -> value.toString().toByte()
            else -> throw MinihDataCovertException("非byte类数据")
        }

        Float::class.createType() -> when {
            value is Float -> value
            tryString && value is String -> if (value.isBlank()) 0.0F else value.toString().toFloat()
            else -> throw MinihDataCovertException("非float类数据")
        }

        Double::class.createType() -> when {
            value is Double -> value
            tryString && value is String -> if (value.isBlank()) 0.0 else value.toString().toDouble()
            else -> throw MinihDataCovertException("非double类数据")
        }

        Boolean::class.createType() -> when {
            value is Boolean -> value
            tryString && value is String -> if (value.isBlank()) false else value.toString().toBoolean()
            value is Int -> value == 1
            value is Long -> value == 1L
            else -> throw MinihDataCovertException("非boolean类数据")
        }

        Char::class.createType() -> when (value) {
            is Char -> value
            else -> throw MinihDataCovertException("非char类数据")
        }

        else -> throw MinihDataCovertException("非基础数据类型")
    }

}

fun <T : Any> covertBasic(value: Any, clazz: KClass<T>, tryString: Boolean = true): T {
    @Suppress("UNCHECKED_CAST")
    return covertBasic(value, clazz.createType(), tryString) as T
}

inline fun <reified T> covertBasic(value: Any, tryString: Boolean): T {
    val clazz = T::class
    return covertBasic(value, clazz.createType(), tryString) as T
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

val <T>T?.notNull: Boolean get() = this != null && !isNullOrBlankOrZero(this)
val <T>T?.isNull: Boolean get() = this == null || isNullOrBlankOrZero(this)

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

fun <T : Any> T.updateData(
    source: Any,
    ignoredNull: Boolean = true,
    ignoredField: List<KProperty1<T, *>> = listOf()
): T {
    val targetClazz = this::class.createType().classifier as KClass<*>
    val targetFields = targetClazz.memberProperties
    val sourceData = source.toJsonObject()
    targetFields.forEach {
        if (ignoredField.isEmpty() || !ignoredField.contains(it)) {
            if (it is KMutableProperty1<*, *>) {
                val value = sourceData.getValue(it.name)
                if ((value != null && !isNullOrBlankOrZero(value)) || !ignoredNull) {
                    setField(it, this, value)
                }
            }
        }

    }
    return this
}

fun isNullOrBlankOrZero(v: Any): Boolean {
    return when (v) {
        is String -> v.isBlank() || v == "null"
        is Int -> v == 0
        is Double -> v == 0.0
        is Float -> v == 0.0f
        is Long -> v == 0L
        is List<*> -> v.isEmpty()
        is Map<*, *> -> v.isEmpty()
        else -> false
    }
}

fun generateArgs(argsNeed: List<KParameter>, params: JsonObject): List<Pair<KParameter, Any?>> {
    return argsNeed.map { argsType ->
        val type = argsType.type
        val typeClass = type.classifier as KClass<*>
        val isMarkedNullable = type.isMarkedNullable
        val param = argsType.name?.let {
            when {
                isBasicType(type) && !params.containsKey(it) -> null
                isBasicType(type) -> covertBasic(params[it], type)
                typeClass.simpleName === List::class.simpleName -> {
                    val d = params.getJsonArray(it)
                    when {
                        d == null -> listOf<Any>()
                        d.isEmpty -> listOf<Any>()
                        else -> {
                            val childType = type.arguments.first().type?.classifier as KClass<*>
                            when {
                                isBasicType(childType.createType()) -> d.map { vt ->
                                    vt?.let {
                                        covertBasic(
                                            vt,
                                            childType.createType()
                                        )
                                    }
                                }

                                else -> d.map { vt -> vt?.let { fillObject(vt.toJsonObject(), childType) } }
                            }
                        }
                    }
                }

                typeClass.superclasses.contains(Enum::class) -> typeClass.functions.first { it.name == "valueOf" }
                    .call(params[it])!!

                else -> params.covertTo(type)
            }
        }
        if (isBasicType(type)) {
            if (!isMarkedNullable && param == null) {
                throw MinihArgumentErrorException("参数：${argsType.name} 不能为空！")
            }
        } else {
            val c = type.classifier as KClass<*>
            if (c.isData) {
                c.primaryConstructor?.parameters?.forEach {
                    if (!it.type.isMarkedNullable) {
                        val field = c.memberProperties.find { p -> p.name == it.name }
                        field?.getter?.call(param) ?: throw MinihArgumentErrorException("参数：${it.name} 不能为空！")
                    }
                }
            }
        }
        Pair(argsType, param)
    }
}

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

fun getSuperClassRecursion(clazz: KClass<*>): Set<KClass<*>> {
    val list = mutableSetOf<KClass<*>>()
    val superTypes = clazz.supertypes
    if (superTypes.isNotEmpty()) {
        superTypes.forEach {
            val clazz1 = it.classifier as KClass<*>
            list.addAll(getSuperClassRecursion(clazz1))
        }
    }
    list.addAll(clazz.superclasses)
    return list
}

fun findRequestMapping(t: KAnnotatedElement): RequestMapping? {
    val requestType = when {
        t.findAnnotation<Post>() != null -> t.findAnnotation<Post>()
        t.findAnnotation<Get>() != null -> t.findAnnotation<Get>()
        t.findAnnotation<Put>() != null -> t.findAnnotation<Put>()
        t.findAnnotation<Delete>() != null -> t.findAnnotation<Delete>()
        t.findAnnotation<Request>() != null -> t.findAnnotation<Request>()
        else -> null
    } ?: return null
    val fields = requestType::class.memberProperties
    var url = ""
    fields.forEach { field ->
        if (field.name == "value") {
            url = field.getter.call(requestType).toString()
        }
    }

    return RequestMapping(url, requestType)
}

fun formatPath(pathTmp: String): String {
    var path = pathTmp
    if (!path.startsWith("/")) {
        path = "/$path"
    }
    if (path.endsWith("/")) {
        path = path.substring(0, path.length - 1)
    }
    return path
}

fun getIpLong(ip: String): Long {
    val s: List<String> = ip.split(".")
    return ((s[0].toLong() shl 24)
            + (s[1].toLong() shl 16) +
            (s[2].toLong() shl 8) + s[3].toLong())
}

fun getMinihException(e: Throwable): Throwable {
    var me = e
    for (i in 0..5) {
        if (me is MinihException) {
            break
        }
        me.cause?.let {
            me = me.cause as Exception
        }
    }
    return me
}
