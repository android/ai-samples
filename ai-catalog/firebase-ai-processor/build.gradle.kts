plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":annotations"))
    implementation(libs.symbol.processing.api)
    implementation("com.squareup:kotlinpoet-ksp:2.2.0")

    testImplementation(libs.junit)
    testImplementation("org.assertj:assertj-core:3.27.4")
    testImplementation("dev.zacsweers.kctfork:ksp:0.8.0")

}