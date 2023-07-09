package cn.minih.app.system.auth

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.shareddata.AsyncMap
import java.util.concurrent.ThreadLocalRandom

/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
object AuthUtil {
    val config: Future<AsyncMap<String, String>>
        get() {
            val sharedData = Vertx.currentContext().owner().sharedData()
            return sharedData.getLocalAsyncMap("auth")
        }


    fun getRandomString(length: Int): String {
        val str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sb = StringBuilder()
        for (i in 0 until length) {
            val number = ThreadLocalRandom.current().nextInt(62)
            sb.append(str[number])
        }
        return sb.toString()
    }

}