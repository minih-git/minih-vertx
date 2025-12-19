plugins {
    application
}
dependencies {
    api(libs.vertx.core)
    api(libs.vertx.web)
    api(libs.vertx.kotlin.coroutines)

    implementation(libs.minih.core)
    implementation(libs.minih.common)

    implementation(libs.onnxruntime)
    implementation(libs.hnswlib)

    implementation(libs.logging)
    implementation(libs.logback)

    implementation("ai.djl:api:0.29.0")
    implementation("ai.djl.huggingface:tokenizers:0.29.0")
}

application {
    mainClass.set("cn.minih.semantic.SemanticRegistryApplicationKt")
}