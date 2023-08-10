package cn.minih.rocketmq

import org.apache.rocketmq.client.apis.ClientConfigurationBuilder
import org.apache.rocketmq.client.apis.consumer.FilterExpression
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType
import org.apache.rocketmq.client.java.impl.consumer.SimpleConsumerBuilderImpl
import java.time.Duration

/**
 * 生产者测试
 * @author hubin
 * @since 2023-08-09 23:41:45
 * @desc
 */
class ProducerTest

data class A(val a: String)

fun main() {
    val configBuild = ClientConfigurationBuilder().setEndpoints("foobar.com:8080").build()
    val filterExpression = FilterExpression("", FilterExpressionType.TAG)
    val consumer = SimpleConsumerBuilderImpl().setClientConfiguration(configBuild)
        .setConsumerGroup("aaa_aa")
        .setAwaitDuration(Duration.ofSeconds(15))
        .setSubscriptionExpressions(mapOf("aaa_aa" to filterExpression)).build()


//    val vertx = Vertx.vertx()

//    val config = vertx.orCreateContext.config()

//    RocketConsumer.create<A>(vertx).subscribe("aaa_aa","aaa_aa").handler {
//        println("AAAAAAAA:${it.data.a}")
//    }

//    vertx.executeBlocking<Void> {
//        config.put("minih", jsonObjectOf("rocketmq" to jsonObjectOf("endpoints" to "122.51.65.210:9801")))
//        println("0")
//        RocketConsumer.create<A>(vertx).subscribe("aaa_aa","aaa_aa").handler {
//            println("AAAAAAAA:${it.data.a}")
//        }
//        println("4")
//        val data = RocketProducerRecord.from("aaa_aa", A("aaa"))
//        val producer = RocketProducer.create<A>(vertx)
//        println("5")
//        producer.write(data).onComplete { println(it.result()) }.onFailure { println(it.message) }
//
//
//    }
}
