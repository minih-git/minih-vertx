import com.github.gradle.node.yarn.task.YarnTask

plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.node-gradle.node") version "5.0.0"
}

tasks.register<YarnTask>("yarnInstall") {
}

tasks.register<YarnTask>("yarnBuild") {
    dependsOn("yarnInstall")
    args.addAll("run", "build")
}
