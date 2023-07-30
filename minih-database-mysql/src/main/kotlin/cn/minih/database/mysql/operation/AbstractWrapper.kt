@file:Suppress("UNCHECKED_CAST")

package cn.minih.database.mysql.operation

import com.google.common.base.CaseFormat
import kotlin.reflect.KProperty1


/**
 * 查询包装
 * @author hubin
 * @since 2023-07-27 22:07:58
 * @desc
 */
data class UpdateItem(val key: String, val value: Any?)
data class QueryCondition(val key: String, val value: List<Any>, val type: QueryConditionType = QueryConditionType.EQ)

enum class QueryConditionType { EQ, IN, BETWEEN, GT, LT, GTE, LTE }

@Suppress("unused")
abstract class AbstractWrapper<T, R, Children : AbstractWrapper<T, R, Children>> : Wrapper<T>() {

    fun eq(key: KProperty1<T, *>, value: Any): Children {
        condition.add(QueryCondition(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name), listOf(value)))
        return this as Children
    }

    fun eq(key: String, value: Any): Children {
        condition.add(QueryCondition(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), listOf(value)))
        return this as Children
    }

    fun gt(key: KProperty1<T, Any>, value: Any): Children {
        this.condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf(value),
                QueryConditionType.GT
            )
        )
        return this as Children
    }

    fun `in`(key: KProperty1<T, Any>, value: List<Any>): Children {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                value,
                QueryConditionType.IN
            )
        )
        return this as Children
    }

    fun maybeDo(fn: () -> Unit): Children {
        fn()
        return this as Children
    }


}