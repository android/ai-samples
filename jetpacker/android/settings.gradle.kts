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

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "JetPacker"

include(":app")

include(":core:flags")

include(":core:speech")

include(":core:ui")

include(":data:db")

include(":data:itinerary")

include(":data:trips")

include(":feature:create_trip")

include(":feature:detail")
include(":feature:detail:museum_assistant")
include(":feature:detail:hotel_chat")
include(":feature:detail:review")

include(":feature:home")

include(":feature:trip")

include(":feature:trip:itinerary")

include(":feature:trip:expenses")

include(":feature:trip:voice_notes")

include(":feature:trip:itinerary:enrichment")
include(":feature:appfunctions")
