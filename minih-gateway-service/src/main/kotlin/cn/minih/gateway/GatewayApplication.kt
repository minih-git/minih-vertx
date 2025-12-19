package cn.minih.gateway

import cn.minih.core.annotation.ComponentScan
import cn.minih.core.boot.MinihBootServiceRun

@ComponentScan("cn.minih.gateway")
class GatewayApplication

suspend fun main(args: Array<String>) {
    MinihBootServiceRun.run(GatewayApplication::class, "-standalone", *args)
}