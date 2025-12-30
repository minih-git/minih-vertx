plugins {
    application
}

dependencies {
    implementation(libs.minih.core)
    implementation(libs.minih.common)
    implementation(libs.minih.web)
    implementation(libs.minih.auth)
    implementation(libs.minih.mysql)
    implementation(project(":minih-microservice-client"))

    implementation(libs.logging)
    implementation(libs.logback)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.jackson)
}

application {
    mainClass.set("cn.minih.system.SystemApplicationKt")
}