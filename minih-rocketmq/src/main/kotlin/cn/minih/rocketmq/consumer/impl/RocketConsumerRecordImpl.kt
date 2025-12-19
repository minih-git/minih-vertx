package cn.minih.rocketmq.consumer.impl

import cn.minih.rocketmq.consumer.RocketConsumerRecord

/**
 *  消费者消息
 * @author hubin
 * @since 2023-08-10 12:48:37
 */
class RocketConsumerRecordImpl<T>(override val data: T) : RocketConsumerRecord<T> {

}
