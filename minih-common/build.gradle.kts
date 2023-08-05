plugins {
    kotlin("jvm") version "1.9.0"
    id("java-library")
    id("maven-publish")

}
java {
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
        maven {
            url = uri("https://packages.aliyun.com/maven/repository/2405309-snapshot-BRUb5T/")
            credentials {
                username = "64ccfc96d7cc86cfb4a243ac"
                password = "Kzd4TVR-FiJU"
            }
        }
    }
}

dependencies {

    implementation(libs.logging)
    implementation(libs.logback)
    implementation(libs.gson)
    api(libs.kotlin.reflect)
    api(libs.vertx.core)
    api(libs.vertx.kotlin)
    api(libs.vertx.kotlin.coroutines)
}