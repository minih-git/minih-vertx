plugins {
    kotlin("jvm") version "1.9.22"
    id("java-library")
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.noarg") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.22"
}


allprojects {

    group = "cn.minih"
    version = "2.0.0"


    apply {
        plugin("org.jetbrains.kotlin.plugin.noarg")
        plugin("org.jetbrains.kotlin.plugin.allopen")
        plugin("java-library")
        plugin("maven-publish")
        plugin("kotlin")
    }

    allOpen {
        annotation("cn.minih.core.annotation.Component")
        annotation("cn.minih.core.annotation.Configuration")
        annotation("cn.minih.core.annotation.Bean")
        annotation("cn.minih.core.annotation.Service")
    }
    noArg {
        annotation("cn.minih.core.annotation.NoArg")
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
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public") }
        mavenCentral()
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

}