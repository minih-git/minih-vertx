@file:Suppress("KotlinConstantConditions")

package cn.minih.core.boot

import cn.minih.common.util.*
import cn.minih.core.annotation.*
import cn.minih.core.beans.BeanDefinitionBuilder
import cn.minih.core.beans.BeanFactory
import cn.minih.core.config.MAX_INSTANCE_COUNT
import cn.minih.core.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.core.eventbus.EventBusConsumer
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.*
import io.vertx.core.impl.ContextInternal
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions


/**
 * @author hubin
 * @date 2023/7/18
 * @desc
 */
@Suppress("unused")
object MinihBootServiceRun {

    private val systemConfigs = mutableListOf<ConfigStoreOptions>()
    private val successDeploy = mutableListOf<String>()
    private val allClazzList = mutableListOf<KClass<*>>()


    private fun initFunctionBean(clazz: KClass<*>, hostName: String) {
        clazz.memberFunctions.forEach { fn ->
            fn.findAnnotation<Bean>()?.let { anno ->
                val beanClazz = fn.returnType.classifier as KClass<*>
                var beanName = beanClazz.simpleName!!
                if (anno.value.isNotBlank()) {
                    beanName = anno.value
                }
                val beanDefinition = BeanDefinitionBuilder().build(beanClazz, beanName)
                BeanFactory.instance.registerBeanDefinition(beanName, beanDefinition)
                val bean = BeanFactory.instance.getBean(hostName)
                val realArgs = fn.parameters.subList(1, fn.parameters.size)
                if (realArgs.isEmpty()) {
                    fn.call(bean)?.let { BeanFactory.instance.registerBean(beanName, it) }
                } else {
                    val p = fn.parameters.map { p -> p.name?.let { BeanFactory.instance.getBean(it) } }
                    fn.call(bean, p.toTypedArray())?.let { BeanFactory.instance.registerBean(beanName, it) }
                }
            }
        }
    }

    private fun initBean(clazz: KClass<*>) {
        allClazzList.addAll(getClassesByPath("cn.minih"))
        if (clazz.hasAnnotation<ComponentScan>()) {
            val componentScan = clazz.findAnnotation<ComponentScan>()
            if (componentScan != null && componentScan.basePackage.isNotBlank()) {
                allClazzList.addAll(getClassesByPath(componentScan.basePackage))
            }
        }
        val components = allClazzList.filter { hasComponentsAnnotation(it) }
        components.forEach {
            var beanName = it.simpleName ?: ""
            it.findAnnotation<Component>()?.let { con ->
                con.value.notNullAndExec { beanName = con.value }
            }
            it.findAnnotation<Service>()?.let { con ->
                con.value.notNullAndExec { beanName = con.value }
            }
            val beanDefinition = BeanDefinitionBuilder().build(it, beanName)
            BeanFactory.instance.registerBeanDefinition(beanName, beanDefinition)
            initFunctionBean(it, beanName)
        }
    }

    private suspend fun deployVerticle(vertx: Vertx): Future<Boolean> {
        val services = BeanFactory.instance.findBeanDefinitionByAnnotation(MinihServiceVerticle::class)
        val config = initConfig(vertx)
        val options = DeploymentOptions().setConfig(config)
        return Future.all(
            services.map {
                val annoOptions =
                    it.annotations.first { an -> an.annotationClass == MinihServiceVerticle::class } as MinihServiceVerticle
                var instance = annoOptions.instance
                if (instance >= MAX_INSTANCE_COUNT) {
                    instance = MAX_INSTANCE_COUNT
                }
                vertx.deployVerticle(it.clazz.createType().toString(), options.setInstances(instance)).compose { re ->
                    successDeploy.add(re)
                    log.info("[${it.beanName}]服务部署成功,实例数量：$instance")
                    postDeployHandling(vertx, re)
                    Future.succeededFuture(true)
                }
            }
        ).compose { Future.succeededFuture(true) }


    }

    private fun replenishInitBean(vertx: Vertx): Future<Boolean> {
        val initBeanProcess = BeanFactory.instance.findBeanDefinitionByType(ReplenishInitBeanProcess::class)
        return Future.all(initBeanProcess.map { process ->
            val promise = Promise.promise<Boolean>()
            val bean = BeanFactory.instance.getBean(process.beanName) as ReplenishInitBeanProcess
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch {
                bean.exec(vertx.orCreateContext, allClazzList)
                promise.complete()
            }
            promise.future()
        }).compose { Future.succeededFuture(true) }
    }

    private fun deployEventBusConsumers(vertx: Vertx) {
        val consumers = BeanFactory.instance.findBeanDefinitionByType(EventBusConsumer::class)
        consumers.forEach { consumer ->
            val bean = BeanFactory.instance.getBean(consumer.beanName) as EventBusConsumer
            vertx.eventBus().consumer<JsonObject>(bean.channel).handler { obj ->
                CoroutineScope(vertx.orCreateContext.dispatcher()).launch { bean.exec(obj.body()) }
            }
        }
    }

    private fun preStartHandling(vertx: Vertx): Future<Boolean> {
        val startingProcess = BeanFactory.instance.findBeanDefinitionByType(PreStartingProcess::class)
        return Future.all(startingProcess.map { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PreStartingProcess
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch { bean.exec(vertx.orCreateContext) }
            Future.succeededFuture(true)
        }).compose { Future.succeededFuture(true) }
    }

    private fun postStartHandling(vertx: Vertx): Future<Boolean> {
        val startingProcess = BeanFactory.instance.findBeanDefinitionByType(PostStartingProcess::class)
        return Future.all(startingProcess.map { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PostStartingProcess
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch { bean.exec(vertx.orCreateContext) }
            Future.succeededFuture(true)
        }).compose { Future.succeededFuture(true) }
    }

    private fun postDeployHandling(vertx: Vertx, deployId: String) {
        val startingProcess = BeanFactory.instance.findBeanDefinitionByType(PostDeployingProcess::class)
        startingProcess.forEach { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PostDeployingProcess
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch {
                bean.exec(
                    vertx.orCreateContext,
                    deployId
                )
            }
        }
    }

    private fun registerCloseHandling(context: Context): Future<Boolean> {
        log.info("开始执行关闭程序...")
        val stopProcess = BeanFactory.instance.findBeanDefinitionByType(PreStopProcess::class)
        return Future.all(stopProcess.map { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PreStopProcess
            val future: Promise<Boolean> = Promise.promise()
            CoroutineScope(context.dispatcher()).launch {
                bean.exec(context)
            }.invokeOnCompletion {
                log.info("${process.beanName} 完成...")
                future.complete()
            }
            future.future()
        }).compose { context.owner().close();Future.succeededFuture() }
    }

    private suspend fun getVertx(vararg args: String): Vertx {
        val options = VertxOptions().setMetricsOptions(
            MicrometerMetricsOptions()
                .setPrometheusOptions(VertxPrometheusOptions().setEnabled(true))
                .setJvmMetricsEnabled(true)
                .setEnabled(true)
        )
        return when {
            args.isEmpty() -> Vertx.clusteredVertx(options).coAwait()
            args[0] == "-standalone" -> Vertx.vertx(options)
            else -> Vertx.clusteredVertx(options).coAwait()
        }
    }

    suspend fun run(clazz: KClass<*>, vararg args: String): Vertx {
        val vertx = getVertx(*args)
        try {
            log.info("服务开始启动...")
            val currentTime = System.currentTimeMillis()
            initBean(clazz)
            replenishInitBean(vertx).coAwait()
            deployEventBusConsumers(vertx)
            preStartHandling(vertx).coAwait()
            deployVerticle(vertx).coAwait()
            postStartHandling(vertx).coAwait()
            shutdownHook(vertx)
            log(vertx, currentTime)
        } catch (e: Throwable) {
            successDeploy.forEach { vertx.undeploy(it) }
            registerCloseHandling(vertx.orCreateContext)
            log.warn("部署服务出现错误,{}", e.message, e)
        }
        return vertx
    }

    private suspend fun log(vertx: Vertx, startTime: Long) {
        val projectName = getProjectName(vertx.orCreateContext)
        val shareData = vertx.sharedData().getAsyncMap<String, Int>("share-$projectName")
        val port = shareData.coAwait().get("port").coAwait()
        var msg = "${projectName}服务启动成功,当前环境：${getEnv()},"
        port?.let { msg = msg.plus("端口:${it},") }
        var bd = BigDecimal((System.currentTimeMillis() - startTime) / 1000.00)
        bd = bd.setScale(2, RoundingMode.HALF_UP)
        log.info(msg.plus("耗时: {}s"), bd.toString())
    }

    private fun shutdownHook(vertx: Vertx) {
        val context = vertx.orCreateContext as ContextInternal
        Runtime.getRuntime().addShutdownHook(Thread() {
            val a = registerCloseHandling(context)
            @Suppress("ControlFlowWithEmptyBody")
            while (!a.isComplete) {
            }
        })
    }

    fun setSystemConfigs(fn: () -> List<ConfigStoreOptions>): MinihBootServiceRun {
        systemConfigs.addAll(fn())
        return this
    }

    fun setSystemConfig(fn: () -> ConfigStoreOptions): MinihBootServiceRun {
        systemConfigs.add(fn())
        return this
    }

    private suspend fun initConfig(vertx: Vertx): JsonObject {

        val retrieverOptions = ConfigRetrieverOptions().setIncludeDefaultStores(true)

        if (systemConfigs.isNotEmpty()) {
            systemConfigs.forEach { retrieverOptions.addStore(it) }
        }
        val retriever = ConfigRetriever.create(vertx, retrieverOptions)
        vertx.eventBus().consumer<JsonObject>(SYSTEM_CONFIGURATION_SUBSCRIBE).handler { it1 ->
            it1.body().forEach {
                Vertx.currentContext().config().put(it.key, it.value)
            }
        }
        val config = retriever.config.coAwait()
        val configCopy = config.copy()
        configCopy.forEach {
            if (it.key.contains(".")) {
                updateJson(config, it.key.split("."), it.value.toString())
            }
        }
        val vertxConfig = vertx.orCreateContext.config()
        config.forEach {
            vertxConfig.put(it.key, it.value)
        }
        return config
    }

    private fun updateJson(jsonObj: JsonObject, keys: List<String>, newValue: String) {
        val key = keys[0]
        if (keys.size == 1) {
            jsonObj.put(key, newValue)
        } else {
            val nextObj = jsonObj.getValue(key) ?: JsonObject().also { jsonObj.put(key, it) }
            if (nextObj is JsonObject) {
                updateJson(nextObj, keys.drop(1), newValue)
            }
        }
    }

    private fun hasComponentsAnnotation(clazz: KClass<*>): Boolean {
        try {
            return clazz.hasAnnotation<Component>() ||
                    clazz.hasAnnotation<MinihServiceVerticle>() ||
                    clazz.hasAnnotation<Service>() ||
                    clazz.hasAnnotation<Configuration>()
        } catch (_: UnsupportedOperationException) {
        }
        return false
    }

}