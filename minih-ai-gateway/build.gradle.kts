dependencies {
    api(libs.vertx.core)
    api(libs.vertx.web)
    api(libs.vertx.kotlin.coroutines)
    
    implementation(project(":minih-core"))
    implementation(project(":minih-common"))
    
    implementation(libs.kotlin.reflect)
    
    implementation(libs.logging)
    implementation(libs.logback)
}
