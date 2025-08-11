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
package com.android.ai.samples.geminivideometadatacreation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.android.ai.samples.geminivideometadatacreation.player.extractListOfThumbnails
import com.android.ai.samples.geminivideometadatacreation.util.sampleVideoList
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel class responsible for handling video metadata creation using Gemini API.
 *
 * This ViewModel interacts with the Gemini API to generate metadata of a provided video.
 * It manages the state of the metadata creation process and exposes the output text through a
 * [StateFlow].
 */
@HiltViewModel
class VideoMetadataCreationViewModel @Inject constructor(private val application: Application) : ViewModel() {

    private val tag = "VideoMetadataVM"

    private val _uiState = MutableStateFlow(VideoMetadataCreationState())
    val uiState: StateFlow<VideoMetadataCreationState> = _uiState.asStateFlow()

    fun onVideoSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedVideoUri = uri,
            )
        }
    }

    fun onThumbnailStateChanged(newThumbnailState: ThumbnailState) {
        val currentState = _uiState.value.metadataCreationState
        if (currentState is MetadataCreationState.Success) {
            _uiState.update {
                it.copy(metadataCreationState = currentState.copy(thumbnailState = newThumbnailState))
            }
        }
    }

    fun onTtsInitializationResult(isSuccess: Boolean, errorMessage: String?) {
        if (!isSuccess && errorMessage != null) {
            _uiState.update {
                it.copy(metadataCreationState = MetadataCreationState.Error(errorMessage))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun onMetadataTypeSelected(metadataType: MetadataType) {
        _uiState.update { it.copy(selectedMetadataType = metadataType) }
    }

    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun createMetadata(metadataType: MetadataType) {
        val videoSource = _uiState.value.selectedVideoUri ?: return
        viewModelScope.launch {
            val promptData = getPromptForMetadataType(metadataType)
            _uiState.update { it.copy(metadataCreationState = MetadataCreationState.InProgress) }

            try {
                val generativeModel =
                    Firebase.ai(backend = GenerativeBackend.vertexAI())
                        .generativeModel("gemini-2.0-flash")

                val requestContent = content {
                    fileData(videoSource.toString(), "video/mp4")
                    text(promptData)
                }
                val outputStringBuilder = StringBuilder()
                generativeModel.generateContentStream(requestContent).collect { response ->
                    outputStringBuilder.append(response.text)
                }
                val metadataText = outputStringBuilder.toString()
                _uiState.update {
                    it.copy(
                        metadataCreationState = MetadataCreationState.Success(metadataText),
                    )
                }

                // Load HDR quality image thumbnails in Media3, based from timestamps returned by Gemini
                if (metadataType == MetadataType.THUMBNAILS) {
                    onThumbnailStateChanged(ThumbnailState.Loading)
                    val bitmaps = extractListOfThumbnails(application, videoSource, metadataText)
                    onThumbnailStateChanged(ThumbnailState.Success(bitmaps))
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        metadataCreationState = MetadataCreationState.Error(error.localizedMessage ?: "An unknown error occurred"),
                    )
                }
                Log.e(tag, "Error processing prompt : $error")
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(metadataCreationState = MetadataCreationState.Idle) }
    }
}

enum class MetadataType {
    DESCRIPTION,
    THUMBNAILS,
    HASHTAGS,
    ACCOUNT_TAGS,
    CHAPTERS,
    LINKS,
}

sealed interface MetadataCreationState {
    data object Idle : MetadataCreationState
    data object InProgress : MetadataCreationState
    data class Error(val message: String) : MetadataCreationState
    data class Success(
        val metadataText: String,
        val thumbnailState: ThumbnailState = ThumbnailState.Idle,
    ) : MetadataCreationState
}

sealed interface ThumbnailState {
    data object Idle : ThumbnailState
    data object Loading : ThumbnailState
    data class Success(val bitmaps: List<Bitmap>) : ThumbnailState
    data class Error(val message: String) : ThumbnailState
}

data class VideoMetadataCreationState(
    val selectedVideoUri: Uri? = sampleVideoList.first().uri,
    val metadataCreationState: MetadataCreationState = MetadataCreationState.Idle,
    val selectedAccent: Locale = Locale.US,
    val selectedMetadataType: MetadataType? = null,
)

private fun getPromptForMetadataType(metadataType: MetadataType): String {
    return when (metadataType) {
        MetadataType.DESCRIPTION ->
            "Provide a compelling and concise description for this video, suitable for a YouTube video description in about 7-8 lines." +
                "The description should be engaging and accurately reflect the video\'s content. Don't assume if you don't know"
        MetadataType.THUMBNAILS ->
            "Get three thumbnails for this video. Return only a comma separated list of timestamps in format \"hh:mm:ss\". Don\'t return any other text."
        MetadataType.HASHTAGS ->
            "Generate a list of relevant and trending hashtags for this video to maximize its visibility on social media platforms. Return only the list of hashtags, separated by commas."
        MetadataType.ACCOUNT_TAGS ->
            "Suggest relevant accounts to tag in the video\'s description or comments to increase its reach and engagement. Return only the list of accounts, separated by commas."
        MetadataType.CHAPTERS ->
            "Analyze the video and create a list of chapters with timestamps and descriptive titles. This will help viewers navigate the video and find specific sections of interest."
        MetadataType.LINKS ->
            "Analyze the video and create a list of relevant links to be tagged. Return possible 3-4 links to be shared in the video."
    }
}
