package cn.minih.core.components

import cn.minih.core.annotation.Component
import cn.minih.core.annotation.ComponentScan
import cn.minih.core.annotation.MinihServiceVerticle
import cn.minih.core.beans.BeanDefinition
import cn.minih.core.beans.BeanFactory
import cn.minih.core.constants.MAX_INSTANCE_COUNT
import cn.minih.core.constants.SYSTEM_CONFIGURATION_SUBSCRIBE
import cn.minih.core.utils.getClassesByPath
import cn.minih.core.utils.log
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
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
                val beanDefinition = BeanDefinition(it, it.createType(), it.annotations, beanName)
                BeanFactory.instance.registerBeanDefinition(beanName, beanDefinition)
            }
        }
        val services = BeanFactory.instance.findBeanDefinitionByAnnotation(MinihServiceVerticle::class)
        val config = initConfig(vertx)
        val options = DeploymentOptions().setConfig(config)
        vertx.eventBus().consumer<JsonObject>(SYSTEM_CONFIGURATION_SUBSCRIBE).handler { it1 ->
            it1.body().forEach {
                Vertx.currentContext().config().put(it.key, it.value)
            }
        }

        services.forEach {
            val annoOptions =
                it.annotations.first { an -> an.annotationClass == MinihServiceVerticle::class } as MinihServiceVerticle
            var instance = annoOptions.instance
            if (instance >= MAX_INSTANCE_COUNT) {
                instance = MAX_INSTANCE_COUNT
            }
            vertx.deployVerticle(it.clazz.createType().toString(), options.setInstances(instance)) { re ->
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
            if (it.key.contains(".")) {
                val key = it.key.substring(0, it.key.indexOf("."))
                val subKey = it.key.substring(it.key.indexOf(".") + 1)
                val value = it.value
                val map = config.getJsonObject(key)
                map.put(subKey, value)
                config.put(key, map)
            }
            config.put(it.key, it.value)
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