package cn.minih.core.eventbus

import io.vertx.core.json.JsonObject

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
abstract class EventBusConsumer(var channel: String) {

    abstract suspend fun exec(obj: JsonObject)


}