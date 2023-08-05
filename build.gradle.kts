plugins {
    kotlin("jvm") version "1.9.0"
    id("java-library")
    id("maven-publish")
}

allprojects {
    group = "cn.minih"
    version = "1.0.0"
    repositories {
        maven {
            url = uri("https://packages.aliyun.com/maven/repository/2405309-snapshot-BRUb5T/")
            credentials {
                username = "64ccfc96d7cc86cfb4a243ac"
                password = "Kzd4TVR-FiJU"
            }
        }
        mavenLocal()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public") }
        mavenCentral()
    }

}