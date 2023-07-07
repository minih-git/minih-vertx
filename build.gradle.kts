plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "cn.minih"
version = "1.0-SNAPSHOT"

repositories {

    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
    maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public") }

}

dependencies {
    implementation("io.vertx:vertx-core:4.4.4")
    implementation("io.vertx:vertx-web:4.4.4")
    implementation("io.vertx:vertx-mongo-client:4.4.4")
    implementation("io.vertx:vertx-lang-kotlin:4.4.4")
//    implementation("io.vertx:vertx-auth-common:4.4.4")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.4.4")
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}
