@file:Suppress("unused")

package cn.minih.rocketmq.producer

import cn.minih.rocketmq.producer.impl.RocketProducerRecordImpl
import org.apache.rocketmq.client.apis.message.Message
import java.util.*

/**
 *  消息记录
 * @author hubin
 * @since 2023-08-09 17:47:15
 */
interface RocketProducerRecord<T : Any> : Message {

    companion object {
        fun <T : Any> from(
            topic: String,
            data: T,
            tag: Optional<String> = Optional.empty(),
            keys: MutableCollection<String> = mutableListOf(),
            messageGroup: Optional<String> = Optional.empty(),
            deliveryTimestamp: Optional<Long> = Optional.empty(),
            properties: MutableMap<String, String> = mutableMapOf()
        ): RocketProducerRecord<T> {
            return RocketProducerRecordImpl(topic, data, tag, keys, messageGroup, deliveryTimestamp, properties)
        }
    }
}