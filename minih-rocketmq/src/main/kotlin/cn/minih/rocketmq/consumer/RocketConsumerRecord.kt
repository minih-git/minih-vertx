package cn.minih.rocketmq.consumer

import cn.minih.rocketmq.consumer.impl.RocketConsumerRecordImpl

/**
 *  消费者消息
 * @author hubin
 * @since 2023-08-10 09:02:31
 */
interface RocketConsumerRecord<T> {
    val data: T

    companion object {
        fun <T> create(data: T): RocketConsumerRecord<T> {
            return RocketConsumerRecordImpl(data)
        }
    }


}
