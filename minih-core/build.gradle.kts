plugins {
    kotlin("jvm") version "1.9.0"
    id("java-library")
    id("maven-publish")

}


java{
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("minihLibrary") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}

dependencies {
    api(libs.kotlin.reflect)
    api(libs.vertx.core)
    api(libs.vertx.config)
    api(libs.vertx.config.yaml)
    api(libs.vertx.kotlin.coroutines)
    api(libs.vertx.kotlin)
    api(libs.vertx.hazelcast)


    implementation(libs.logging)
    implementation(libs.logback)
    implementation(libs.gson)

}
