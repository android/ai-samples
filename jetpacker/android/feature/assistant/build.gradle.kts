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
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.jetpacker.feature.assistant"
  compileSdk {
    version =
      release(libs.versions.compileSdk.get().toInt()) {
        minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
      }
  }
  defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    buildConfig = true
  }
}

dependencies {
  implementation(project(":core:speech"))
  implementation(project(":core:ui"))
  implementation(project(":data:db"))
  implementation(project(":data:itinerary"))
  implementation(project(":data:trips"))
  implementation(project(":feature:appfunctions"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appfunctions)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)

  implementation(libs.hilt.android)
  implementation(libs.androidx.hilt.navigation.compose)
  "ksp"(libs.hilt.compiler)

  implementation(libs.mlkit.genai.agent)
  implementation(libs.mlkit.genai.common)
  implementation(libs.mlkit.genai.embedding)
  implementation(libs.mlkit.genai.prompt)
  implementation(libs.mlkit.genai.schema)
  implementation(libs.androidx.appsearch)
  implementation(libs.androidx.appsearch.local.storage)
  implementation(libs.androidx.concurrent.futures.ktx)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.junit)
  testImplementation(libs.google.truth)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation("org.json:json:20240303")
}

kotlin { jvmToolchain(17) }
