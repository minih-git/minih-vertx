plugins {
    kotlin("jvm") version "1.9.0"
    id("java-library")
    id("maven-publish")
}

allprojects {
    group = "cn.minih"
    version = "1.0.1"
    repositories {
        mavenLocal()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public") }
        mavenCentral()
    }

}