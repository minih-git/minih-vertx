package cn.minih.core.handler

import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
abstract class EventBusConsumer(var channel: String) {

    abstract suspend fun exec(obj: JsonObject)

    fun setConsumer(consumer: MessageConsumer<JsonObject>) {}

}
