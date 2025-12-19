# minih-vertx 开发框架

![license](https://img.shields.io/badge/license-Apache--2.0-green)
![stars](https://img.shields.io/github/stars/minih-git/minih-vertx)
![maven-central](https://img.shields.io/github/v/release/minih-git/minih-vertx)

基于 Vert.x 和 Kotlin 协程的轻量级微服务开发框架。

## 功能特性

### 核心功能

- 🚀 快速启动 Verticle
- ⚙️ 自动配置与装配
- 💉 依赖注入
- 🔐 鉴权管理
- 💾 缓存管理
- 📝 注解路由

### 微服务

- 📡 服务注册与发现
- 🔗 服务间调用
- 💗 心跳检测与自动下线
- 🧠 语义注册中心（AI Tool 路由）

### 中间件

- 📨 RocketMQ 封装
- 🗄️ 数据库支持

## 模块说明

| 模块 | 说明 |
|------|------|
| `minih-core` | 核心启动、生命周期、Bean 管理 |
| `minih-common` | 通用工具类、注解 |
| `minih-web` | Web 路由、请求处理 |
| `minih-auth` | 认证鉴权 |
| `minih-cache` | 缓存管理 |
| `minih-database` | 数据库支持 |
| `minih-microservice-client` | 微服务客户端、服务发现 |
| `minih-semantic-registry` | 语义注册中心（HNSW 向量索引） |
| `minih-ai-gateway` | AI 网关、SSE 支持 |
| `minih-rocketmq` | RocketMQ 封装 |

## 快速开始

### 1. 最简启动

```kotlin
@ComponentScan("com.minih")
class SystemMain

suspend fun main(vararg args: String) {
    MinihBootServiceRun.run(SystemMain::class, *args)
}
```

### 2. 配置文件

```kotlin
suspend fun main(vararg args: String) {
    MinihBootServiceRun.setSystemConfigs {
        listOf(
            // Consul 配置中心
            ConfigStoreOptions().setType("http").setFormat("yaml")
                .setConfig(
                    JsonObject()
                        .put("host", "localhost").put("port", 8500)
                        .put("path", "/v1/kv/app.yaml?raw=true")
                ),
            // 本地配置文件
            ConfigStoreOptions().setType("file").setFormat("yaml")
                .setConfig(JsonObject().put("path", "app.yaml"))
        )
    }.run(SystemMain::class, *args)
}
```

### 3. Verticle 配置

```kotlin
@MinihServiceVerticle(instance = 8)
class CustomerVerticle : MinihWebVerticle(8081) {

    override suspend fun initRouterHandler() {
        register(IService::class)
    }
}
```

### 4. AI Tool 注解

```kotlin
interface IUserService {
    @AiTool("获取用户列表")
    @Get("/list")
    suspend fun listUsers(): List<User>
}
```

## 语义注册中心

支持基于语义的服务发现，使用 HNSW 向量索引实现 AI Tool 路由。

### 特性

- 🔍 语义搜索：根据自然语言描述查找服务
- 💗 心跳检测：30秒心跳间隔，90秒 TTL 超时自动下线
- 📊 向量索引：基于 ONNX Embedding + HNSW 算法

### API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/semantic/api/register` | POST | 注册服务 |
| `/semantic/api/unregister` | POST | 注销服务 |
| `/semantic/api/search` | POST | 语义搜索 |
| `/semantic/api/heartbeat` | POST | 心跳更新 |

## License

[Apache-2.0](LICENSE.txt)
