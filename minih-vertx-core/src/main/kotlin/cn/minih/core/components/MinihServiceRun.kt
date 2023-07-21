package cn.minih.core.components

import cn.minih.core.annotation.Component
import cn.minih.core.annotation.ComponentScan
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.core.beans.BeanDefinition
import cn.minih.core.beans.BeanDefinitionBuilder
import cn.minih.core.beans.BeanFactory
import cn.minih.core.constants.MAX_INSTANCE_COUNT
import cn.minih.core.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.core.handler.BeforeDeployHandler
import cn.minih.core.handler.EventBusConsumer
import cn.minih.core.utils.getClassesByPath
import cn.minih.core.utils.log
import cn.minih.core.utils.toJsonObject
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextInternal
import io.vertx.core.impl.VertxImpl
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.superclasses

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
        val config = JsonObject()
        retriever.config.await().forEach {
            val vertxConfig = Vertx.currentContext().config()
            if (it.key.contains(".")) {
                val key = it.key.substring(0, it.key.indexOf("."))
                val subKey = it.key.substring(it.key.indexOf(".") + 1)
                val value = it.value
                val map = config.getJsonObject(key, jsonObjectOf())
                map.put(subKey, value)
                config.put(key, map)
                vertxConfig.put(key, map)
            }
            config.put(it.key, it.value)
            vertxConfig.put(it.key, it.value)
        }
        return config

    }

    private fun hasComponentsAnnotation(clazz: KClass<*>): Boolean {
        try {
            return clazz.hasAnnotation<Component>() || clazz.hasAnnotation<MinihServiceVerticle>()
        } catch (_: UnsupportedOperationException) {
        }
        return false
    }

}
