plugins {
    kotlin("jvm") version "1.9.0"
    id("java-library")
}

dependencies {
    api(libs.vertx.core)
    api(libs.vertx.web)
    api(libs.vertx.config)

    api(libs.vertx.kotlin)
    api(libs.vertx.kotlin.coroutines)

    api(libs.vertx.mysql)
    api(libs.vertx.redis)


    api(libs.kotlin.reflect)


    api(libs.hutool)
    api(libs.gson)
    api(libs.guava)
    api(libs.logback)
    api(libs.logging)

    implementation(libs.smsapi) {
        exclude(group = "pull-parser")
    }

}
