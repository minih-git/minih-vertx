package cn.minih.database.mysql.operation

import cn.minih.common.util.notNullAndExec
import cn.minih.database.mysql.annotation.TableName
import cn.minih.database.mysql.enum.OrderByType
import com.google.common.base.CaseFormat
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 *  Sql构建器
 * @author hubin
 * @since 2023-08-09 09:33:57
 */
object SqlBuilder {
    inline fun <reified T : Any> generateQuerySql(wrapper: Wrapper<T>): String {
        var tableName = T::class.findAnnotation<TableName>()?.value
        if (tableName.isNullOrBlank()) {
            tableName = T::class.simpleName?.let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
        }
        var keys = "*"
        wrapper.selectItems.notNullAndExec {
            keys =
                wrapper.selectItems.joinToString(",") { k -> CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, k) }
        }

        return """
                select $keys from $tableName  ${generateConditionSql(wrapper)} ${generateOderBySql(wrapper)}
            """.trimIndent()
    }

    inline fun <reified T : Any> generateCountQuerySql(wrapper: Wrapper<T>): String {
        var tableName = T::class.findAnnotation<TableName>()?.value
        if (tableName.isNullOrBlank()) {
            tableName = T::class.simpleName?.let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
        }
        return """
                select count(1) as count from $tableName  ${generateConditionSql(wrapper)} ${generateOderBySql(wrapper)}
            """.trimIndent()
    }

    inline fun <reified T : Any> generateUpdateSql(wrapper: Wrapper<T>): String {
        var tableName = T::class.findAnnotation<TableName>()?.value
        if (tableName.isNullOrBlank()) {
            tableName = T::class.simpleName?.let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
        }
        val sql = """
                update $tableName  set 
            """.trimIndent()
            .plus(wrapper.updateItems.joinToString(", ") {
                "${
                    CaseFormat.LOWER_CAMEL.to(
                        CaseFormat.LOWER_UNDERSCORE,
                        it.key
                    )
                } = ? "
            })
        return sql.plus("  ${generateConditionSql(wrapper)}")
    }

    inline fun <reified T : Any> generateDeleteSql(wrapper: Wrapper<T>): String {
        var tableName = T::class.findAnnotation<TableName>()?.value
        if (tableName.isNullOrBlank()) {
            tableName = T::class.simpleName?.let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
        }
        val sql = """
                delete from  $tableName  
            """
        return sql.plus("  ${generateConditionSql(wrapper)}")
    }

    inline fun <reified T : Any> generateInsertSql(entity: T): String {
        var tableName = entity::class.findAnnotation<TableName>()?.value
        if (tableName.isNullOrBlank()) {
            tableName = T::class.simpleName?.let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
        }
        val fields = T::class.memberProperties
        val sql = """
                insert into $tableName (
            """.trimIndent()
            .plus(fields.joinToString(", ") { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.name) })
            .plus(") values (")
            .plus(fields.filter { it is KMutableProperty1<*, *> }.joinToString(", ") { "?" })
        return sql.plus(")")

    }

    inline fun <reified T : Any> generateConditionSql(wrapper: Wrapper<T>): String {
        var sql = ""
        wrapper.condition.forEach {
            sql = sql.plus(" and  ${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.key)} ").plus(
                getConditionSqlByType(it.type, it.value)
            )
        }
        return sql.replaceFirst("and", "where")
    }

    fun getConditionSqlByType(type: QueryConditionType, value: List<Any>): String {
        return when (type) {
            QueryConditionType.EQ -> " = ?"
            QueryConditionType.NOT_EQ -> " != ?"
            QueryConditionType.IN -> {
                val perch = when {
                    value.isEmpty() -> "?"
                    else -> value.joinToString(",") { _ -> "?" }
                }
                " in (${perch})"
            }

            QueryConditionType.NOT_IN -> {
                val perch = when {
                    value.isEmpty() -> "?"
                    else -> value.joinToString(",") { _ -> "?" }
                }
                " not in (${perch})"
            }

            QueryConditionType.BETWEEN -> " between ? an ?"
            QueryConditionType.GT -> " > ? "
            QueryConditionType.LT -> " < ? "
            QueryConditionType.GTE -> " >= ? "
            QueryConditionType.LTE -> " <= ? "
            QueryConditionType.LIKE -> " like ? "

        }
    }

    inline fun <reified T : Any> generateOderBySql(wrapper: Wrapper<T>): String {
        if (wrapper.orderByItems.isEmpty()) return ""
        return "order by ".plus(wrapper.orderByItems.map {
            "${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.key)} ".plus(
                when (it.type) {
                    OrderByType.ASC -> " "
                    OrderByType.DESC -> " desc "
                }
            )
        }.joinToString(","))
    }


}
