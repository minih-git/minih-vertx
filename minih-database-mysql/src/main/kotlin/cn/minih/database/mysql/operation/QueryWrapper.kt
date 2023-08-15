@file:Suppress("unused")

package cn.minih.database.mysql.operation

import kotlin.reflect.KProperty1

/**
 *  查询条件
 * @author hubin
 * @since 2023-07-27 11:09:38
 */
open class QueryWrapper<T : Any> : AbstractWrapper<T, QueryWrapper<T>, QueryWrapper<T>>() {

    fun orderBy(vararg key: KProperty1<T, Any>, orderType: OrderByType = OrderByType.ASC): QueryWrapper<T> {
        return maybeDo {
            key.forEach {
                orderByItems.add(OrderByItem(it.name, orderType))
            }
        }
    }

    fun orderByDesc(vararg key: KProperty1<T, Any>): QueryWrapper<T> {
        return maybeDo {
            key.forEach {
                orderByItems.add(OrderByItem(it.name, OrderByType.DESC))
            }
        }
    }

    fun selects(vararg key: KProperty1<T, Any>): QueryWrapper<T> {
        return maybeDo {
            key.forEach {
                selectItems.add(it.name)
            }
        }
    }

}
