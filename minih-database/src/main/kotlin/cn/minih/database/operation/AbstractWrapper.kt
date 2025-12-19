@file:Suppress("UNCHECKED_CAST")

package cn.minih.database.operation

import cn.minih.database.enums.OrderByType
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
data class OrderByItem(val key: String, val type: OrderByType = OrderByType.ASC)
enum class QueryConditionType { EQ, NOT_EQ, IN, NOT_IN, BETWEEN, GT, LT, GTE, LTE, LIKE }

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

    fun notEq(key: KProperty1<T, *>, value: Any): Children {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf(value),
                QueryConditionType.NOT_EQ
            )
        )
        return this as Children
    }

    fun notEq(key: String, value: Any): Children {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                listOf(value),
                QueryConditionType.NOT_EQ
            )
        )
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

    fun gte(key: KProperty1<T, Any>, value: Any): Children {
        this.condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf(value),
                QueryConditionType.GTE
            )
        )
        return this as Children
    }

    fun lt(key: KProperty1<T, Any>, value: Any): Children {
        this.condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf(value),
                QueryConditionType.LT
            )
        )
        return this as Children
    }

    fun lte(key: KProperty1<T, Any>, value: Any): Children {
        this.condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf(value),
                QueryConditionType.LTE
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

    fun notIn(key: KProperty1<T, Any>, value: List<Any>): Children {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                value,
                QueryConditionType.NOT_IN
            )
        )
        return this as Children
    }

    fun like(key: KProperty1<T, Any>, value: Any): Children {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf("%$value%"),
                QueryConditionType.LIKE
            )
        )
        return this as Children
    }

    fun likeRight(key: KProperty1<T, Any>, value: Any): Children {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf("$value%"),
                QueryConditionType.LIKE
            )
        )
        return this as Children
    }

    fun likeLeft(key: KProperty1<T, Any>, value: Any): Children {
        condition.add(
            QueryCondition(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name),
                listOf("%$value"),
                QueryConditionType.LIKE
            )
        )
        return this as Children
    }


    fun maybeDo(fn: () -> Unit): Children {
        fn()
        return this as Children
    }


}