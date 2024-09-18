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

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.widget.EditText
import com.google.ai.edge.aicore.demo.GenerationConfigUtils.getMaxOutputTokens
import com.google.ai.edge.aicore.demo.GenerationConfigUtils.getTemperature
import com.google.ai.edge.aicore.demo.GenerationConfigUtils.getTopK
import com.google.ai.edge.aicore.demo.GenerationConfigUtils.setMaxOutputTokens
import com.google.ai.edge.aicore.demo.GenerationConfigUtils.setTemperature
import com.google.ai.edge.aicore.demo.GenerationConfigUtils.setTopK

class GenerationConfigDialog : DialogFragment() {
  interface OnConfigUpdateListener {
    fun onConfigUpdated()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val activity: Activity = requireActivity()
    val builder = AlertDialog.Builder(activity)

    val view = layoutInflater.inflate(R.layout.dialog_generation_config, null)
    val temperatureEditText = view.findViewById<EditText>(R.id.temperature_edit_text)
    temperatureEditText.setText(getTemperature(activity).toString())
    val topKEditText = view.findViewById<EditText>(R.id.top_k_edit_text)
    topKEditText.setText(getTopK(activity).toString())
    val maxOutputTokensEditText = view.findViewById<EditText>(R.id.max_output_tokens_edit_text)
    maxOutputTokensEditText.setText(getMaxOutputTokens(activity).toString())

    builder
      .setView(view)
      .setPositiveButton(R.string.button_save) { _: DialogInterface, _: Int ->
        setTemperature(activity, temperatureEditText.text.toString().toFloat())
        setTopK(activity, topKEditText.text.toString().toInt())
        setMaxOutputTokens(activity, maxOutputTokensEditText.text.toString().toInt())
        if (activity is OnConfigUpdateListener) {
          (activity as OnConfigUpdateListener).onConfigUpdated()
        }
      }
      .setNegativeButton(R.string.button_cancel, null)
    return builder.create()
  }
}
