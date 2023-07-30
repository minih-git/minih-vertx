plugins {
    kotlin("jvm") version "1.9.0"
    id("java-library")
    id("maven-publish")

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
    api(libs.vertx.redis)
    implementation(libs.gson)
    implementation(libs.logging)
    implementation(libs.logback)


    implementation(libs.minih.core)
    implementation(libs.minih.web)
    implementation(libs.hutool)


}