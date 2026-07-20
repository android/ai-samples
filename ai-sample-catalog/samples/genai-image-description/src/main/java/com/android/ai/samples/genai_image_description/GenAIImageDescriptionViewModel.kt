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
package com.android.ai.samples.genai_image_description

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.geminimultimodal.R
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.imagedescription.ImageDescriber
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

sealed class GenAIImageDescriptionUiState {
    data object Initial : GenAIImageDescriptionUiState()
    data object CheckingFeatureStatus : GenAIImageDescriptionUiState()
    data class DownloadingFeature(
        val bytesToDownload: Long,
        val bytesDownloaded: Long,
    ) : GenAIImageDescriptionUiState()

    data class Generating(val partialOutput: String) : GenAIImageDescriptionUiState()
    data class Success(val generatedOutput: String) : GenAIImageDescriptionUiState()
    data class Error(@StringRes val errorMessageStringRes: Int) : GenAIImageDescriptionUiState()
}

class GenAIImageDescriptionViewModel @Inject constructor(val context: Application) : AndroidViewModel(context) {
    private val _uiState = MutableStateFlow<GenAIImageDescriptionUiState>(GenAIImageDescriptionUiState.Initial)
    val uiState: StateFlow<GenAIImageDescriptionUiState> = _uiState.asStateFlow()

    private var imageDescriber: ImageDescriber = ImageDescription.getClient(
        ImageDescriberOptions.builder(context).build(),
    )

    fun clearGeneratedText() {
        _uiState.value = GenAIImageDescriptionUiState.Initial
    }

    fun getImageDescription(imageUri: Uri?) {
        if (imageUri == null) {
            _uiState.value = GenAIImageDescriptionUiState.Error(R.string.genai_image_description_no_image_selected)
            return
        }

        viewModelScope.launch {
            var featureStatus = FeatureStatus.UNAVAILABLE

            try {
                _uiState.value = GenAIImageDescriptionUiState.CheckingFeatureStatus
                featureStatus = imageDescriber.checkFeatureStatus().await()
            } catch (error: Exception) {
                _uiState.value = GenAIImageDescriptionUiState.Error(R.string.image_desc_feature_check_fail)
                Log.e("GenAIImageDesc", "Error checking feature status", error)
            }

            if (featureStatus == FeatureStatus.UNAVAILABLE) {
                _uiState.value = GenAIImageDescriptionUiState.Error(R.string.genai_image_description_not_available)
                return@launch
            }

            if (featureStatus == FeatureStatus.DOWNLOADABLE || featureStatus == FeatureStatus.DOWNLOADING) {
                imageDescriber.downloadFeature(
                    object : DownloadCallback {
                        override fun onDownloadStarted(bytesToDownload: Long) {
                            _uiState.value = GenAIImageDescriptionUiState.DownloadingFeature(bytesToDownload, 0)
                        }

                        override fun onDownloadProgress(bytesDownloaded: Long) {
                            _uiState.update {
                                (it as? GenAIImageDescriptionUiState.DownloadingFeature)?.copy(bytesDownloaded = bytesDownloaded) ?: it
                            }
                        }

                        override fun onDownloadCompleted() {
                            viewModelScope.launch {
                                generateImageDescription(imageUri)
                            }
                        }

                        override fun onDownloadFailed(exception: GenAiException) {
                            Log.e("GenAIImageDesc", "Download failed", exception)
                            _uiState.value = GenAIImageDescriptionUiState.Error(R.string.image_desc_download_failed)
                        }
                    },
                )
            } else {
                generateImageDescription(imageUri)
            }
        }
    }

    private suspend fun generateImageDescription(imageUri: Uri) {
        _uiState.value = GenAIImageDescriptionUiState.Generating("")
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val request = ImageDescriptionRequest.builder(bitmap).build()

        imageDescriber.runInference(request) { newText ->
            _uiState.update {
                (it as? GenAIImageDescriptionUiState.Generating)?.copy(partialOutput = it.partialOutput + newText) ?: it
            }
        }.await()

        (_uiState.value as? GenAIImageDescriptionUiState.Generating)?.partialOutput?.let { generatedOutput ->
            _uiState.value = GenAIImageDescriptionUiState.Success(generatedOutput)
        }
    }

    override fun onCleared() {
        imageDescriber.close()
    }
}
