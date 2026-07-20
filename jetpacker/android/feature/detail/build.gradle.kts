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
  alias(libs.plugins.android.compose.screenshot)
}

android {
  namespace = "com.example.jetpacker.feature.detail"
  compileSdk = libs.versions.compileSdk.get().toInt()
  defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  experimentalProperties["android.experimental.enableScreenshotTest"] = true
  buildFeatures { compose = true }
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
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.hilt.android)
  "ksp"(libs.hilt.compiler)

  debugImplementation(libs.androidx.compose.ui.tooling)

  screenshotTestImplementation(project(":data:trips"))
  screenshotTestImplementation(libs.androidx.compose.ui.tooling)
  screenshotTestImplementation(libs.screenshot.validation.api)
}

kotlin { jvmToolchain(17) }

screenshotTests {
  imageDifferenceThreshold = 0.05f
}
