dependencies {

    api(libs.vertx.discovery)
    api(libs.vertx.consul)
    api(libs.vertx.web.client)
    api(libs.vertx.kotlin.coroutines)



    implementation(libs.logging)
    implementation(libs.logback)
    implementation(libs.gson)
    implementation(libs.minih.core)
    implementation(libs.minih.common)


}