# minih-vertx开发包

![license](https://img.shields.io/badge/license-Apache--2.0-green)
![stars](https://img.shields.io/github/stars/minih-git/minih-vertx)
![maven-central](https://img.shields.io/github/v/release/minih-git/minih-vertx?include_prereleases)

### 有什么功能？

* [快速启动verticle]
* 自动配置
* 自动装配
* 依赖注入
* 缓存管理
* 鉴权管理
* rocketmq封装
* 注解路由
* 微服务客户端
* 服务注册
* 服务间调用
* 更多...

### 快速启动

```kotlin
@ComponentScan("com.tuoling")
class SystemMain

suspend fun main(vararg args: String) {
    MinihBootServiceRun.run(SystemMain::class, *args)
}
```

### 增加配置文件

```kotlin
@ComponentScan("com.tuoling")
class SystemMain

suspend fun main(vararg args: String) {
    MinihBootServiceRun.setSystemConfigs {
        listOf(
            //使用consul配置中心
            ConfigStoreOptions().setType("http").setFormat("yaml")
                .setConfig(
                    JsonObject()
                        .put("host", "localhost").put("port", 8500)
                        .put("path", "/v1/kv/app.yaml?raw=true")
                ),
            //项目resource下配置文件app.yaml
            ConfigStoreOptions().setType("file").setFormat("yaml").setConfig(JsonObject().put("path", "app.yaml"))
        )
    }.run(SystemMain::class, *args)
}
```

###Verticle配置

`1、简单配置，实现MinihWebVerticle类，注册路由处理器即可`

```kotlin
/**
 * 标注是一个Verticle，启动会自动扫描该注解
 * @instance 项目启动实例数量
 * @8001 端口号
 */
@MinihServiceVerticle(instance = 8)
class CustomerVerticle : MinihWebVerticle(8081) {

    /**
     * 注册路由处理器
     */
    override suspend fun initRouterHandler() {
        register(IService::class)
    }

}
```

`2、加上鉴权控制，实现MinihAuthVerticle类`

```kotlin
/**
 * 标注是一个Verticle，启动会自动扫描该注解
 * @instance 项目启动实例数量
 * @8001 端口号
 */
@MinihServiceVerticle(instance = 8)
class CustomerVerticle : MinihAuthVerticle(8001) {

    /**
     * 注册路由处理器
     * 可以手动匹配path，也可以通过register将接口注入
     */
    override suspend fun initRouterHandler() {
        router.route()
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())
        router.get("/options").coroutineHandler { it.json(R.ok("success")) }
        register(IService::class)
    }

}
```

### 更多使用方式请阅读源码...
