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
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.android.compose.screenshot)
  alias(libs.plugins.hilt.android)
}

android {
  namespace = "com.example.jetpacker"
  compileSdk {
    version = release(libs.versions.compileSdk.get().toInt()) {
      minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
    }
  }

  defaultConfig {
    applicationId = "com.example.jetpacker"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  experimentalProperties["android.experimental.enableScreenshotTest"] = true
  buildFeatures {
    compose = true
    buildConfig = true
  }
  packaging {
    resources {
      excludes += "META-INF/DEPENDENCIES"
      excludes += "META-INF/LICENSE"
      excludes += "META-INF/LICENSE.txt"
      excludes += "META-INF/license.txt"
      excludes += "META-INF/NOTICE"
      excludes += "META-INF/NOTICE.txt"
      excludes += "META-INF/notice.txt"
      excludes += "META-INF/ASL2.0"
      excludes += "META-INF/INDEX.LIST"
    }
  }
}

kotlin { jvmToolchain(17) }

hilt { enableAggregatingTask = false }

dependencies {
  implementation(platform(libs.androidx.compose.bom))

  implementation(project(":core:flags"))
  implementation(project(":core:ui"))
  implementation(project(":data:db"))
  implementation(project(":data:itinerary"))
  implementation(project(":data:trips"))
  implementation(project(":feature:create_trip"))
  implementation(project(":feature:detail"))
  implementation(project(":feature:home"))
  implementation(project(":feature:trip"))
  implementation(project(":feature:trip:itinerary"))

  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.datastore.core)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  "ksp"(libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.hilt.android)
  "ksp"(libs.hilt.compiler)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.logging.interceptor)
  implementation(libs.material)
  implementation(libs.moshi.kotlin)
  "ksp"(libs.moshi.kotlin.codegen)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.retrofit)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)

  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  androidTestImplementation(platform(libs.androidx.compose.bom))

  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
