package cn.minih.rocketmq.producer.impl

import cn.minih.rocketmq.producer.RocketProducer
import cn.minih.rocketmq.producer.RocketProducerRecord
import cn.minih.rocketmq.producer.RocketWriteStream
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.streams.WriteStream

/**
 *  rocket消息生产者实现
 * @author hubin
 * @since 2023-08-09 17:51:20
 */
class RocketProducerImpl<K, V>(val vertx: Vertx, val stream: RocketWriteStream<K, V>) : RocketProducer<K, V> {

    override fun exceptionHandler(handler: Handler<Throwable>?): WriteStream<RocketProducerRecord<K, V>> {
        TODO("Not yet implemented")
    }

    override fun end(handler: Handler<AsyncResult<Void>>?) {
        TODO("Not yet implemented")
    }

    override fun setWriteQueueMaxSize(maxSize: Int): WriteStream<RocketProducerRecord<K, V>> {
        TODO("Not yet implemented")
    }

    override fun writeQueueFull(): Boolean {
        TODO("Not yet implemented")
    }

    override fun drainHandler(handler: Handler<Void>?): WriteStream<RocketProducerRecord<K, V>> {
        TODO("Not yet implemented")
    }

    override fun write(data: RocketProducerRecord<K, V>?, handler: Handler<AsyncResult<Void>>?) {
        TODO("Not yet implemented")
    }

    override fun write(data: RocketProducerRecord<K, V>?): Future<Void> {
        TODO("Not yet implemented")
    }
}
