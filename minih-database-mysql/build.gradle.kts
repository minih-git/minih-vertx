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
    api(libs.vertx.mysql)


    implementation(libs.gson)
    implementation(libs.logging)
    implementation(libs.logback)
    implementation(libs.guava)

    implementation(libs.minih.core)


}