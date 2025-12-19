plugins {
    application
}

dependencies {
    implementation(libs.minih.core)
    implementation(libs.minih.common)
    implementation(libs.minih.web)
    implementation(libs.minih.auth)
    implementation(project(":minih-microservice-client"))

    implementation(libs.logging)
    implementation(libs.logback)
}

application {
    mainClass.set("cn.minih.weather.WeatherApplicationKt")
}