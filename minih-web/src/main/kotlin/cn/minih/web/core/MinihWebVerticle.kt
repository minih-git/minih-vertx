@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.minih.web.core

import cn.minih.common.util.getConfig
import cn.minih.common.util.getProjectName
import cn.minih.common.util.notNullAndExec
import cn.minih.core.boot.MinihVerticle
import cn.minih.web.config.WebConfig
import cn.minih.web.handler.RouteFailureHandler
import cn.minih.web.service.Service
import io.vertx.core.Vertx
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod

/**
 * web服务启动器
 * @author hubin
 * @since 2023-07-30 22:08:04
 * @desc
 */
@Suppress("unused")
abstract class MinihWebVerticle(private val port: Int = 8080) : MinihVerticle, CoroutineVerticle() {

    val router: Router
        get() {
            return this.routerInstance
        }
    private lateinit var routerInstance: Router

    private val serviceList = mutableListOf<KClass<*>>()

    fun initRouter() {
        this.routerInstance = Router.router(vertx)
        val sockJSHandler = SockJSHandler.create(vertx)
        val options = SockJSBridgeOptions()
        options.addInboundPermitted(PermittedOptions().setAddressRegex("cn.minih.*"))
        options.addOutboundPermitted(PermittedOptions().setAddressRegex("cn.minih.*"))
        var bodyHandler = BodyHandler.create().setDeleteUploadedFilesOnEnd(true)
        getConfig("web", WebConfig::class).tmpFilePath.notNullAndExec {
            bodyHandler = bodyHandler.setUploadsDirectory(it).setDeleteUploadedFilesOnEnd(false)
        }
        routerInstance.route()
            .handler(ResponseContentTypeHandler.create())
            .handler(bodyHandler)
            .failureHandler(RouteFailureHandler.instance)
        routerInstance.route("/healthCheck").handler { it.end("ok") }
        routerInstance.route("/ws/minihEventbus/*").subRouter(sockJSHandler.bridge(options))
    }


    override suspend fun start() {
        initRouterHandler()
        RegisterService.registerService(serviceList, vertx) {
            if (it.second == null) {
                registerRouterHandler(router.route(it.first), it.third)
            } else {
                registerRouterHandler(router.route(it.second, it.first), it.third)

            }
        }
        val server = vertx.createHttpServer()
        val projectName = getProjectName(vertx.orCreateContext)
        val shareData = vertx.sharedData().getAsyncMap<String, Int>("share-$projectName")
        shareData.coAwait().put("port", port).coAwait()
        server.requestHandler(routerInstance).listen(port)
    }

    open fun registerRouterHandler(route: Route, fn: KFunction<Any?>) {
        route.coroutineHandler(fn)
    }

    abstract suspend fun initRouterHandler()

    fun <T : Service> register(clazz: KClass<T>): MinihWebVerticle {
        this.serviceList.add(clazz)
        return this
    }

}

fun Route.coroutineHandler(fn: KFunction<Any?>) {
    handler { ctx ->
        val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
        val job = CoroutineScope(v).launch {
            try {
                // 1. 获取 Bean 实例
                val bean = getBeanCall(fn.parameters) ?: throw RuntimeException("Cannot find bean for function ${fn.name}")

                // 2. 准备参数
                val paramsJson = JsonObject()
                ctx.request().params().forEach { paramsJson.put(it.key, it.value) }
                try {
                    if (ctx.body() != null && ctx.body().length() > 0) {
                        val checkJson = ctx.body().asString().trim()
                        if (checkJson.startsWith("{") && checkJson.endsWith("}")) {
                             paramsJson.mergeIn(ctx.bodyAsJson)
                        }
                    }
                } catch (e: Exception) { /* ignore body parse error */ }

                // 排除 instance 参数 (第一个通常是 bean 实例)
                val argsNeed = fn.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
                val argsList = cn.minih.common.util.generateArgs(argsNeed, paramsJson)
                val argsMap = argsList.toMap().toMutableMap()
                
                // 将 bean 实例放入参数 map
                val instanceParam = fn.parameters.find { it.kind == kotlin.reflect.KParameter.Kind.INSTANCE }
                if (instanceParam != null) {
                    argsMap[instanceParam] = bean
                }

                // 3. 调用方法
                // 构造有序参数列表
                val orderedArgs = ArrayList<Any?>()
                fn.valueParameters.forEach { param ->
                     orderedArgs.add(argsMap[param])
                }
                
                // 处理可变参数等复杂情况比较麻烦，暂且假设没有 varargs
                val argsArray = orderedArgs.toArray()
                
                // 3. 调用方法
                // 构造有序参数列表
                val orderedArgs = ArrayList<Any?>()
                fn.valueParameters.forEach { param ->
                     orderedArgs.add(argsMap[param])
                }
                val argsArray = orderedArgs.toArray()

                // 修复: 使用 Java Reflection + Continuation 直接调用，解决 Proxy 和 Kotlin Reflection 的兼容问题
                val javaMethod = fn.javaMethod
                val result = if (fn.isSuspend && javaMethod != null) {
                    kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Any?> { cont ->
                        val fullArgs = arrayOfNulls<Any?>(argsArray.size + 1)
                        System.arraycopy(argsArray, 0, fullArgs, 0, argsArray.size)
                        fullArgs[fullArgs.size - 1] = cont
                        try {
                            javaMethod.invoke(bean, *fullArgs)
                        } catch (e: java.lang.reflect.InvocationTargetException) {
                            throw e.targetException
                        }
                    }
                } else {
                    fn.call(bean, *argsArray)
                }

                // 4. 处理返回值
                if (result is kotlinx.coroutines.flow.Flow<*>) {
                    ctx.response()
                        .putHeader("Content-Type", "text/event-stream")
                        .putHeader("Cache-Control", "no-cache")
                        .putHeader("Connection", "keep-alive")
                        .setChunked(true)

                    try {
                        result.collect { item ->
                            val data = if (item is String) item else cn.minih.common.util.getGson().toJson(item)
                            ctx.response().write("data: $data\n\n")
                        }
                        // Flow 结束，不用主动 end? 或者发送结束标识？
                        // 通常 SSE 保持连接，直到客户端断开或服务端不再发送。
                        // 如果 Flow 结束了，可以 end request
                        if (!ctx.response().ended()) {
                            ctx.response().end() 
                        }
                    } catch (e: Exception) {
                        // CancellationException 会在这里捕获
                        if (!ctx.response().ended()) {
                           ctx.response().end()
                        }
                        throw e
                    }
                } else {
                    if (!ctx.response().ended()) {
                         if (result == null) {
                             ctx.end()
                         } else {
                             ctx.json(result)
                         }
                    }
                }

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                   ctx.fail(e)
                }
            }
        }
        
        // 5. 处理连接断开
        ctx.response().closeHandler {
            job.cancel(kotlinx.coroutines.CancellationException("Client disconnected"))
        }
    }
}

// 辅助方法：获取 Bean (Copy from RegisterService roughly or verify usage)
// 由于 getBeanCall 是 RegisterService 私有的，我们需要在这里实现一个简化版
// 或者依赖 BeanFactory
private fun getBeanCall(params: List<kotlin.reflect.KParameter>): Any? {
    if (params.isNotEmpty()) {
        val p1 = params.first()
        val type = p1.type
        val clazz = type.classifier as? KClass<*>
        if (clazz != null) {
             // 检查是否是 Service
             // 简单处理：尝试从 BeanFactory 获取
             try {
                 return cn.minih.core.beans.BeanFactory.instance.getBeanFromType(type)
             } catch (e: Exception) {
                 // ignore
             }
             // 尝试通过 Interface 获取
              try {
                 return cn.minih.core.beans.BeanFactory.instance.getBeanFromType(clazz.supertypes.first { it.classifier != Any::class })
             } catch (e: Exception) {
                 // ignore
             }
        }
    }
    return null
}