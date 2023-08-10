package cn.minih.rocketmq.producer.impl

import cn.minih.common.exception.MinihException
import cn.minih.common.util.Assert
import cn.minih.common.util.toJsonString
import cn.minih.rocketmq.producer.RocketProducerRecord
import java.nio.ByteBuffer
import java.util.*
import java.util.regex.Pattern

/**
 * Rocket数据
 * @author hubin
 * @since 2023-08-09 23:13:01
 * @desc
 */
val TOPIC_PATTERN: Pattern = Pattern.compile("^[%a-zA-Z0-9_-]+$")

class RocketProducerRecordImpl<T : Any>(
    private val topic: String,
    private val data: T,
    private val tag: Optional<String> = Optional.empty(),
    private val keys: MutableCollection<String> = mutableListOf(),
    private val messageGroup: Optional<String> = Optional.empty(),
    private val deliveryTimestamp: Optional<Long> = Optional.empty(),
    private val properties: MutableMap<String, String> = mutableMapOf()
) : RocketProducerRecord<T> {

    init {
        Assert.isTrue(TOPIC_PATTERN.matcher(topic).matches()) {
            MinihException("topic 字段不符合规范")
        }
    }

    override fun getTopic(): String {
        return topic
    }

    override fun getBody(): ByteBuffer {
        return ByteBuffer.wrap(data.toJsonString().toByteArray())
    }

    override fun getProperties(): MutableMap<String, String> {
        return properties
    }

    override fun getTag(): Optional<String> {
        return tag
    }

    override fun getKeys(): MutableCollection<String> {
        return keys
    }

    override fun getMessageGroup(): Optional<String> {
        return messageGroup
    }

    override fun getDeliveryTimestamp(): Optional<Long> {
        return deliveryTimestamp
    }
}