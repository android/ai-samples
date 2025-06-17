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
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.android.ai.samples.geminivideosummary.player.VideoPlayer
import com.android.ai.samples.geminivideosummary.player.VideoSelectionDropdown
import com.android.ai.samples.geminivideosummary.ui.OutputTextDisplay
import com.android.ai.samples.geminivideosummary.ui.TextToSpeechControls
import com.android.ai.samples.geminivideosummary.util.VideoList
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

    val context = LocalContext.current
    val videoList = VideoList().videos
    var selectedVideoUri by remember { mutableStateOf<Uri?>(videoList.first().uri) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var newVideoUrl by remember { mutableStateOf("") }
    val outputText by viewModel.outputText.collectAsState()
    var textForSpeech by remember { mutableStateOf("") }
    var textToSpeech: TextToSpeech? by remember { mutableStateOf(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    val videoOptions = videoList.map { it.title to it.uri }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(selectedVideoUri) {
        onSelectedVideoChange(
            selectedVideoUri,
            exoPlayer,
            textToSpeech,
            onSpeakingStateChange = { speaking, paused ->
                isSpeaking = speaking
                isPaused = paused
            },
        )
    }

    DisposableEffect(key1 = true) {
        textToSpeech = initializeTextToSpeech(context) { initialized ->
            isInitialized = initialized
        }
        onDispose {
            textToSpeech?.shutdown()
        }
    }
    var selectedAccent by remember { mutableStateOf(Locale.FRANCE) }
    val accentOptions = listOf(
        Locale.UK,
        Locale.FRANCE,
        Locale.GERMANY,
        Locale.ITALY,
        Locale.JAPAN,
        Locale.KOREA,
        Locale.US,
    )
    var isAccentDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(text = stringResource(R.string.video_summarization_title))
                }, actions = {
                    SeeCodeButton(context)
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
                selectedVideoUri = selectedVideoUri,
                isDropdownExpanded = isDropdownExpanded,
                videoOptions = videoOptions,
                onVideoUriSelected = { uri ->
                    selectedVideoUri = uri
                    viewModel.clearOutputText()
                },
                onNewVideoUrlChanged = { url ->
                    run {
                        newVideoUrl = url
                    }
                },
                onDropdownExpanded = { expanded ->
                    isDropdownExpanded = expanded
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            VideoPlayer(exoPlayer = exoPlayer, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                onSummarizeButtonClick(
                    selectedVideoUri, textToSpeech, onSpeakingStateChange = { speaking, paused ->
                        isSpeaking = speaking
                        isPaused = paused
                    }, viewModel,
                )
            }) {
                Text(stringResource(R.string.summarize_video_button))
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (outputText.toString().isNotEmpty()) {
                textForSpeech = outputText.toString()

                Spacer(modifier = Modifier.height(8.dp))

                TextToSpeechControls(
                    isInitialized = isInitialized,
                    isSpeaking = isSpeaking,
                    isPaused = isPaused,
                    textToSpeech = textToSpeech,
                    speechText = textForSpeech,
                    selectedAccent = selectedAccent,
                    accentOptions = accentOptions,
                    onSpeakingStateChange = { speaking, paused ->
                        isSpeaking = speaking
                        isPaused = paused
                    },
                    isAccentDropdownExpanded = isAccentDropdownExpanded,
                    onAccentSelected = { accent ->
                        selectedAccent = accent
                    },
                    onAccentDropdownExpanded = { expanded ->
                        isAccentDropdownExpanded = expanded
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutputTextDisplay(outputText = outputText, modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
}

fun onSelectedVideoChange(
    selectedVideoUri: Uri?,
    exoPlayer: ExoPlayer,
    textToSpeech: TextToSpeech?,
    onSpeakingStateChange: (speaking: Boolean, paused: Boolean) -> Unit,
) {
    if (selectedVideoUri != null) {
        if (selectedVideoUri == Uri.parse("")) {
            // do nothing
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(selectedVideoUri))
            exoPlayer.prepare()
        }
        textToSpeech?.stop()
        onSpeakingStateChange(false, true)
    }
}

fun onSummarizeButtonClick(
    selectedVideoUri: Uri?,
    textToSpeech: TextToSpeech?,
    onSpeakingStateChange: (speaking: Boolean, paused: Boolean) -> Unit,
    viewModel: VideoSummarizationViewModel,
) {
    if (selectedVideoUri != null) {
        viewModel.getVideoSummary(selectedVideoUri)
        textToSpeech?.stop()
        onSpeakingStateChange(false, true)
    }
}

fun initializeTextToSpeech(context: Context, onInitialized: (Boolean) -> Unit): TextToSpeech {
    var textToSpeech: TextToSpeech? = null
    textToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            onInitialized(true)
        } else {
            Log.e("TextToSpeech", "Initialization failed")
            onInitialized(false)
        }
    }
    return textToSpeech
}

@Composable
fun SeeCodeButton(context: Context) {
    val githubLink =
        "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-video-summarization"
    Button(onClick = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubLink))
        context.startActivity(intent)
    }) {
        Icon(Icons.Filled.Code, contentDescription = "See code")
        Text(
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 12.sp,
            text = stringResource(R.string.see_code),
        )
    }
}
