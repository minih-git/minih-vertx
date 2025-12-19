package cn.minih.weather.controller

import cn.minih.common.annotation.AiTool

import cn.minih.core.annotation.Service
import cn.minih.core.beans.BeanFactory
import cn.minih.web.annotation.Get
import cn.minih.web.annotation.RequestMapping
import cn.minih.weather.service.WeatherService
import cn.minih.web.annotation.Request

@Request("/weather")
interface WeatherController : cn.minih.web.service.Service {

    @Get("/query")
    @AiTool("Get weather by city name")
    suspend fun query(city: String): String
}

@Service("weatherController")
class WeatherControllerImpl : WeatherController {
    private val weatherService: WeatherService
        get() = BeanFactory.instance.getBean("weatherService") as WeatherService

    override suspend fun query(city: String): String {
        return weatherService.getWeather(city)
    }
}