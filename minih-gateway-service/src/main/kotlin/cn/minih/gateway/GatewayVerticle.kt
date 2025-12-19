package cn.minih.gateway

import cn.minih.auth.core.MinihAuthVerticle
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.gateway.controller.SemanticController
import cn.minih.web.core.MinihWebVerticle
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.ext.web.client.WebClient

@MinihServiceVerticle
class GatewayVerticle : MinihAuthVerticle(8080) { // Gateway runs on 8080

    override suspend fun initRouterHandler() {
        val client = WebClient.create(vertx)
        router.route()
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())
        // Forward to System Service
        // Assuming System Service runs on 8081 (Needs config)
        router.route("/system/*").handler { ctx ->
             // Proxy logic here
             // Simplified forward
             val path = ctx.request().path().replace("/system", "")
             client.request(ctx.request().method(), 8081, "localhost", path)
                .sendBuffer(ctx.body().buffer() ?: io.vertx.core.buffer.Buffer.buffer())
                .onSuccess { res ->
                    ctx.response().setStatusCode(res.statusCode()).end(res.body())
                }
                .onFailure { err ->
                    ctx.fail(500, err)
                }
        }

        // Forward to Weather Service
        // Assuming Weather Service runs on 8082
        router.route("/weather/*").handler { ctx ->
             val path = ctx.request().path().replace("/weather", "")
             client.request(ctx.request().method(), 8082, "localhost", path)
                .sendBuffer(ctx.body().buffer() ?: io.vertx.core.buffer.Buffer.buffer())
                .onSuccess { res ->
                    ctx.response().setStatusCode(res.statusCode()).end(res.body())
                }
                .onFailure { err ->
                    ctx.fail(500, err)
                }

        }

        register(SemanticController::class)

    }
}