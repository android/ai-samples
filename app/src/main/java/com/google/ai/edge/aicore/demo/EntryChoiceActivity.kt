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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.ai.edge.aicore.DownloadCallback
import com.google.ai.edge.aicore.DownloadConfig
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.demo.java.MainActivity
import com.google.ai.edge.aicore.generationConfig
import java.lang.String.format
import java.util.Locale
import kotlinx.coroutines.launch

class EntryChoiceActivity : AppCompatActivity() {

  private var modelDownloaded = false
  private var model: GenerativeModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_entry_choice)

    findViewById<TextView>(R.id.kotlin_entry_point).setOnClickListener {
      if (modelDownloaded) {
        val intent =
          Intent(
            this@EntryChoiceActivity,
            com.google.ai.edge.aicore.demo.kotlin.MainActivity::class.java,
          )
        startActivity(intent)
      } else {
        Toast.makeText(this, R.string.model_unavailable, Toast.LENGTH_SHORT).show()
      }
    }

    findViewById<TextView>(R.id.java_entry_point).setOnClickListener {
      if (modelDownloaded) {
        val intent = Intent(this@EntryChoiceActivity, MainActivity::class.java)
        startActivity(intent)
      } else {
        Toast.makeText(this, R.string.model_unavailable, Toast.LENGTH_SHORT).show()
      }
    }

    ensureModelDownloaded()
  }

  private fun ensureModelDownloaded() {
    val downloadProgressTextView = findViewById<TextView>(R.id.download_progress_text_view)
    var totalBytesToDownload = 0L
    val downloadConfig =
      DownloadConfig(
        object : DownloadCallback {
          override fun onDownloadStarted(bytesToDownload: Long) {
            totalBytesToDownload = bytesToDownload
          }

          override fun onDownloadFailed(failureStatus: String, e: GenerativeAIException) {
            Log.e(TAG, "Failed to download model.", e)
          }

          override fun onDownloadProgress(totalBytesDownloaded: Long) {
            if (totalBytesToDownload > 0) {
              downloadProgressTextView?.visibility = View.VISIBLE
              downloadProgressTextView?.text =
                format(
                  Locale.ENGLISH,
                  "Downloading model:  %d / %d MB (%.2f%%)",
                  totalBytesDownloaded / MEGABYTE,
                  totalBytesToDownload / MEGABYTE,
                  100.0 * totalBytesDownloaded / totalBytesToDownload,
                )
            }
          }

          override fun onDownloadCompleted() {
            modelDownloaded = true
          }
        }
      )

    model = GenerativeModel(generationConfig { context = applicationContext }, downloadConfig)
    lifecycleScope.launch {
      try {
        model?.prepareInferenceEngine()
      } catch (e: GenerativeAIException) {
        Log.e(TAG, "Failed to check model availability.", e)
        Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    model?.close()
  }

  companion object {
    private const val TAG = "EntryChoiceActivity"
    private const val MEGABYTE = 1024 * 1024
  }
}
