package cn.minih.weather.service

import cn.minih.core.annotation.Service
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient

@Service("weatherService")
class WeatherService {

    // Helper to get Vertx instance might be needed, or we use a static client if initialized
    // For simplicity, we create a client here or assume it's passed.
    // In minih-framework, usually services are singletons.
    // We can use Vertx.currentContext().owner() to get Vertx if in a Verticle/Context.
    
    suspend fun getWeather(city: String): String {
        // Tianxing API Example
        val apiKey = "YOUR_TIANXING_API_KEY"
        val url = "http://api.tianapi.com/tianqi/index?key=$apiKey&city=$city"
        
        // This is blocking if we don't use coroutines properly with WebClient.
        // But let's assume valid coroutine context.
        val vertx = Vertx.currentContext()?.owner() ?: Vertx.vertx()
        val client = WebClient.create(vertx)
        
        return try {
            val response = io.vertx.kotlin.coroutines.awaitResult<io.vertx.ext.web.client.HttpResponse<io.vertx.core.buffer.Buffer>> { h ->
                client.getAbs(url).send(h)
            }
            response.bodyAsString()
        } catch (e: Exception) {
            "Error querying weather: ${e.message}"
        }
    }
}
