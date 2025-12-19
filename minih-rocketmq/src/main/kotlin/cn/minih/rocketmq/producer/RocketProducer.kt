@file:Suppress("unused")

package cn.minih.rocketmq.producer

import cn.minih.rocketmq.client.RocketClient
import cn.minih.rocketmq.producer.impl.RocketProducerImpl
import io.vertx.core.Vertx
import io.vertx.core.streams.WriteStream

/**
 *  rocketmq客户端
 * @author hubin
 * @since 2023-08-09 17:42:09
 */
interface RocketProducer<T : Any> : WriteStream<RocketProducerRecord<T>> {
    companion object {
        fun <T : Any> create(vertx: Vertx): RocketProducer<T> {
            val stream = RocketWriteStream.create<T>(vertx, RocketClient.producer)
            return RocketProducerImpl(stream)
        }
    }


}