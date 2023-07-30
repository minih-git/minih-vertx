package cn.minih.core.boot

import cn.minih.core.annotation.Component
import cn.minih.core.annotation.ComponentScan
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.core.beans.BeanDefinitionBuilder
import cn.minih.core.beans.BeanFactory
import cn.minih.core.constants.MAX_INSTANCE_COUNT
import cn.minih.core.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.core.eventbus.EventBusConsumer
import cn.minih.core.utils.Utils.getClassesByPath
import cn.minih.core.utils.covertTo
import cn.minih.core.utils.log
import cn.minih.core.utils.toJsonObject
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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


    private fun initBean(clazz: KClass<*>) {
        val componentsList = getClassesByPath("cn.minih")
        if (clazz.hasAnnotation<ComponentScan>()) {
            val componentScan = clazz.findAnnotation<ComponentScan>()
            if (componentScan != null && componentScan.basePackage.isNotBlank()) {
                componentsList.addAll(getClassesByPath(componentScan.basePackage))
            }
        }
        componentsList.filter { hasComponentsAnnotation(it) }.forEach {
            if (it.simpleName != null) {
                var beanName = it.simpleName!!
                clazz.findAnnotation<Component>()?.let { con ->
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
                    Future.succeededFuture(true)
                }
            }
        ).compose { Future.succeededFuture(true) }


    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun deployEventBusConsumers(vertx: Vertx) {
        val consumers = BeanFactory.instance.findBeanDefinitionByType(EventBusConsumer::class)
        consumers.forEach { consumer ->
            val bean = BeanFactory.instance.getBean(consumer.beanName) as EventBusConsumer
            vertx.eventBus().consumer<JsonObject>(bean.channel).handler { obj ->
                GlobalScope.launch(Vertx.currentContext().dispatcher()) { bean.exec(obj.body()) }
            }
        }
    }

    private fun postStartHandling(vertx: Vertx) {
        val startingProcess = BeanFactory.instance.findBeanDefinitionByType(PreStartingProcess::class)
        startingProcess.forEach { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PreStartingProcess
            bean.exec(vertx)
        }
    }

    private fun preStartHandling(vertx: Vertx) {
        val startingProcess = BeanFactory.instance.findBeanDefinitionByType(PostStartingProcess::class)
        startingProcess.forEach { process ->
            val bean = BeanFactory.instance.getBean(process.beanName) as PostStartingProcess
            bean.exec(vertx)
        }
    }


    suspend fun run(clazz: KClass<*>) {
        val vertx = Vertx.vertx()
        try {
            log.info("服务开始启动...")
            val currentTime = System.currentTimeMillis()
            initBean(clazz)
            deployEventBusConsumers(vertx)
            preStartHandling(vertx)
            deployVerticle(vertx).await()
            postStartHandling(vertx)
            log.info(
                "服务启动成功，耗时:{}s",
                String.format("%.2f", ((System.currentTimeMillis() - currentTime) / 1000))
            )
        } catch (e: Exception) {
            successDeploy.forEach {
                vertx.undeploy(it)
            }
            log.warn("部署服务出现错误,{}", e.message, e)
        }

    }

    fun setSystemConfig(fn: () -> List<ConfigStoreOptions>) {
        systemConfigs.addAll(fn())
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
        val obj = config.covertTo(com.google.gson.JsonObject::class)
        config.forEach {
            if (it.key.contains(".")) {
                updateJson(obj, it.key.split("."), it.value.toString())
            }
        }
        obj.entrySet().forEach {
            val vertxConfig = Vertx.currentContext().config()
            vertxConfig.put(it.key, it.value.toString())
            if (it.value is com.google.gson.JsonObject) {
                vertxConfig.put(it.key, it.value.toJsonObject())
            }
        }
        return obj.toJsonObject()

    }

    private fun updateJson(jsonObj: com.google.gson.JsonObject, keys: List<String>, newValue: String) {
        val key = keys[0]
        if (keys.size == 1) {
            jsonObj.addProperty(key, newValue)
        } else {
            val nextObj = jsonObj.getAsJsonObject(key) ?: com.google.gson.JsonObject().also { jsonObj.add(key, it) }
            updateJson(nextObj, keys.drop(1), newValue)
        }
    }


    private fun hasComponentsAnnotation(clazz: KClass<*>): Boolean {
        try {
            return clazz.hasAnnotation<Component>() || clazz.hasAnnotation<MinihServiceVerticle>()
        } catch (_: UnsupportedOperationException) {
        }
        return false
    }

}