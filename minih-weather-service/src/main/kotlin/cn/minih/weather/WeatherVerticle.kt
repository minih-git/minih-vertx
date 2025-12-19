package cn.minih.weather

import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.web.core.MinihWebVerticle
import cn.minih.weather.controller.WeatherController

@MinihServiceVerticle
class WeatherVerticle : MinihWebVerticle(8082) {
    override suspend fun initRouterHandler() {
        initRouter()
        register(WeatherController::class)
    }
}