package cn.minih.core.components

import cn.minih.core.annotation.Component
import cn.minih.core.annotation.ComponentScan
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.core.beans.BeanDefinitionBuilder
import cn.minih.core.beans.BeanFactory
import cn.minih.core.constants.MAX_INSTANCE_COUNT
import cn.minih.core.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.core.handler.BeforeDeployHandler
import cn.minih.core.handler.EventBusConsumer
import cn.minih.core.repository.RepositoryManager
import cn.minih.core.utils.*
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
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
object MinihServiceRun {

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun run(clazz: KClass<*>) {
        val vertx = Vertx.vertx()
        val componentsList = getClassesByPath("cn.minih")
        if (clazz.hasAnnotation<ComponentScan>()) {
            val componentScan = clazz.findAnnotation<ComponentScan>()
            if (componentScan != null && componentScan.basePacakge.isNotBlank()) {
                componentsList.addAll(getClassesByPath(componentScan.basePacakge))
            }
        }
        componentsList.forEach {
            if (hasComponentsAnnotation(it) && it.simpleName != null) {
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

        val beforeDeployHandler = BeanFactory.instance.findBeanDefinitionByType(BeforeDeployHandler::class)
        beforeDeployHandler.forEach {
            val bean = BeanFactory.instance.getBean(it.beanName) as BeforeDeployHandler
            bean.exec(vertx)
        }
        val services = BeanFactory.instance.findBeanDefinitionByAnnotation(MinihServiceVerticle::class)
        val config = initConfig(vertx)
        val options = DeploymentOptions().setConfig(config)
        vertx.eventBus().consumer<JsonObject>(SYSTEM_CONFIGURATION_SUBSCRIBE).handler { it1 ->
            it1.body().forEach {
                Vertx.currentContext().config().put(it.key, it.value)
            }
        }
        val consumers = BeanFactory.instance.findBeanDefinitionByType(EventBusConsumer::class)
        consumers.forEach { consumer ->
            val bean = BeanFactory.instance.getBean(consumer.beanName) as EventBusConsumer
            vertx.eventBus().consumer<JsonObject>(bean.channel).handler { obj ->
                GlobalScope.launch(Vertx.currentContext().dispatcher()) { bean.exec(obj.body()) }
            }
        }
        RepositoryManager.initDb(vertx, getConfig().mysql)
        services.forEach {
            val annoOptions =
                it.annotations.first { an -> an.annotationClass == MinihServiceVerticle::class } as MinihServiceVerticle
            var instance = annoOptions.instance
            if (instance >= MAX_INSTANCE_COUNT) {
                instance = MAX_INSTANCE_COUNT
            }
            vertx.deployVerticle(it.clazz.createType().toString(), options.setInstances(instance)).onComplete { re ->
                if (re.succeeded()) {
                    log.info("[${it.beanName}]服务部署成功,实例数量：$instance")
                }
            }
        }
    }

    private suspend fun initConfig(vertx: Vertx): JsonObject {
        val retriever = ConfigRetriever.create(
            vertx, ConfigRetrieverOptions()
                .addStore(ConfigStoreOptions().setType("env").setFormat("json"))
                .addStore(
                    ConfigStoreOptions().setType("file").setFormat("yaml")
                        .setConfig(JsonObject().put("path", "app.yaml"))
                )
        )
        val config = retriever.config.await()
        config.put("minih.core.aesSecret", generateAesSecret())
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
