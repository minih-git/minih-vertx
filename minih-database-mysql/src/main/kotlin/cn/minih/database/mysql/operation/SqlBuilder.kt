package cn.minih.database.mysql.operation

import cn.minih.common.util.notNullAndExec
import cn.minih.database.mysql.annotation.TableName
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
                delete $tableName  
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
                when (it.type) {
                    QueryConditionType.EQ -> " = ?"
                    QueryConditionType.IN -> {
                        val perch = when {
                            it.value.isEmpty() -> "?"
                            else -> it.value.joinToString(",") { _ -> "?" }
                        }
                        " in (${perch})"
                    }

                    QueryConditionType.BETWEEN -> " between ? an ?"
                    QueryConditionType.GT -> " > ? "
                    QueryConditionType.LT -> " < ? "
                    QueryConditionType.GTE -> " >= ? "
                    QueryConditionType.LTE -> " =< ? "
                }
            )
        }
        return sql.replaceFirst("and", "where")
    }

    inline fun <reified T : Any> generateOderBySql(wrapper: Wrapper<T>): String {
        if (wrapper.orderByItems.isEmpty()) return ""
        var sql = "order by "
        wrapper.orderByItems.forEach {
            sql = sql.plus("${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.key)} ").plus(
                when (it.type) {
                    OrderByType.ASC -> " "
                    OrderByType.DESC -> " desc "
                }
            )
        }
        return sql
    }


}
