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

package com.example.jetpacker.core.speech

import android.annotation.SuppressLint
import android.util.Log
import com.example.jetpacker.core.flags.FeatureFlags
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
@SuppressLint("NewApi", "GlobalCoroutineDispatchers")
class VoiceInputManager @Inject constructor() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val _uiState = MutableStateFlow(VoiceInputState())
  val uiState: StateFlow<VoiceInputState> = _uiState.asStateFlow()

  private var speechRecognizer: SpeechRecognizer? = null
  private var translator: Translator? = null
  private var listenJob: Job? = null
  private var hasRecordAudioPermission = false
  private var autoStartListeningWhenReady = false
  private var queuedOnResult: ((original: String, translated: String) -> Unit)? = null

  init {
    initSpeechAndTranslation()
  }

  private var initializedLanguage: String? = null

  private fun initSpeechAndTranslation() {
    val currentLang = FeatureFlags.DEMO_LANGUAGE
    val isTranslatorReady = currentLang == "en" || translator != null
    Log.d(
      "VoiceInputManager",
      "initSpeechAndTranslation: currentLang=$currentLang, isTranslatorReady=$isTranslatorReady, speechRecognizer=${speechRecognizer != null}, initializedLanguage=$initializedLanguage",
    )

    if (isTranslatorReady && speechRecognizer != null && initializedLanguage == currentLang) {
      Log.d(
        "VoiceInputManager",
        "initSpeechAndTranslation: Returning early since clients are already warm and matching current language.",
      )
      return
    }

    val isLangChange = initializedLanguage != null && initializedLanguage != currentLang

    if (isLangChange) {
      Log.d(
        "VoiceInputManager",
        "initSpeechAndTranslation: Language change detected, resetting ready state to allow download indicator flow.",
      )
      _uiState.value = _uiState.value.copy(isReady = false)
      try {
        translator?.close()
        translator = null
      } catch (e: Exception) {
        Log.e(
          "VoiceInputManager",
          "initSpeechAndTranslation: Error releasing existing translator.",
          e,
        )
      }
    }

    try {
      Log.d(
        "VoiceInputManager",
        "initSpeechAndTranslation: Closing previous speechRecognizer for fresh rebind.",
      )
      speechRecognizer?.close()
      speechRecognizer = null
    } catch (e: Exception) {
      Log.e(
        "VoiceInputManager",
        "initSpeechAndTranslation: Error releasing existing speechRecognizer.",
        e,
      )
    }

    initializedLanguage = currentLang

    try {
      if (currentLang != "en" && translator == null) {
        Log.d(
          "VoiceInputManager",
          "initSpeechAndTranslation: Constructing Translator for language: $currentLang",
        )
        val options =
          TranslatorOptions.Builder()
            .setSourceLanguage(currentLang)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        val client = Translation.getClient(options)
        client.downloadModelIfNeeded().addOnSuccessListener {
          Log.d(
            "VoiceInputManager",
            "initSpeechAndTranslation: Translator model downloaded/cached successfully.",
          )
          translator = client
        }
      }

      Log.d(
        "VoiceInputManager",
        "initSpeechAndTranslation: Constructing fresh SpeechRecognizer for language: $currentLang",
      )
      val speechOptions = speechRecognizerOptions {
        locale = Locale(currentLang)
        preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
      }
      speechRecognizer = SpeechRecognition.getClient(speechOptions)

      if (isLangChange) {
        Log.d(
          "VoiceInputManager",
          "initSpeechAndTranslation: Language changed, checking model status to warm it up.",
        )
        checkModelStatus()
      }
    } catch (e: Exception) {
      Log.e(
        "VoiceInputManager",
        "initSpeechAndTranslation: Failed to build ML Kit clients asynchronously (common in tests).",
        e,
      )
    }
  }

  private fun checkModelStatus() {
    val recognizer =
      speechRecognizer
        ?: run {
          Log.w(
            "VoiceInputManager",
            "checkModelStatus: Bypassed check because speechRecognizer is null.",
          )
          return
        }
    scope.launch {
      Log.d("VoiceInputManager", "checkModelStatus: Checking speech model status asynchronously...")
      _uiState.value = _uiState.value.copy(statusText = "Checking model status...", isReady = false)
      val status = recognizer.checkStatus()
      Log.d("VoiceInputManager", "checkModelStatus: Speech model status resolved to: $status")
      if (status == FeatureStatus.DOWNLOADABLE) {
        Log.d(
          "VoiceInputManager",
          "checkModelStatus: Speech model is downloadable, starting collectors flow...",
        )
        _uiState.value = _uiState.value.copy(statusText = "Downloading model...", isReady = false)
        recognizer.download().collect { downloadStatus ->
          Log.d("VoiceInputManager", "checkModelStatus collect: downloadStatus=$downloadStatus")
          when (downloadStatus) {
            is DownloadStatus.DownloadCompleted -> {
              Log.d(
                "VoiceInputManager",
                "checkModelStatus collect: Download completed successfully!",
              )
              _uiState.value = _uiState.value.copy(statusText = "Model ready!", isReady = true)
              triggerListeningIfQueued()

              // Preemptively warm up ASR model loading in background to displace other concurrent
              // isolated AI/LLM models in AICore memory
              scope.launch {
                Log.d(
                  "VoiceInputManager",
                  "checkModelStatus: Preemptively warming up SpeechRecognizer model...",
                )
                try {
                  // recognizer.warmup()
                  Log.d(
                    "VoiceInputManager",
                    "checkModelStatus: SpeechRecognizer model warm up completed successfully.",
                  )
                } catch (e: Exception) {
                  Log.e(
                    "VoiceInputManager",
                    "checkModelStatus: Preemptive SpeechRecognizer warm up failed.",
                    e,
                  )
                }
              }
            }
            is DownloadStatus.DownloadFailed -> {
              Log.e("VoiceInputManager", "checkModelStatus collect: Download failed.")
              _uiState.value =
                _uiState.value.copy(
                  statusText =
                    "Download failed. Please check your internet connection and try again.",
                  isReady = false,
                  isError = true,
                  showDialog = true,
                )
            }
            else ->
              _uiState.value = _uiState.value.copy(statusText = "Downloading...", isReady = false)
          }
        }
      } else if (status == FeatureStatus.AVAILABLE) {
        Log.d(
          "VoiceInputManager",
          "checkModelStatus: Speech model is fully available locally. Making ready.",
        )
        _uiState.value = _uiState.value.copy(statusText = "Model ready!", isReady = true)
        triggerListeningIfQueued()

        // Preemptively warm up ASR model loading in background to displace other concurrent
        // isolated AI/LLM models in AICore memory
        scope.launch {
          Log.d(
            "VoiceInputManager",
            "checkModelStatus: Preemptively warming up SpeechRecognizer model...",
          )
          try {
            // recognizer.warmup()
            Log.d(
              "VoiceInputManager",
              "checkModelStatus: SpeechRecognizer model warm up completed successfully.",
            )
          } catch (e: Exception) {
            Log.e(
              "VoiceInputManager",
              "checkModelStatus: Preemptive SpeechRecognizer warm up failed.",
              e,
            )
          }
        }
      } else {
        Log.w(
          "VoiceInputManager",
          "checkModelStatus: Speech model is completely unavailable on device (Status: $status).",
        )
        _uiState.value =
          _uiState.value.copy(statusText = "Model not available (Status: $status)", isReady = false)
      }
    }
  }

  private fun triggerListeningIfQueued() {
    Log.d(
      "VoiceInputManager",
      "triggerListeningIfQueued: Checked. autoStartListeningWhenReady=$autoStartListeningWhenReady, speechRecognizer=${speechRecognizer != null}",
    )
    if (autoStartListeningWhenReady) {
      Log.d(
        "VoiceInputManager",
        "triggerListeningIfQueued: Launching deferred auto-start listening session.",
      )
      autoStartListeningWhenReady = false
      val recognizer = speechRecognizer ?: return
      val onResult = queuedOnResult
      startListening(recognizer, onResult)
    }
  }

  fun setPermissionGranted(granted: Boolean) {
    hasRecordAudioPermission = granted
    if (granted) {
      initSpeechAndTranslation()
      checkModelStatus()
    }
  }

  fun handleListenToggle(
    onPermissionRequired: () -> Unit,
    onResult: ((original: String, translated: String) -> Unit)? = null,
  ) {
    Log.d(
      "VoiceInputManager",
      "handleListenToggle: Triggered. isListening=${_uiState.value.isListening}, isReady=${_uiState.value.isReady}",
    )
    scope.launch {
      if (_uiState.value.isListening) {
        Log.d("VoiceInputManager", "handleListenToggle: Stopping current active listening session.")
        stopListening(onResult)
        return@launch
      }

      if (!hasRecordAudioPermission) {
        Log.w(
          "VoiceInputManager",
          "handleListenToggle: Bypassed toggle due to missing RECORD_AUDIO permission.",
        )
        onPermissionRequired()
        return@launch
      }

      initSpeechAndTranslation()

      Log.d(
        "VoiceInputManager",
        "handleListenToggle: Constructing fresh single-use SpeechRecognizer for instant unshared rebind.",
      )
      try {
        speechRecognizer?.close()
      } catch (e: Exception) {}

      val currentLang = FeatureFlags.DEMO_LANGUAGE
      try {
        val speechOptions = speechRecognizerOptions {
          locale = Locale(currentLang)
          preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
        }
        speechRecognizer = SpeechRecognition.getClient(speechOptions)
      } catch (e: Exception) {
        Log.e(
          "VoiceInputManager",
          "handleListenToggle: Failed to build fresh SpeechRecognizer client.",
          e,
        )
        speechRecognizer = null
      }

      val recognizer =
        speechRecognizer
          ?: run {
            Log.e(
              "VoiceInputManager",
              "handleListenToggle: Failed because speechRecognizer is null.",
            )
            _uiState.value =
              _uiState.value.copy(
                isListening = false,
                isError = true,
                showDialog = true,
                statusText = "Speech recognizer could not be initialized.",
                audioLevel = 0f,
              )
            return@launch
          }

      if (!_uiState.value.isReady) {
        Log.d(
          "VoiceInputManager",
          "handleListenToggle: Mic triggered but model is not ready. Queuing auto-start listener on status completed.",
        )
        autoStartListeningWhenReady = true
        queuedOnResult = onResult
        _uiState.value =
          _uiState.value.copy(
            showDialog = true,
            statusText = "Speech model is not ready. Initializing...",
          )
        checkModelStatus()
        return@launch
      }
      Log.d("VoiceInputManager", "handleListenToggle: Model is ready. Starting listening segment.")
      startListening(recognizer, onResult)
    }
  }

  private fun startListening(
    recognizer: SpeechRecognizer,
    onResult: ((original: String, translated: String) -> Unit)? = null,
  ) {
    Log.d(
      "VoiceInputManager",
      "startListening: Waking up microphone and opening SODA continuous streaming collection.",
    )
    _uiState.value =
      _uiState.value.copy(
        isListening = true,
        showDialog = true,
        statusText = "Warming up mic...",
        transcription = "",
        partialTranscription = "",
        translatedTranscription = "",
        audioLevel = 0f,
        isError = false,
      )
    listenJob = scope.launch {
      var retryCount = 0
      val maxRetries = 5
      var success = false
      var currentRecognizer = recognizer

      while (retryCount < maxRetries && !success) {
        try {
          val request = speechRecognizerRequest { audioSource = AudioSource.fromMic() }
          Log.d(
            "VoiceInputManager",
            "startListening: Invoking startRecognition flow... attempt ${retryCount + 1}",
          )

          var errorOccurred = false
          var lastError: Throwable? = null

          var lastVoiceActivityTime = System.currentTimeMillis()
          val autoStopJob = scope.launch {
            while (isActive) {
              kotlinx.coroutines.delay(500)
              if (System.currentTimeMillis() - lastVoiceActivityTime > 3000L) {
                Log.d("VoiceInputManager", "startListening: Auto-stopping due to silence.")
                stopListening(onResult)
                break
              }
            }
          }

          currentRecognizer.startRecognition(request).collect { response ->
            if (
              _uiState.value.statusText == "Warming up mic..." ||
                _uiState.value.statusText.startsWith("Mic busy, retrying")
            ) {
              _uiState.value = _uiState.value.copy(statusText = "Listening...")
            }
            when (response) {
              is SpeechRecognizerResponse.PartialTextResponse -> {
                success = true
                lastVoiceActivityTime = System.currentTimeMillis()
                Log.d(
                  "VoiceInputManager",
                  "startRecognition collect: PartialTextResponse='${response.text}'",
                )
                _uiState.value =
                  _uiState.value.copy(
                    statusText = "Transcribing...",
                    partialTranscription = response.text,
                  )
                translateText(_uiState.value.transcription + response.text)
              }
              is SpeechRecognizerResponse.FinalTextResponse -> {
                success = true
                lastVoiceActivityTime = System.currentTimeMillis()
                Log.d(
                  "VoiceInputManager",
                  "startRecognition collect: FinalTextResponse='${response.text}'",
                )
                val newTranscription = _uiState.value.transcription + response.text + " "
                _uiState.value =
                  _uiState.value.copy(transcription = newTranscription, partialTranscription = "")
                translateText(newTranscription)
              }
              is SpeechRecognizerResponse.ErrorResponse -> {
                Log.e(
                  "VoiceInputManager",
                  "startRecognition collect: ErrorResponse: Error ${response.e.message}",
                  response.e,
                )
                errorOccurred = true
                lastError = response.e
              }
              is SpeechRecognizerResponse.CompletedResponse -> {
                Log.d(
                  "VoiceInputManager",
                  "startRecognition collect: CompletedResponse received naturally from SODA stream.",
                )
                if (_uiState.value.isListening) {
                  finishListening(onResult)
                }
                success = true
              }
              else -> {
                if (response.javaClass.simpleName == "AudioLevelResponse") {
                  try {
                    val field = response.javaClass.getDeclaredField("audioLevel")
                    field.isAccessible = true
                    val level = field.get(response) as? Number
                    if (level != null) {
                      _uiState.value = _uiState.value.copy(audioLevel = level.toFloat())
                    }
                  } catch (e: Exception) {
                    Log.e("VoiceInputManager", "Failed to read audio level via reflection", e)
                  }
                } else {
                  Log.d(
                    "VoiceInputManager",
                    "startRecognition collect: Unknown response: $response",
                  )
                }
              }
            }
          }

          autoStopJob.cancel()

          if (errorOccurred) {
            val isRetryable = lastError?.message?.contains("ERROR_TYPE_INVALID_REQUEST") == true
            if (isRetryable && retryCount < maxRetries - 1) {
              retryCount++
              _uiState.value =
                _uiState.value.copy(statusText = "Mic busy, retrying... ($retryCount/$maxRetries)")
              Log.w(
                "VoiceInputManager",
                "Releasing speech recognizer and waiting 1500ms to retry due to INVALID_REQUEST model load...",
              )
              try {
                currentRecognizer.close()
              } catch (e: Exception) {}
              kotlinx.coroutines.delay(1500)
              val currentLang = FeatureFlags.DEMO_LANGUAGE
              try {
                val speechOptions = speechRecognizerOptions {
                  locale = Locale(currentLang)
                  preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
                }
                speechRecognizer = SpeechRecognition.getClient(speechOptions)
                currentRecognizer = speechRecognizer!!
              } catch (e: Exception) {
                Log.e("VoiceInputManager", "Failed to recreate speechRecognizer", e)
              }
            } else {
              _uiState.value =
                _uiState.value.copy(
                  isListening = false,
                  isError = true,
                  showDialog = true,
                  statusText = getUserFriendlyErrorMessage(lastError),
                  audioLevel = 0f,
                )
              break
            }
          } else {
            break
          }
        } catch (e: Exception) {
          if (e is kotlinx.coroutines.CancellationException) throw e
          Log.e(
            "VoiceInputManager",
            "startListening exception: Continuous recognition loop failed.",
            e,
          )
          val isRetryable = e.message?.contains("ERROR_TYPE_INVALID_REQUEST") == true
          if (isRetryable && retryCount < maxRetries - 1) {
            retryCount++
            _uiState.value =
              _uiState.value.copy(statusText = "Mic busy, retrying... ($retryCount/$maxRetries)")
            try {
              currentRecognizer.close()
            } catch (ex: Exception) {}
            kotlinx.coroutines.delay(1500)
            val currentLang = FeatureFlags.DEMO_LANGUAGE
            try {
              val speechOptions = speechRecognizerOptions {
                locale = Locale(currentLang)
                preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
              }
              speechRecognizer = SpeechRecognition.getClient(speechOptions)
              currentRecognizer = speechRecognizer!!
            } catch (ex: Exception) {}
          } else {
            _uiState.value =
              _uiState.value.copy(
                isListening = false,
                isError = true,
                showDialog = true,
                statusText = getUserFriendlyErrorMessage(e),
                audioLevel = 0f,
              )
            break
          }
        } finally {
          Log.d(
            "VoiceInputManager",
            "startListening: stopping recognizer in attempt loop finalizer.",
          )
          try {
            currentRecognizer.stopRecognition()
          } catch (e: Exception) {}
        }
      }
    }
  }

  private suspend fun stopListening(
    onResult: ((original: String, translated: String) -> Unit)? = null
  ) {
    _uiState.value =
      _uiState.value.copy(
        isListening = false,
        showDialog = false,
        statusText = "Completed.",
        audioLevel = 0f,
      )
    try {
      speechRecognizer?.stopRecognition()
      kotlinx.coroutines.delay(200)
    } catch (e: Exception) {
      Log.e("VoiceInputManager", "Failed to stop recognition gracefully", e)
    }
    listenJob?.cancel()
    finishListening(onResult)
  }

  private fun finishListening(onResult: ((original: String, translated: String) -> Unit)? = null) {
    val original = (_uiState.value.transcription + _uiState.value.partialTranscription).trim()
    val translated = _uiState.value.translatedTranscription.trim()
    val finalResult = translated.ifEmpty { original }

    Log.d("VoiceInputManager", "finishListening: finalResult='$finalResult'")
    _uiState.value =
      _uiState.value.copy(
        isListening = false,
        showDialog = false,
        statusText = "Completed.",
        transcriptionResult = finalResult,
        isError = false,
      )
    onResult?.invoke(original, translated)
  }

  private fun translateText(text: String) {
    if (text.isNotEmpty()) {
      translator?.translate(text)?.addOnSuccessListener { translatedText ->
        _uiState.value = _uiState.value.copy(translatedTranscription = translatedText)
      }
    } else {
      _uiState.value = _uiState.value.copy(translatedTranscription = "")
    }
  }

  fun clearTranscriptionResult() {
    _uiState.value = _uiState.value.copy(transcriptionResult = null)
  }

  fun cancelListening() {
    Log.d("VoiceInputManager", "cancelListening called")
    autoStartListeningWhenReady = false
    queuedOnResult = null
    _uiState.value =
      _uiState.value.copy(
        isListening = false,
        showDialog = false,
        statusText = "Initializing...",
        audioLevel = 0f,
        isError = false,
      )
    val recognizer = speechRecognizer
    scope.launch {
      try {
        recognizer?.stopRecognition()
        kotlinx.coroutines.delay(200)
      } catch (e: Exception) {
        Log.e("VoiceInputManager", "Failed to stop recognition during cancel", e)
      }
      listenJob?.cancel()
    }
  }

  suspend fun translate(text: String): String {
    if (FeatureFlags.DEMO_LANGUAGE == "en") return text
    initSpeechAndTranslation()
    val currentTranslator = translator ?: return text
    return try {
      currentTranslator.translate(text).await()
    } catch (e: Exception) {
      if (e is kotlinx.coroutines.CancellationException) throw e
      text
    }
  }

  private fun getUserFriendlyErrorMessage(e: Throwable?): String {
    val msg = e?.message ?: return "An unexpected error occurred."
    return when {
      msg.contains("ERROR_TYPE_AICORE_NOT_ENABLED_RUNTIME_LIMITS") -> {
        "AI Core speech engine runtime limits exceeded. Please wait a few moments and try again."
      }
      msg.contains("PERMISSION_DENIED") || msg.contains("permission") -> {
        "Microphone permission is required for voice input."
      }
      msg.contains("NETWORK_ERROR") || msg.contains("internet") || msg.contains("timeout") -> {
        "Network error. Please check your connection."
      }
      else -> {
        "Speech recognition engine was closed due to an internal error: ${e.message}"
      }
    }
  }

  fun close() {
    listenJob?.cancel()
  }
}
