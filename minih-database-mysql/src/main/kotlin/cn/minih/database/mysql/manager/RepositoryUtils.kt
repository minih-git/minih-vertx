package cn.minih.database.mysql.manager

import cn.minih.common.util.Assert
import cn.minih.common.util.log
import cn.minih.common.util.toJsonObject
import cn.minih.core.util.SnowFlakeContext
import cn.minih.database.mysql.annotation.TableId
import cn.minih.database.mysql.enum.TableIdType
import cn.minih.database.mysql.operation.Wrapper
import com.google.common.base.CaseFormat
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

/**
 *  数据库操作工具类
 * @author hubin
 * @since 2023-08-15 16:23:58
 */
inline fun <reified T : Any> covert(row: Row, wrapper: Wrapper<T>): T {
    val entity = T::class.createInstance()
    val fields = entity::class.memberProperties
    fields.forEach {
        if (it is KMutableProperty1) {
            var type = it.returnType.classifier as KClass<*>
            val fieldName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.name)
            var stringToList = false
            if (type.simpleName === List::class.simpleName) {
                stringToList = true
                type = String::class
            }
            var valueRaw: Any? = null
            if (wrapper.selectItems.isNotEmpty()) {
                if (!wrapper.selectItems.contains(it.name)) {
                    return@forEach
                }
            }
            try {
                valueRaw = row.get(type.javaObjectType, fieldName)
            } catch (_: Exception) {
            }
            valueRaw?.let { v ->
                var value = v
                if (stringToList) {
                    value = valueRaw.toString().split(",").toList()
                }
                it.setter.call(entity, value)
            }

        }
    }
    return entity
}

fun <T : Any> insertPrimaryKey(entity: T) {
    val params = entity::class.primaryConstructor?.parameters
    params?.let {
        val primaryKey = it.first { p -> p.hasAnnotation<TableId>() }
        val tableId = primaryKey.findAnnotation<TableId>() ?: return
        val fields = entity::class.memberProperties
        fields.forEach { filed ->
            if (filed.name == primaryKey.name) {
                val id = filed.getter.call(entity)
                if ((id == null || id == 0L || id == 0) && tableId.value != TableIdType.AUTO_INCREMENT) {
                    if (filed is KMutableProperty1<*, *>) {
                        filed.setter.call(
                            entity, when (tableId.value) {
                                TableIdType.INPUT -> System.currentTimeMillis()
                                TableIdType.SNOWFLAKE -> SnowFlakeContext.instance.currentContext()
                                    .nextId(tableId.sfBusId)

                                TableIdType.UUID -> UUID.randomUUID().toString()
                                else -> ""
                            }
                        )
                    }
                    return
                }
            }
        }
    }
}

fun printLog(sql: String, tuple: Tuple, resultRaw: Any?) {
    log.info("====>sql:${sql}")
    log.info("====>参数:${tuple.deepToString()}")
    var result = resultRaw
    if (resultRaw is List<*>) {
        result = resultRaw.map { it?.toJsonObject() }
    }
    log.info("====>结果:${result}")
}

fun printLog(sql: String, tuples: List<Tuple>, resultRaw: Any?) {
    log.info("====>sql:${sql}")
    log.info("====>参数:${tuples.map { it.deepToString() }}")
    var result = resultRaw
    if (resultRaw is List<*>) {
        result = resultRaw.map { it?.toJsonObject() }
    }
    log.info("====>结果:${result}")
}

fun printWarningLog(sql: String, tuple: Tuple, resultRaw: Any?) {
    log.warn("====>sql:${sql}")
    log.warn("====>参数:${tuple.deepToString()}")
    var result = resultRaw
    if (resultRaw is List<*>) {
        result = resultRaw.map { it?.toJsonObject() }
    }
    log.warn("====>结果:${result}")
}

fun printWarningLog(sql: String, tuples: List<Tuple>, resultRaw: Any?) {
    log.warn("====>sql:${sql}")
    log.warn("====>参数:${tuples.map { it.deepToString() }}")
    var result = resultRaw
    if (resultRaw is List<*>) {
        result = resultRaw.map { it?.toJsonObject() }
    }
    log.warn("====>结果:${result}")
}

fun getNextCursorByFieldName(name: String, data: Any): Long {
    val clazz = data::class
    val field = clazz.memberProperties.first { it.name == name }
    return when (val value = field.getter.call(data)) {
        null -> 0
        is Long -> value
        else -> value.hashCode().toLong()
    }
}

inline fun <reified A : Annotation> getFieldByAnnotation(clazz: KClass<*>): KParameter? {
    val params = clazz.primaryConstructor?.parameters
    Assert.notNull(params, "未找到数据类字段！")
    return params!!.firstOrNull { p -> p.hasAnnotation<A>() }
}

inline fun <reified A : Annotation> getFieldAnnotation(clazz: KClass<*>): A? {
    val params = clazz.primaryConstructor?.parameters
    Assert.notNull(params, "未找到数据类字段！")
    return params!!.firstOrNull { p -> p.hasAnnotation<A>() }?.findAnnotation<A>()
}
