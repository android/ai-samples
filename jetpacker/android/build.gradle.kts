/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.android.compose.screenshot) apply false
  alias(libs.plugins.hilt.android) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.dependency.license.report)
}

subprojects {
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
      freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
  }
  tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
  }
  configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
  }
}

licenseReport {
  outputDir = layout.buildDirectory.dir("reports/dependency-license").get().asFile.absolutePath
  projects = subprojects.toTypedArray()
  renderers = arrayOf(com.github.jk1.license.render.JsonReportRenderer("licenses.json"))
}

tasks.register<Exec>("generateThirdPartyNotices") {
  dependsOn("generateLicenseReport")
  commandLine("./gradle/scripts/generate_notices.py")
}

project(":app") {
  afterEvaluate {
    tasks.named("preBuild") {
      dependsOn(rootProject.tasks.named("generateThirdPartyNotices"))
    }
  }
}




