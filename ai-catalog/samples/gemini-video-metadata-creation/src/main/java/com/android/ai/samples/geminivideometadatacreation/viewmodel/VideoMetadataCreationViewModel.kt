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
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.android.ai.samples.geminivideometadatacreation.R
import com.android.ai.samples.geminivideometadatacreation.generateAccountTags
import com.android.ai.samples.geminivideometadatacreation.generateChapters
import com.android.ai.samples.geminivideometadatacreation.generateDescription
import com.android.ai.samples.geminivideometadatacreation.generateHashtags
import com.android.ai.samples.geminivideometadatacreation.generateLinks
import com.android.ai.samples.geminivideometadatacreation.generateThumbnails
import com.android.ai.samples.geminivideometadatacreation.util.sampleVideoList
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _uiState = MutableStateFlow(VideoMetadataCreationState())
    val uiState: StateFlow<VideoMetadataCreationState> = _uiState.asStateFlow()

    @OptIn(UnstableApi::class)
    fun generateMetadata(metadataType: MetadataType) {
        val videoUri = _uiState.value.selectedVideoUri ?: return
        // Since we will start an async call, show a progressbar
        _uiState.update { it.copy(metadataCreationState = MetadataCreationState.InProgress) }
        // Start the right async call based on the selected metadata type
        viewModelScope.launch {
            try {
                val generatedUI = when (metadataType) {
                    MetadataType.DESCRIPTION -> generateDescription(videoUri)
                    MetadataType.THUMBNAILS -> generateThumbnails(videoUri, application)
                    MetadataType.HASHTAGS -> generateHashtags(videoUri)
                    MetadataType.ACCOUNT_TAGS -> generateAccountTags(videoUri)
                    MetadataType.CHAPTERS -> generateChapters(
                        videoUri,
                        onChapterClicked = { timestamp ->
                            uiState.value.player?.seekTo(timestamp)
                        },
                    )
                    MetadataType.LINKS -> generateLinks(videoUri)
                }
                _uiState.update {
                    it.copy(
                        metadataCreationState = MetadataCreationState.Success(generatedUI),
                    )
                }
            } catch (e: Exception) {
                // If something went wrong, show an error
                _uiState.update {
                    it.copy(
                        metadataCreationState = MetadataCreationState.Error(
                            e.localizedMessage ?: "An unknown error occurred",
                        ),
                    )
                }
            }
        }
    }

    fun createPlayer() {
        _uiState.update { it ->
            it.copy(
                player = ExoPlayer.Builder(application).build().apply {
                    playWhenReady = true
                    playVideo(this, uiState.value.selectedVideoUri)
                },
            )
        }
    }

    fun releasePlayer() {
        uiState.value.player?.release()
        _uiState.update { it.copy(player = null) }
    }

    private fun playVideo(player: Player?, uri: Uri?) {
        if (player == null || uri == null) return
        player.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    fun onVideoSelected(uri: Uri) {
        playVideo(uiState.value.player, uri)
        _uiState.update { it.copy(selectedVideoUri = uri) }
    }

    fun onMetadataTypeSelected(metadataType: MetadataType) {
        _uiState.update { it.copy(selectedMetadataType = metadataType) }
    }

    fun resetMetadataState() {
        _uiState.update {
            it.copy(
                metadataCreationState = MetadataCreationState.Idle,
                selectedMetadataType = null,
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(metadataCreationState = MetadataCreationState.Idle) }
    }
}

enum class MetadataType(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
) {
    THUMBNAILS(com.android.ai.uicomponent.R.drawable.ic_ai_img, R.string.thumbnails),
    DESCRIPTION(com.android.ai.uicomponent.R.drawable.ic_ai_summary, R.string.description),
    HASHTAGS(com.android.ai.uicomponent.R.drawable.ic_ai_hashtags, R.string.hashtags),
    ACCOUNT_TAGS(com.android.ai.uicomponent.R.drawable.ic_ai_tags, R.string.account_tags),
    CHAPTERS(com.android.ai.uicomponent.R.drawable.ic_ai_chapters, R.string.chapters),
    LINKS(com.android.ai.uicomponent.R.drawable.ic_ai_hashtags, R.string.links),
}

sealed interface MetadataCreationState {
    data object Idle : MetadataCreationState
    data object InProgress : MetadataCreationState
    data class Error(val message: String) : MetadataCreationState
    data class Success(
        val generatedUi: @Composable () -> Unit,
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
    val selectedMetadataType: MetadataType? = null,
    val player: Player? = null,
)
