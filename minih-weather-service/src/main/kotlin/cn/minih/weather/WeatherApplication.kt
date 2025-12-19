package cn.minih.weather

import cn.minih.core.annotation.ComponentScan
import cn.minih.core.boot.MinihBootServiceRun

@ComponentScan("cn.minih.weather")
class WeatherApplication

suspend fun main(args: Array<String>) {
    MinihBootServiceRun.run(WeatherApplication::class, "-standalone",*args)
}