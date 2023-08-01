rootProject.name = "minih-vertx"


dependencyResolutionManagement {

    versionCatalogs {
        create("libs") {
            version("vertx", "4.4.4")
            version("minih-vertx", "1.0.0")
            library("vertx-core", "io.vertx", "vertx-core").versionRef("vertx")
            library("vertx-web", "io.vertx", "vertx-web").versionRef("vertx")
            library("vertx-kotlin", "io.vertx", "vertx-lang-kotlin").versionRef("vertx")
            library("vertx-kotlin-coroutines", "io.vertx", "vertx-lang-kotlin-coroutines").versionRef("vertx")
            library("vertx-config", "io.vertx", "vertx-config").versionRef("vertx")
            library("vertx-config-yaml", "io.vertx", "vertx-config-yaml").versionRef("vertx")
            library("vertx-service-proxy", "io.vertx", "vertx-service-proxy").versionRef("vertx")
            library("vertx-mysql", "io.vertx", "vertx-mysql-client").versionRef("vertx")
            library("vertx-redis", "io.vertx", "vertx-redis-client").versionRef("vertx")
            library("vertx-discovery", "io.vertx", "vertx-service-discovery").versionRef("vertx")
            library("vertx-web-client", "io.vertx", "vertx-web-client").versionRef("vertx")
            library("vertx-hazelcast", "io.vertx", "vertx-hazelcast").versionRef("vertx")
            library("vertx-consul", "io.vertx", "vertx-consul-client").versionRef("vertx")
            library("vertx-discovery-bridge", "io.vertx", "vertx-service-discovery-bridge-consul").versionRef("vertx")
            library("vertx-discovery-backend", "io.vertx", "vertx-service-discovery-backend-consul").versionRef("vertx")

            library("logging", "io.github.oshai:kotlin-logging-jvm:4.0.0")
            library("logback", "ch.qos.logback:logback-classic:1.4.8")

            library("gson", "com.google.code.gson:gson:2.10.1")
            library("guava", "com.google.guava:guava:32.1.1-jre")
            library("hutool", "cn.hutool:hutool-core:5.8.20")
            library("jackson", "com.fasterxml.jackson.core:jackson-databind:2.15.2")
            library("bcrypt", "org.mindrot:jbcrypt:0.4")
            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:1.9.0")

            library("minih-core", "cn.minih", "minih-core").versionRef("minih-vertx")
            library("minih-web", "cn.minih", "minih-web").versionRef("minih-vertx")
            library("minih-auth", "cn.minih", "minih-auth").versionRef("minih-vertx")
            library("minih-mysql", "cn.minih", "minih-database-mysql").versionRef("minih-vertx")


        }
    }
}


include("minih-core")
include("minih-database-mysql")
include("minih-web")
include("minih-auth")
include("minih-microservice-client")
