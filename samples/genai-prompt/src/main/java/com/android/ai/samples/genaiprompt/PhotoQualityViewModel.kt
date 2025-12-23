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

package com.android.ai.samples.genaiprompt

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

sealed class PhotoQualityUiState {
    data object Initial : PhotoQualityUiState()
    data object CheckingFeatureStatus : PhotoQualityUiState()
    data class DownloadingFeature(
        val bytesToDownload: Long,
        val bytesDownloaded: Long,
    ) : PhotoQualityUiState()

    data object Analyzing : PhotoQualityUiState()
    data class Success(val resultText: String, val isHighQuality: Boolean) : PhotoQualityUiState()
    data class Error(@StringRes val errorMessageStringRes: Int) : PhotoQualityUiState()
}

@HiltViewModel
class PhotoQualityViewModel @Inject constructor(val context: Application) : AndroidViewModel(context) {

    private val _uiState = MutableStateFlow<PhotoQualityUiState>(PhotoQualityUiState.Initial)
    val uiState: StateFlow<PhotoQualityUiState> = _uiState.asStateFlow()

    private val generativeModel: GenerativeModel = Generation.getClient()

    fun clearState() {
        _uiState.value = PhotoQualityUiState.Initial
    }

    fun checkPhotoQuality(imageUri: Uri?) {
        if (imageUri == null) {
            _uiState.value = PhotoQualityUiState.Error(R.string.genai_prompt_error_generic)
            return
        }

        viewModelScope.launch {
            var featureStatus = FeatureStatus.UNAVAILABLE

            try {
                _uiState.value = PhotoQualityUiState.CheckingFeatureStatus
                featureStatus = generativeModel.checkStatus()
            } catch (error: Exception) {
                Log.e(TAG, "Error checking feature status", error)
                _uiState.value = PhotoQualityUiState.Error(R.string.genai_prompt_error_generic)
            }

            if (featureStatus == FeatureStatus.UNAVAILABLE) {
                _uiState.value = PhotoQualityUiState.Error(R.string.genai_prompt_error_generic)
                return@launch
            }

            if (featureStatus == FeatureStatus.DOWNLOADABLE || featureStatus == FeatureStatus.DOWNLOADING) {
                generativeModel.download().collect { status ->
                     when (status) {
                        is DownloadStatus.DownloadStarted -> {
                            _uiState.value = PhotoQualityUiState.DownloadingFeature(0, 0)
                        }
                        is DownloadStatus.DownloadProgress -> {
                            _uiState.update {
                                (it as? PhotoQualityUiState.DownloadingFeature)?.copy(
                                    bytesDownloaded = status.totalBytesDownloaded,
                                    // bytesToDownload not available in status
                                ) ?: it
                            }
                        }
                        is DownloadStatus.DownloadCompleted -> {
                            analyzePhoto(imageUri)
                        }
                        is DownloadStatus.DownloadFailed -> {
                            Log.e(TAG, "Download failed", status.e)
                            _uiState.value = PhotoQualityUiState.Error(R.string.genai_prompt_error_generic)
                        }
                    }
                }
            } else {
                analyzePhoto(imageUri)
            }
        }
    }


    private suspend fun analyzePhoto(imageUri: Uri) {
        _uiState.value = PhotoQualityUiState.Analyzing
        
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            
            // Prompt: "Does this photo show exactly one person with a clear view of their face? Answer strictly YES or NO."
            val prompt = "Does this photo show exactly one person with a clear view of their face? Answer strictly YES or NO."
            
            val response = generativeModel.generateContent(
                generateContentRequest(ImagePart(bitmap), TextPart(prompt)) {}
            )

            // Access text directly from candidate
            val output = response.candidates.firstOrNull()?.text ?: ""
            
            Log.d(TAG, "Model output: $output")

            val isHighQuality = output.trim().equals("YES", ignoreCase = true)
            val resultText = if (isHighQuality) {
                context.getString(R.string.genai_prompt_result_good)
            } else {
                context.getString(R.string.genai_prompt_result_bad)
            }

            _uiState.value = PhotoQualityUiState.Success(resultText, isHighQuality)

        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
            _uiState.value = PhotoQualityUiState.Error(R.string.genai_prompt_error_generic)
        }
    }



    override fun onCleared() {
        // generativeModel.close() // If close exists
        super.onCleared()
    }

    companion object {
        private const val TAG = "PhotoQualityVM"
    }
}
