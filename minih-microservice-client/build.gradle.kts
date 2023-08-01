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
    }
}

dependencies {

    api(libs.vertx.discovery)
    api(libs.vertx.consul)
//    api(libs.vertx.discovery.backend)
    api(libs.vertx.web.client)

    implementation(libs.logging)
    implementation(libs.logback)
    implementation(libs.gson)
    implementation(libs.minih.core)

}
