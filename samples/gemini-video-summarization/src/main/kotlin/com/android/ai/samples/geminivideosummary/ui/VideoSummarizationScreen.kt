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
package com.android.ai.samples.geminivideosummary.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.android.ai.samples.geminivideosummary.util.VideoItem
import com.android.ai.samples.geminivideosummary.util.sampleVideoList
import com.android.ai.samples.geminivideosummary.viewmodel.SummarizationState
import com.android.ai.samples.geminivideosummary.viewmodel.TtsState
import com.android.ai.samples.geminivideosummary.viewmodel.VideoSummarizationState
import com.android.ai.samples.geminivideosummary.viewmodel.VideoSummarizationViewModel
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SelectableItem
import com.android.ai.uicomponent.VideoPickerData
import com.android.ai.uicomponent.VideoPickerDropdown
import com.android.ai.uicomponent.VideoPlayer
import com.google.com.android.ai.samples.geminivideosummary.R

/**
 * Composable function for the AI Video Summarization screen.
 *
 * This screen allows users to select a video, play it, and generate a summary of its content
 * using Firebase AI. It also provides text-to-speech functionality to read out
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSummarizationScreen(viewModel: VideoSummarizationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(uiState.selectedVideo) {
        uiState.selectedVideo?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it.uri))
            exoPlayer.prepare()
        }
    }

    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    VideoSummarizationScreen(
        uiState = uiState,
        exoPlayer = exoPlayer,
        isDropdownExpanded = isDropdownExpanded,
        onDropdownExpandedChanged = { isDropdownExpanded = it },
        onVideoSelected = { viewModel.onVideoSelected(it) },
        onSummarizeClick = {
            viewModel.onTtsStateChanged(TtsState.Idle)
            viewModel.summarize()
        },
        onTtsStateChanged = viewModel::onTtsStateChanged,
        onDismissError = viewModel::dismissError,
        onRedo = viewModel::redo,
        onTtsInitializationResult = viewModel::onTtsInitializationResult,
    )
}

class VideoSelectableItem(
    override val itemLabel: String,
    override val itemData: VideoItem,
) : SelectableItem<VideoItem>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoSummarizationScreen(
    uiState: VideoSummarizationState,
    exoPlayer: ExoPlayer?,
    isDropdownExpanded: Boolean,
    onDropdownExpandedChanged: (Boolean) -> Unit,
    onVideoSelected: (VideoItem) -> Unit,
    onSummarizeClick: () -> Unit,
    onTtsStateChanged: (TtsState) -> Unit,
    onDismissError: () -> Unit,
    onRedo: () -> Unit,
    onTtsInitializationResult: (Boolean, String?) -> Unit,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val videoItemList = sampleVideoList.map { item -> VideoSelectableItem(stringResource(item.titleResId), item) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.video_summarization_title),
                sampleDescription = stringResource(R.string.video_summarization_description),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/samples/gemini-video-summarization",
                onBackClick = { backDispatcher?.onBackPressed() },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VideoPlayer(
                player = exoPlayer,
                videoPicker = {
                    VideoPickerDropdown(
                        videoItems = sampleVideoList.map { VideoPickerData(stringResource(it.titleResId), it.uri) },
                        selectedVideo = uiState.selectedVideo?.uri,
                        isExpanded = isDropdownExpanded,
                        onDropdownExpandedChanged = onDropdownExpandedChanged,
                        onVideoSelected = { videoData ->
                            sampleVideoList.firstOrNull { it.uri == videoData.uri }?.let(onVideoSelected)
                        },
                    )
                },
                forceShowControls = isDropdownExpanded,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )

            SummarizationSection(
                uiState = uiState,
                onSummarizeClick = onSummarizeClick,
                onTtsStateChanged = onTtsStateChanged,
                onDismissError = onDismissError,
                onTtsInitializationResult = onTtsInitializationResult,
                onRedo = onRedo,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummarizationSection(
    uiState: VideoSummarizationState,
    onSummarizeClick: () -> Unit,
    onTtsStateChanged: (TtsState) -> Unit,
    onDismissError: () -> Unit,
    onTtsInitializationResult: (Boolean, String?) -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSummary by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.summarizationState) {
        if (uiState.summarizationState is SummarizationState.Success) {
            showSummary = true
        }
    }

    val onRedoClick = {
        onRedo()
        showSummary = false
    }

    Box(
        modifier = modifier,
    ) {
        Column {
            when (val summarizationState = uiState.summarizationState) {
                is SummarizationState.InProgress -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                is SummarizationState.Error -> {
                    AlertDialog(
                        onDismissRequest = onDismissError,
                        title = { Text("Error") },
                        text = { Text(summarizationState.message) },
                        confirmButton = {
                            Button(onClick = onDismissError) {
                                Text("OK")
                            }
                        },
                    )
                }

                is SummarizationState.Success -> {
                    if (showSummary) {
                        SummarizationSheet(
                            uiState = uiState,
                            onTtsStateChanged = onTtsStateChanged,
                            onTtsInitializationResult = onTtsInitializationResult,
                            onRedo = onRedoClick,
                            onDismiss = { showSummary = false },
                        )
                    }
                }

                is SummarizationState.Idle -> {
                    // Nothing to show
                }
            }
        }

        val summarizationState = uiState.summarizationState
        if (!(summarizationState is SummarizationState.Success && showSummary)) {
            val buttonText = if (summarizationState is SummarizationState.Success) {
                stringResource(R.string.show_summary)
            } else {
                stringResource(R.string.summarize_video_button)
            }
            val buttonOnClick = if (summarizationState is SummarizationState.Success) {
                { showSummary = true }
            } else {
                onSummarizeClick
            }
            GenerateButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                text = buttonText,
                icon = painterResource(com.android.ai.uicomponent.R.drawable.ic_video_play),
                onClick = buttonOnClick,
                enabled = uiState.summarizationState != SummarizationState.InProgress,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VideoSummarizationScreenPreview() {
    AISampleCatalogTheme {
        VideoSummarizationScreen(
            uiState = VideoSummarizationState(),
            exoPlayer = null,
            isDropdownExpanded = false,
            onDropdownExpandedChanged = {},
            onVideoSelected = {},
            onSummarizeClick = {},
            onTtsStateChanged = {},
            onDismissError = {},
            onRedo = {},
            onTtsInitializationResult = { _, _ -> },
        )
    }
}
