package cn.minih.rocketmq.consumer

import cn.minih.rocketmq.consumer.impl.RocketReadStreamImpl
import io.vertx.core.Vertx
import io.vertx.core.streams.ReadStream
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer
import kotlin.reflect.KClass

/**
 *  mq消息消费流
 * @author hubin
 * @since 2023-08-10 09:05:46
 */
interface RocketReadStream<T : Any> : ReadStream<RocketConsumerRecord<T>> {
    val msgClazz: KClass<T>

    companion object {
        fun <T : Any> create(vertx: Vertx, consumer: SimpleConsumer, msgClazz: KClass<T>): RocketReadStream<T> {
            return RocketReadStreamImpl(vertx, consumer, msgClazz)
        }
    }

}
