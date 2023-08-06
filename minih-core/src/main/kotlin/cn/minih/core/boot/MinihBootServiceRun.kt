@file:Suppress("KotlinConstantConditions")

package cn.minih.core.boot

import cn.minih.common.util.getClassesByPath
import cn.minih.common.util.getEnv
import cn.minih.common.util.log
import cn.minih.core.annotation.Component
import cn.minih.core.annotation.ComponentScan
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.core.annotation.Service
import cn.minih.core.beans.BeanDefinitionBuilder
import cn.minih.core.beans.BeanFactory
import cn.minih.core.config.MAX_INSTANCE_COUNT
import cn.minih.core.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.core.eventbus.EventBusConsumer
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation


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
            if (it.simpleName != null) {
                var beanName = it.simpleName!!
                it.findAnnotation<Component>()?.let { con ->
                    if (con.value.isNotBlank()) {
                        beanName = con.value
                    }
                }
                it.findAnnotation<Service>()?.let { con ->
                    if (con.value.isNotBlank()) {
                        beanName = con.value
                    }
                }
                val beanDefinition = BeanDefinitionBuilder().build(it, beanName)
                BeanFactory.instance.registerBeanDefinition(beanName, beanDefinition)
            }
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
                bean.exec(vertx, allClazzList);
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

    private fun preStartHandling(vertx: Vertx) {
        val startingProcess = BeanFactory.instance.findBeanDefinitionByType(PreStartingProcess::class)
        startingProcess.forEach { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PreStartingProcess
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch { bean.exec(vertx) }
        }
    }

    private fun postStartHandling(vertx: Vertx) {
        val startingProcess = BeanFactory.instance.findBeanDefinitionByType(PostStartingProcess::class)
        startingProcess.forEach { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PostStartingProcess
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch { bean.exec(vertx) }
        }
    }

    private fun postDeployHandling(vertx: Vertx, deployId: String) {
        val startingProcess = BeanFactory.instance.findBeanDefinitionByType(PostDeployingProcess::class)
        startingProcess.forEach { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PostDeployingProcess
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch { bean.exec(vertx, deployId) }
        }
    }

    private fun registerCloseHandling(vertx: Vertx): Future<Boolean> {
        log.info("开始执行关闭程序...")
        val stopProcess = BeanFactory.instance.findBeanDefinitionByType(PreStopProcess::class)
        return Future.all(stopProcess.map { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PreStopProcess
            val future: Promise<Boolean> = Promise.promise()
            CoroutineScope(vertx.orCreateContext.dispatcher()).launch {
                bean.exec(vertx)
            }.invokeOnCompletion {
                log.info("${process.beanName} 完成...")
                future.complete()
            }
            future.future()
        }).compose { vertx.close();Future.succeededFuture() }
    }


    suspend fun run(clazz: KClass<*>, vararg args: String) {
        val vertx = when {
            args.isEmpty() -> Vertx.vertx()
            args[0] == "-standalone" -> Vertx.vertx()
            else -> {
                val mgr = HazelcastClusterManager()
                val options = VertxOptions().setClusterManager(mgr)
                Vertx.clusteredVertx(options).await()
            }
        }
        try {
            log.info("服务开始启动...")
            val currentTime = System.currentTimeMillis()
            initBean(clazz)
            replenishInitBean(vertx)
            deployEventBusConsumers(vertx)
            preStartHandling(vertx)
            deployVerticle(vertx).await()
            postStartHandling(vertx)
            Runtime.getRuntime().addShutdownHook(Thread() {
                val a = registerCloseHandling(vertx)
                @Suppress("ControlFlowWithEmptyBody")
                while (!a.isComplete) {
                }
            })
            val shareData = vertx.sharedData().getAsyncMap<String, Int>("share")
            val port = shareData.await().get("port").await()
            var msg = "服务启动成功,当前环境：${getEnv()},"
            port?.let {
                msg = msg.plus("端口:${it},")
            }
            var bd = BigDecimal((System.currentTimeMillis() - currentTime) / 1000.00)
            bd = bd.setScale(2, RoundingMode.HALF_UP)
            log.info(msg.plus("耗时: {}s"), bd.toString())
        } catch (e: Exception) {
            successDeploy.forEach {
                vertx.undeploy(it)
            }
            registerCloseHandling(vertx)
            log.warn("部署服务出现错误,{}", e.message, e)
        }

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
        val config = retriever.config.await()
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
            return clazz.hasAnnotation<Component>() || clazz.hasAnnotation<MinihServiceVerticle>() || clazz.hasAnnotation<Service>()
        } catch (_: UnsupportedOperationException) {
        }
        return false
    }


}