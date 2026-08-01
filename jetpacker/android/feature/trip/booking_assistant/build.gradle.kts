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
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.example.jetpacker.feature.booking_assistant"
  compileSdk = libs.versions.compileSdk.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))

  implementation(project(":core:flags"))
  implementation(project(":core:ui"))
  implementation(project(":data:itinerary"))
  implementation(project(":data:trips"))

  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.hilt.android)
  "ksp"(libs.hilt.compiler)

  debugImplementation(libs.androidx.compose.ui.tooling)

  implementation(libs.google.adk)
  implementation(libs.google.adk.firebase)
  "ksp"(libs.google.adk.processor)
  implementation(libs.agui.client)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.kotlinx.serialization.json)

  implementation("androidx.a2ui:a2ui-engine:1.0.0-SNAPSHOT")
  implementation("androidx.a2ui:a2ui-model:1.0.0-SNAPSHOT")
  implementation("androidx.a2ui.compose:compose-runtime:1.0.0-SNAPSHOT")
  implementation("androidx.a2ui.compose:compose-ui:1.0.0-SNAPSHOT")


  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.google.truth)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
}

kotlin { jvmToolchain(21) }
