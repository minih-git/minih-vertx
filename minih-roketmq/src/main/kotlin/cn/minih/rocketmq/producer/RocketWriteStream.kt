package cn.minih.rocketmq.producer

import cn.minih.rocketmq.producer.impl.RocketWriteStreamImpl
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.streams.WriteStream
import org.apache.rocketmq.client.apis.producer.Producer
import org.apache.rocketmq.client.apis.producer.SendReceipt

/**
 *  rocketWriteStream
 * @author hubin
 * @since 2023-08-09 17:53:45
 */
interface RocketWriteStream<K, V> : WriteStream<RocketProducerRecord<K, V>> {

    val DEFAULT_MAX_SIZE: Int get() = 1024 * 1024

    companion object {
        fun <K, V> create(vertx: Vertx, producer: Producer): RocketWriteStream<K, V> {
            return RocketWriteStreamImpl(vertx, JsonObject(), producer)
        }
    }

    fun send(message: RocketProducerRecord<K, V>): Future<SendReceipt>


}
