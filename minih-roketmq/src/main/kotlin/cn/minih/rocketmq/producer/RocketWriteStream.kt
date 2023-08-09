package cn.minih.rocketmq.producer

import cn.minih.rocketmq.producer.impl.RocketWriteStreamImpl
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.streams.WriteStream
import org.apache.rocketmq.client.apis.producer.Producer
import org.apache.rocketmq.client.apis.producer.SendReceipt

/**
 *  rocketWriteStream
 * @author hubin
 * @since 2023-08-09 17:53:45
 */
interface RocketWriteStream<T : Any> : WriteStream<IRocketProducerRecord<T>> {

    companion object {
        fun <T : Any> create(vertx: Vertx, producer: Producer): RocketWriteStream<T> {
            return RocketWriteStreamImpl(vertx, producer)
        }
    }

    fun send(message: IRocketProducerRecord<T>): Future<SendReceipt>
    fun send(message: IRocketProducerRecord<T>, handler: Handler<AsyncResult<Void>>)


}