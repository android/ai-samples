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

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.android.ai.samples.geminivideosummary.player.VideoPlayer
import com.android.ai.samples.geminivideosummary.player.VideoSelectionDropdown
import com.android.ai.samples.geminivideosummary.util.sampleVideoList
import com.android.ai.samples.geminivideosummary.viewmodel.SummarizationState
import com.android.ai.samples.geminivideosummary.viewmodel.TtsState
import com.android.ai.samples.geminivideosummary.viewmodel.VideoSummarizationState
import com.android.ai.samples.geminivideosummary.viewmodel.VideoSummarizationViewModel
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.theme.extendedColorScheme
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SecondaryButton
import com.google.com.android.ai.samples.geminivideosummary.R
import java.util.Locale

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

    LaunchedEffect(uiState.selectedVideoUri) {
        uiState.selectedVideoUri?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it))
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
        onVideoSelected = { titleId, uri ->
            viewModel.onVideoSelected(titleId = titleId, uri = uri) },
        onSummarizeClick = {
            viewModel.onTtsStateChanged(TtsState.Idle)
            viewModel.summarize()
        },
        onTtsStateChanged = viewModel::onTtsStateChanged,
        onAccentSelected = viewModel::onAccentSelected,
        onDismissError = viewModel::dismissError,
        onRedo = viewModel::redo,
        onTtsInitializationResult = viewModel::onTtsInitializationResult
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoSummarizationScreen(
    uiState: VideoSummarizationState,
    exoPlayer: ExoPlayer?,
    isDropdownExpanded: Boolean,
    onDropdownExpandedChanged: (Boolean) -> Unit,
    onVideoSelected: (Int, Uri) -> Unit,
    onSummarizeClick: () -> Unit,
    onTtsStateChanged: (TtsState) -> Unit,
    onAccentSelected: (Locale) -> Unit,
    onDismissError: () -> Unit,
    onRedo: () -> Unit,
    onTtsInitializationResult: (Boolean, String?) -> Unit
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.video_summarization_title),
                sampleDescription = stringResource(R.string.video_summarization_description),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-video-summarization",
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
            VideoSelectionDropdown(
                selectedVideoUri = Pair(uiState.selectedVideoTitle, uiState.selectedVideoUri),
                isDropdownExpanded = isDropdownExpanded,
                videoOptions = sampleVideoList,
                onVideoUriSelected = onVideoSelected,
                onDropdownExpanded = onDropdownExpandedChanged,
            )

            if (exoPlayer != null) {
                VideoPlayer(exoPlayer = exoPlayer, modifier = Modifier.fillMaxWidth())
            }

            SummarizationSection(
                uiState = uiState,
                onSummarizeClick = onSummarizeClick,
                onTtsStateChanged = onTtsStateChanged,
                onAccentSelected = onAccentSelected,
                onDismissError = onDismissError,
                onTtsInitializationResult = onTtsInitializationResult,
                onRedo = onRedo,
                modifier = Modifier.weight(1f)
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
    onAccentSelected: (Locale) -> Unit,
    onDismissError: () -> Unit,
    onTtsInitializationResult: (Boolean, String?) -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newState -> newState != SheetValue.Hidden }
    )
    Box(
        modifier = modifier
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
                    ModalBottomSheet(
                        onDismissRequest = { },
                        sheetState = sheetState,
                    ) {
                        Column (modifier = Modifier.padding(24.dp)) {
                            TextToSpeechControls(
                                title = uiState.selectedVideoTitle,
                                ttsState = summarizationState.ttsState,
                                speechText = summarizationState.summarizedText,
                                selectedAccent = uiState.selectedAccent,
                                accentOptions = accentOptions,
                                onTtsStateChange = onTtsStateChanged,
                                onAccentSelected = onAccentSelected,
                                onInitializationResult = onTtsInitializationResult,
                            )
                            OutputTextDisplay(
                                summarizationState.summarizedText,
                            )
                            Row (modifier
                                .wrapContentHeight()
                            ){
                                Text(
                                    text = stringResource(R.string.text_generated_with_gemini),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    modifier = Modifier
                                        .background(
                                            color = extendedColorScheme.geminiProFlash,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                )
                                Spacer(modifier.weight(1f).height(1.dp))
                                SecondaryButton(
                                    text = "",
                                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_redo),
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(32.dp),
                                    onClick = onRedo,
                                )
                            }
                        }
                    }
                }

                is SummarizationState.Idle -> {
                    // Nothing to show
                }
            }
        }
        GenerateButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            text = stringResource(R.string.summarize_video_button),
            icon = painterResource(com.android.ai.uicomponent.R.drawable.ic_video_play),
            onClick = onSummarizeClick,
            enabled = uiState.summarizationState != SummarizationState.InProgress,
        )
    }
}

private val accentOptions = listOf(
    Locale.UK,
    Locale.US,
    Locale.CANADA,
)

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VideoSummarizationScreenPreview() {
    val context = LocalContext.current
    AISampleCatalogTheme {
        VideoSummarizationScreen(
            uiState = VideoSummarizationState(),
            exoPlayer = null,
            isDropdownExpanded = false,
            onDropdownExpandedChanged = {},
            onVideoSelected = {_, _ -> },
            onSummarizeClick = {},
            onTtsStateChanged = {},
            onAccentSelected = {},
            onDismissError = {},
            onRedo = {},
            onTtsInitializationResult = { _, _ -> },
        )
    }
}
