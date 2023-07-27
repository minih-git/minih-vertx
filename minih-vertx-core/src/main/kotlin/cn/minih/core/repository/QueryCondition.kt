package cn.minih.core.repository

import com.google.common.base.CaseFormat
import kotlin.reflect.KProperty1

/**
 *  查询条件
 * @author hubin
 * @since 2023-07-27 11:09:38
 */
enum class QueryConditionType {
    EQ, IN, BETWEEN, GT, LT, GTE, LTE
}

data class QueryCondition(val key: String, val value: List<Any>, val type: QueryConditionType = QueryConditionType.EQ)
class QueryWrapper<T> {


    val condition: MutableList<QueryCondition> = mutableListOf()

    fun eq(key: KProperty1<T, Any>, value: Any): QueryWrapper<T> {
        condition.add(QueryCondition(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name), listOf(value)))
        return this
    }

    fun gt(key: KProperty1<T, Any>, value: Any): QueryWrapper<T> {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf(value),
                QueryConditionType.GT
            )
        )
        return this
    }

    fun `in` (key: KProperty1<T, Any>, value: List<Any>): QueryWrapper<T> {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                value,
                QueryConditionType.IN
            )
        )
        return this
    }
}
