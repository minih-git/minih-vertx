package cn.minih.rocketmq.producer.impl

import cn.minih.rocketmq.producer.IRocketProducerRecord
import cn.minih.rocketmq.producer.RocketProducer
import cn.minih.rocketmq.producer.RocketWriteStream
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.streams.WriteStream

/**
 *  rocket消息生产者实现
 * @author hubin
 * @since 2023-08-09 17:51:20
 */
class RocketProducerImpl<T : Any>(private val stream: RocketWriteStream<T>) : RocketProducer<T> {
    override fun exceptionHandler(handler: Handler<Throwable>?): WriteStream<IRocketProducerRecord<T>> {
        stream.exceptionHandler(handler)
        return this
    }

    override fun end(handler: Handler<AsyncResult<Void>>?) {
        stream.end(handler)
    }

    override fun setWriteQueueMaxSize(maxSize: Int): WriteStream<IRocketProducerRecord<T>> {
        stream.setWriteQueueMaxSize(maxSize)
        return this
    }

    override fun writeQueueFull(): Boolean {
        return stream.writeQueueFull()
    }

    override fun drainHandler(handler: Handler<Void>?): WriteStream<IRocketProducerRecord<T>> {
        stream.drainHandler(handler)
        return this
    }

    override fun write(data: IRocketProducerRecord<T>, handler: Handler<AsyncResult<Void>>) {
        stream.send(data, handler)

    }

    override fun write(data: IRocketProducerRecord<T>): Future<Void> {
        return stream.send(data).compose { Future.succeededFuture() }
    }

}