plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "cn.minih"
version = "1.0-SNAPSHOT"

repositories {

    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public") }
    mavenCentral()

}
val vertxVersion = "4.4.4"

dependencies {
    //vertx
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-mongo-client:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-redis-client:$vertxVersion")
    implementation("io.vertx:vertx-auth-common:$vertxVersion")
    implementation("io.vertx:vertx-config:$vertxVersion")
    implementation("io.vertx:vertx-config-yaml:$vertxVersion")
    implementation("io.vertx:vertx-service-proxy:$vertxVersion")
    testImplementation("io.vertx:vertx-junit5:$vertxVersion")

    //log
    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0")
    implementation("ch.qos.logback:logback-classic:1.4.8")

    //utils
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("cn.hutool:hutool-core:5.8.20")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")


    //测试
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    implementation("org.mindrot:jbcrypt:0.4")



}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
        )
    }
}
