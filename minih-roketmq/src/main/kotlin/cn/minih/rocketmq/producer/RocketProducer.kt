package cn.minih.rocketmq.producer

import cn.minih.rocketmq.producer.impl.RocketProducerImpl
import io.vertx.core.Vertx
import io.vertx.core.streams.WriteStream

/**
 *  rocketmq客户端
 * @author hubin
 * @since 2023-08-09 17:42:09
 */
interface RocketProducer<K, V> : WriteStream<RocketProducerRecord<K, V>> {

    companion object {
        fun <K, V> create(vertx: Vertx): RocketProducer<K, V> {
            val stream = RocketWriteStream.create<K, V>(vertx)
            return RocketProducerImpl(vertx, stream)
        }
    }


}
