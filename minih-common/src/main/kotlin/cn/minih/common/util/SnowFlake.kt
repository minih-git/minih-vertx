@file:Suppress("unused")

package cn.minih.common.util

import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong

const val sp = 1585644268888L
const val businessIdBits = 5L
const val datacenterIdBits = 5L
const val sequenceBits = 12L
const val businessIdShift = sequenceBits
const val datacenterIdShift = sequenceBits + businessIdBits
const val timestampLeftShift = sequenceBits + businessIdBits + datacenterIdBits
const val sequenceMask = -1L xor (-1L shl sequenceBits.toInt())

class SnowFlake(bid: String) {

    companion object {
        fun getBusinessId(id: Long): Long {
            return (id ushr 12) and (-1L shl 5).inv()
        }

        fun getTimeStamp(id: Long): Long {
            return (id ushr 22) + sp
        }

        fun getDateCenter(id: Long): Long {
            return (id ushr 17) and (-1L shl 5).inv()
        }
    }

    private var businessId: Long
    private val datacenterId = getCenterId(InetAddress.getLocalHost().hostAddress)
    private var sequence = AtomicLong(0)

    private var lastTimestamp = AtomicLong(-1L)

    init {
        businessId = getBusinessId(bid)
    }

    @Synchronized
    fun nextId(bid: Long = 0L): Long {
        if (bid != 0L) {
            businessId = bid
        }
        var timestamp: Long = System.currentTimeMillis()
        if (timestamp < lastTimestamp.get()) {
            log.error { "系统时间不正确" }
            throw RuntimeException("系统时间不正确")
        }
        if (lastTimestamp.get() == timestamp) {
            sequence.set((sequence.incrementAndGet() and sequenceMask))
            if (sequence.get() == 0L) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp.get())
            }

        } else {
            sequence.set(0)
        }
        lastTimestamp.set(timestamp)
        return timestamp - sp shl timestampLeftShift.toInt() or
                (datacenterId shl datacenterIdShift.toInt()) or
                (businessId shl businessIdShift.toInt()) or sequence.get()
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
                (s[2].toLong() shl 8) + s[3].toLong())
    }

    private fun getBusinessId(businessId: String): Long {
        return (businessId.hashCode() and Int.MAX_VALUE).toLong() % 31L
    }
}