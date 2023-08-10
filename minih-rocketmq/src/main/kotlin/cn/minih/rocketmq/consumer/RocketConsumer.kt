package cn.minih.rocketmq.consumer

import cn.minih.rocketmq.consumer.impl.RocketConsumerImpl
import io.vertx.core.Vertx
import io.vertx.core.streams.ReadStream

/**
 *  rocketmq消费者
 * @author hubin
 * @since 2023-08-10 08:58:17
 */
interface RocketConsumer<T : Any> : ReadStream<RocketConsumerRecord<T>> {

    companion object {
        inline fun <reified T : Any> create(vertx: Vertx): RocketConsumer<T> {
            return RocketConsumerImpl(vertx, T::class)
        }
    }

    fun subscribe(topic: String, consumerGroup: String = "", tag: String = ""): RocketConsumer<T>


}
