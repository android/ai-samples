/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ai.common

enum class Model(val id: String, val displayName: String) {
    GeminiFlash("gemini-2.5-flash", "Gemini 2.5 Flash"),
    GeminiFlashImage("gemini-2.5-flash-image", "Gemini 2.5 Flash Image"),
    GeminiFlashLive("gemini-2.0-flash-live-preview-04-09", "Gemini 2.0 Flash Live"),
    Imagen("imagen-4.0-generate-001", "Imagen 4.0"),
    ImagenEditing("imagen-3.0-capability-001", "Imagen 3.0 Editing"),
}
