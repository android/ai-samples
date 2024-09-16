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
