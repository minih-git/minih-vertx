plugins {
    kotlin("jvm") version "1.9.0"
    application
}

dependencies {

    implementation(project(":minih-vertx-core"))
    implementation(project(":minih-vertx-auth"))
    implementation(libs.bcrypt)
    implementation(libs.vertx.config.yaml)

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

}


application {
    mainClass.set("MainKt")
}
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
        )
    }
}