package cn.minih.core.repository

import io.vertx.core.json.JsonObject
import kotlin.reflect.KProperty1

/**
 * @author hubin
 * @date 2023/7/14
 * @desc
 */
class MongoQueryOption<V> : JsonObject() {
    fun put(k: KProperty1<V, Any>, v: Any): MongoQueryOption<V> {
        super.put(k.name, v)
        return this
    }
}
