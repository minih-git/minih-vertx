@file:Suppress("unused")

package cn.minih.core.utils

import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostDeployingProcess
import io.vertx.core.Vertx
import java.net.InetAddress

private const val sp = 1585644268888L
private const val workerIdBits = 5L
private const val datacenterIdBits = 5L
private const val sequenceBits = 12L
private const val workerIdShift = sequenceBits
private const val datacenterIdShift = sequenceBits + workerIdBits
private const val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits
private const val sequenceMask = -1L xor (-1L shl sequenceBits.toInt())

/**
 * 雪花算法实现
 * @author hubin
 * @since 2023-07-30 22:33:13
 * @desc
 */
@Suppress("unused")
@Component
class InitSnowFlake : PostDeployingProcess {
    override suspend fun exec(vertx: Vertx, deployId: String) {
        SnowFlakeContext.instance.putContext(deployId, SnowFlake(deployId))

    }
}

class SnowFlakeContext {
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SnowFlakeContext() }
    }

    private val context = mutableMapOf<String, SnowFlake>()

    fun putContext(dId: String, sk: SnowFlake) {
        context[dId] = sk
    }

    fun currentContext(): SnowFlake {
        return context[Vertx.currentContext().deploymentID()] ?: SnowFlake("1")
    }


}


class SnowFlake(wid: String) {
    private val workerId: Long
    private val datacenterId = getCenterId(InetAddress.getLocalHost().hostAddress)
    private var sequence = 0

    private var lastTimestamp = -1L

    init {
        workerId = getWorkId(wid)
    }

    @Synchronized
    fun nextId(): Long {
        var timestamp: Long = System.currentTimeMillis()
        if (timestamp < lastTimestamp) {
            log.error { "系统时间不正确" }
            throw RuntimeException("系统时间不正确")
        }
        if (lastTimestamp == timestamp) {
            sequence = ((sequence + 1).toLong() and sequenceMask).toInt()
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp)
            }

        } else {
            sequence = 0
        }
        lastTimestamp = timestamp
        return timestamp - sp shl timestampLeftShift.toInt() or
                (datacenterId shl datacenterIdShift.toInt()) or
                (workerId shl workerIdShift.toInt()) or sequence.toLong()
    }

    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp: Long = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }

    private fun getCenterId(ip: String): Long {
        val s: List<String> = ip.split(".")
        return ((s[0].toLong() shl 24)
                + (s[1].toLong() shl 16) +
                (0 shl 8) + 0)


    }

    private fun getWorkId(deployId: String): Long {
        return (deployId.hashCode() and Int.MAX_VALUE).toLong() shl 31
    }

}
