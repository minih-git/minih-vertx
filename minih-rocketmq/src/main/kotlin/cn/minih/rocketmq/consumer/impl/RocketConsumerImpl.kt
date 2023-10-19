package cn.minih.rocketmq.consumer.impl

import cn.minih.common.exception.MinihException
import cn.minih.common.util.Assert
import cn.minih.common.util.getConfig
import cn.minih.rocketmq.config.RocketConfig
import cn.minih.rocketmq.consumer.RocketConsumer
import cn.minih.rocketmq.consumer.RocketConsumerRecord
import cn.minih.rocketmq.consumer.RocketReadStream
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.streams.ReadStream
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder
import org.apache.rocketmq.client.apis.consumer.FilterExpression
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType
import org.apache.rocketmq.client.java.impl.consumer.SimpleConsumerBuilderImpl
import java.time.Duration
import kotlin.reflect.KClass

/**
 *  mq消息消费者实现
 * @author hubin
 * @since 2023-08-10 09:06:48
 */
class RocketConsumerImpl<T : Any>(private val vertx: Vertx, private val msgClazz: KClass<T>) : RocketConsumer<T> {
    private lateinit var stream: RocketReadStream<T>
    override fun subscribe(topic: String, consumerGroup: String, tag: String): RocketConsumer<T> {
        try {
            Assert.notBlank(consumerGroup) { MinihException("consumerGroup 不能为空！") }
            val config = getConfig("rocketmq", RocketConfig::class)
            Assert.notBlank(config.endpoints) { MinihException("请设置rocketmq的地址") }
            val configBuild = ClientConfigurationBuilder().setEndpoints(config.endpoints).build()
            val filterExpression = FilterExpression(tag, FilterExpressionType.TAG)
            val consumer = SimpleConsumerBuilderImpl().setClientConfiguration(configBuild)
                .setConsumerGroup(consumerGroup)
                .setAwaitDuration(Duration.ofSeconds(15))
                .setSubscriptionExpressions(mapOf(topic to filterExpression)).build()
            this.stream = RocketReadStream.create(vertx, consumer, msgClazz)
            return this
        } catch (e: Throwable) {
            throw e
        }
    }


    override fun exceptionHandler(handler: Handler<Throwable>?): ReadStream<RocketConsumerRecord<T>> {
        stream.exceptionHandler(handler)
        return this
    }


    override fun pause(): ReadStream<RocketConsumerRecord<T>> {
        stream.pause()
        return this
    }

    override fun resume(): ReadStream<RocketConsumerRecord<T>> {
        stream.resume()
        return this
    }

    override fun fetch(amount: Long): ReadStream<RocketConsumerRecord<T>> {
        stream.fetch(amount)
        return this
    }

    override fun endHandler(endHandler: Handler<Void>?): ReadStream<RocketConsumerRecord<T>> {
        this.stream.endHandler(endHandler)
        return this
    }

    override fun handler(handler: Handler<RocketConsumerRecord<T>>?): ReadStream<RocketConsumerRecord<T>> {
        this.stream.handler(handler)
        return this
    }

}
