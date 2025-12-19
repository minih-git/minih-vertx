dependencies {
    api(libs.kotlin.reflect)
    api(libs.vertx.core)
    api(libs.vertx.config)
    api(libs.vertx.config.yaml)
    api(libs.vertx.kotlin.coroutines)
    api(libs.vertx.kotlin)
    api(libs.vertx.hazelcast)
    api(libs.buddy)

    implementation(libs.minih.common)


    implementation(libs.logging)
    implementation(libs.logback)
    implementation(libs.gson)

}
