plugins {
    kotlin("jvm") version "1.9.0"
}
allprojects {
    group = "cn.minih"
    version = "1.0.0"
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public") }
        mavenCentral()
    }

}
