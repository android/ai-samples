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
package com.android.ai.samples.geminivideosummary

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.android.ai.samples.geminivideosummary.player.VideoPlayer
import com.android.ai.samples.geminivideosummary.player.VideoSelectionDropdown
import com.android.ai.samples.geminivideosummary.ui.OutputTextDisplay
import com.android.ai.samples.geminivideosummary.ui.TextToSpeechControls
import com.android.ai.samples.geminivideosummary.util.sampleVideoList
import com.android.ai.samples.geminivideosummary.viewmodel.VideoSummarizationViewModel
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
    var textToSpeech: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsInitialized by remember { mutableStateOf(false) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(uiState.selectedVideoUri) {
        uiState.selectedVideoUri?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it))
            exoPlayer.prepare()
            textToSpeech?.stop()
            viewModel.onSpeakingStateChanged(isSpeaking = false, isPaused = true)
        }
    }

    DisposableEffect(key1 = true) {
        textToSpeech = initializeTextToSpeech(context) { initialized ->
            isTtsInitialized = initialized
        }
        onDispose {
            textToSpeech?.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(text = stringResource(R.string.video_summarization_title))
                },
                actions = {
                    SeeCodeButton()
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(12.dp)
                .padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            VideoSelectionDropdown(
                selectedVideoUri = uiState.selectedVideoUri,
                isDropdownExpanded = uiState.isDropdownExpanded,
                videoOptions = sampleVideoList,
                onVideoUriSelected = { uri ->
                    viewModel.onVideoSelected(uri)
                },
                onDropdownExpanded = { viewModel.onDropdownExpandedChanged(it) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            VideoPlayer(exoPlayer = exoPlayer, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    textToSpeech?.stop()
                    viewModel.onSpeakingStateChanged(isSpeaking = false, isPaused = true)
                    viewModel.summarize()
                },
                enabled = !uiState.isSummarizing,
            ) {
                Text(stringResource(R.string.summarize_video_button))
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.summarizedText != null) {
                Spacer(modifier = Modifier.height(8.dp))

                TextToSpeechControls(
                    isInitialized = isTtsInitialized,
                    isSpeaking = uiState.isSpeaking,
                    isPaused = uiState.isPaused,
                    textToSpeech = textToSpeech,
                    speechText = uiState.summarizedText ?: "",
                    selectedAccent = uiState.selectedAccent,
                    accentOptions = accentOptions,
                    onSpeakingStateChange = { speaking, paused ->
                        viewModel.onSpeakingStateChanged(speaking, paused)
                    },
                    isAccentDropdownExpanded = uiState.isAccentDropdownExpanded,
                    onAccentSelected = { accent ->
                        viewModel.onAccentSelected(accent)
                    },
                    onAccentDropdownExpanded = { viewModel.onAccentDropdownExpandedChanged(it) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isSummarizing -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                uiState.errorMessage != null -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissError() },
                        title = { Text("Error") },
                        text = { Text(uiState.errorMessage!!) },
                        confirmButton = {
                            Button(onClick = { viewModel.dismissError() }) {
                                Text("OK")
                            }
                        },
                    )
                }
                uiState.summarizedText != null -> {
                    OutputTextDisplay(uiState.summarizedText!!, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
}

val accentOptions = listOf(
    Locale.UK,
    Locale.FRANCE,
    Locale.GERMANY,
    Locale.ITALY,
    Locale.JAPAN,
    Locale.KOREA,
    Locale.US,
)

fun initializeTextToSpeech(context: Context, onInitialized: (Boolean) -> Unit): TextToSpeech {
    return TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            onInitialized(true)
        } else {
            Log.e("TextToSpeech", "Initialization failed")
            onInitialized(false)
        }
    }
}

@Composable
fun SeeCodeButton() {
    val context = LocalContext.current
    val githubLink =
        "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-video-summarization"
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, githubLink.toUri())
            context.startActivity(intent)
        },
    ) {
        Icon(Icons.Filled.Code, contentDescription = "See code")
        Text(
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 12.sp,
            text = stringResource(R.string.see_code),
        )
    }
}
