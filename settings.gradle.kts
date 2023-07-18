rootProject.name = "minih-vertx"


dependencyResolutionManagement {

    versionCatalogs {
        create("libs") {
            version("vertx", "4.4.4")
            library("vertx-core", "io.vertx", "vertx-core").versionRef("vertx")
            library("vertx-web", "io.vertx", "vertx-web").versionRef("vertx")
            library("vertx-kotlin", "io.vertx", "vertx-lang-kotlin").versionRef("vertx")
            library("vertx-kotlin-coroutines", "io.vertx", "vertx-lang-kotlin-coroutines").versionRef("vertx")
            library("vertx-config", "io.vertx", "vertx-config").versionRef("vertx")
            library("vertx-config-yaml", "io.vertx", "vertx-config-yaml").versionRef("vertx")
            library("vertx-service-proxy", "io.vertx", "vertx-service-proxy").versionRef("vertx")
            library("vertx-mongo", "io.vertx", "vertx-mongo-client").versionRef("vertx")
            library("vertx-redis", "io.vertx", "vertx-redis-client").versionRef("vertx")

            library("logging", "io.github.oshai:kotlin-logging-jvm:4.0.0")
            library("logback", "ch.qos.logback:logback-classic:1.4.8")

            library("gson", "com.google.code.gson:gson:2.10.1")
            library("hutool", "cn.hutool:hutool-core:5.8.20")
            library("jackson", "com.fasterxml.jackson.core:jackson-databind:2.15.2")
            library("bcrypt", "org.mindrot:jbcrypt:0.4")
            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:1.9.0")


        }
    }
}

include("minih-vertx-system","minih-vertx-auth", "minih-vertx-core")
