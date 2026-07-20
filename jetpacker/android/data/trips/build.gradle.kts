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
}

android {
  namespace = "com.example.jetpacker.data.trips"
  compileSdk = libs.versions.compileSdk.get().toInt()
  defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
  implementation(project(":core:ui"))
  implementation(project(":data:itinerary"))

  implementation(libs.androidx.core.ktx)
  "ksp"(libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)
  implementation(libs.hilt.android)
  "ksp"(libs.hilt.compiler)
}

kotlin { jvmToolchain(17) }
