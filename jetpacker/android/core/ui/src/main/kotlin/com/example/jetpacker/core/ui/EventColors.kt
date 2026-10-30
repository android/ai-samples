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
  val Flight = EventColor(container = Color(0xFF102A4C), content = Color(0xFF60A5FA))
  val Hotel = EventColor(container = Color(0xFF0D333B), content = Color(0xFF26C6DA))
  val Activity = EventColor(container = Color(0xFF0B3820), content = Color(0xFF3DDC84))
  val Museum = EventColor(container = Color(0xFF12362C), content = Color(0xFF38D9A9))
  val Food = EventColor(container = Color(0xFF0A3622), content = Color(0xFF01AF57))
  val Shopping = EventColor(container = Color(0xFF3E1F38), content = Color(0xFFFDADEE))
  val Entertainment = EventColor(container = Color(0xFF241B4B), content = Color(0xFFA78BFA))
}
