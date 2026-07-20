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

package com.example.jetpacker.core.ui

import androidx.compose.ui.graphics.Color

data class EventColor(val container: Color, val content: Color)

object EventColors {
  val Flight = EventColor(container = Color(0xFFF3E5F5), content = Color(0xFF8523C2))
  val Hotel = EventColor(container = Color(0xFFE3F2FD), content = Color(0xFF1976D2))
  val Activity = EventColor(container = Color(0xFFFFF3E0), content = Color(0xFFE65100))
  val Museum = EventColor(container = Color(0xFFFFF3E0), content = Color(0xFF8BC34A))
  val Food = EventColor(container = Color(0xFFE8F5E9), content = Color(0xFF2E7D32))
  val Shopping = EventColor(container = Color(0xFFFCE4EC), content = Color(0xFFC2185B))
  val Entertainment = EventColor(container = Color(0xFFFFFDE7), content = Color(0xFFFBC02D))
}
