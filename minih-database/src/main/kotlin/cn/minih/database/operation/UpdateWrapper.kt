package cn.minih.database.operation

import com.google.common.base.CaseFormat
import kotlin.reflect.KProperty1

/**
 * 更新包装
 * @author hubin
 * @since 2023-07-27 22:34:01
 * @desc
 */
class UpdateWrapper<T : Any> : AbstractWrapper<T, UpdateWrapper<T>, UpdateWrapper<T>>(), Update<UpdateWrapper<T>, T> {
    override fun set(key: KProperty1<T, Any>, value: Any?): UpdateWrapper<T> {
        return maybeDo {
            updateItems.add(UpdateItem(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.name), value))
        }
    }

    override fun set(key: String, value: Any?): UpdateWrapper<T> {
        return maybeDo {
            updateItems.add(UpdateItem(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), value))
        }
    }
}