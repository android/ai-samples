/*
 * Copyright 2024 Google LLC. All rights reserved.
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

package com.google.ai.edge.aicore.demo

import android.content.Context
import androidx.preference.PreferenceManager

object GenerationConfigUtils {
  @JvmStatic
  fun getTemperature(context: Context): Float {
    return PreferenceManager.getDefaultSharedPreferences(context)
      .getFloat(context.getString(R.string.pref_key_temperature), 0.2f)
  }

  @JvmStatic
  fun setTemperature(context: Context, temperature: Float) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit()
      .putFloat(context.getString(R.string.pref_key_temperature), temperature)
      .apply()
  }

  @JvmStatic
  fun getTopK(context: Context): Int {
    return PreferenceManager.getDefaultSharedPreferences(context)
      .getInt(context.getString(R.string.pref_key_top_k), 16)
  }

  @JvmStatic
  fun setTopK(context: Context, topK: Int) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit()
      .putInt(context.getString(R.string.pref_key_top_k), topK)
      .apply()
  }

  @JvmStatic
  fun getMaxOutputTokens(context: Context): Int {
    return PreferenceManager.getDefaultSharedPreferences(context)
      .getInt(context.getString(R.string.pref_key_max_output_tokens), 256)
  }

  @JvmStatic
  fun setMaxOutputTokens(context: Context, maxTokenCount: Int) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit()
      .putInt(context.getString(R.string.pref_key_max_output_tokens), maxTokenCount)
      .apply()
  }
}
