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
    implementation(libs.kotlin.reflect)
    implementation(libs.minih.core)
    implementation(libs.logback)
    implementation(libs.gson)
    api(libs.vertx.web)


}