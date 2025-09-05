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
package com.android.ai.samples.geminivideometadatacreation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.geminivideometadatacreation.player.VideoPlayer
import com.android.ai.samples.geminivideometadatacreation.player.VideoSelectionDropdown
import com.android.ai.samples.geminivideometadatacreation.ui.ButtonGrid
import com.android.ai.samples.geminivideometadatacreation.util.sampleVideoList
import com.android.ai.samples.geminivideometadatacreation.viewmodel.MetadataCreationState
import com.android.ai.samples.geminivideometadatacreation.viewmodel.MetadataType
import com.android.ai.samples.geminivideometadatacreation.viewmodel.VideoMetadataCreationState
import com.android.ai.samples.geminivideometadatacreation.viewmodel.VideoMetadataCreationViewModel

/**
 * Composable function for the AI Video Metadata Creation screen.
 *
 * This screen allows users to select a video, play it, and generate metadata of its content
 * using Firebase AI. It also provides text-to-speech functionality to read out
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoMetadataCreationScreen(viewModel: VideoMetadataCreationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isDropdownExpanded by remember { mutableStateOf(false) }

    LifecycleStartEffect(viewModel) {
        viewModel.createPlayer()
        onStopOrDispose { viewModel.releasePlayer() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(text = stringResource(R.string.video_metadata_creation_title))
                },
                actions = {
                    SeeCodeButton()
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoSelectionDropdown(
                selectedVideoUri = uiState.selectedVideoUri,
                isDropdownExpanded = isDropdownExpanded,
                videoOptions = sampleVideoList,
                onVideoUriSelected = { uri ->
                    viewModel.onVideoSelected(uri)
                    viewModel.resetMetadataState()
                },
                onDropdownExpanded = { isDropdownExpanded = it },
            )

            VideoPlayer(
                player = uiState.player,
                modifier = Modifier.aspectRatio(16f / 9f),
            )

            MetadataCreationSection(
                uiState = uiState,
                onDismissError = { viewModel.dismissError() },
                onMetadataTypeClicked = {
                    viewModel.onMetadataTypeSelected(it)
                    viewModel.generateMetadata(it)
                },
            )
        }
    }
}

@Composable
private fun MetadataCreationSection(
    uiState: VideoMetadataCreationState,
    onDismissError: () -> Unit,
    onMetadataTypeClicked: (MetadataType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        ButtonGrid(
            selectedMetadataType = uiState.selectedMetadataType,
            onMetadataCreationClicked = onMetadataTypeClicked,
        )

        when (val metadataCreationState = uiState.metadataCreationState) {
            is MetadataCreationState.InProgress -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            is MetadataCreationState.Error -> {
                AlertDialog(
                    onDismissRequest = onDismissError,
                    title = { Text("Error") },
                    text = { Text(metadataCreationState.message) },
                    confirmButton = {
                        Button(onClick = onDismissError) {
                            Text("OK")
                        }
                    },
                )
            }

            is MetadataCreationState.Success -> metadataCreationState.generatedUi()

            MetadataCreationState.Idle -> {
                // Default state - No button is selected unless explicitly selected
            }
        }
    }
}

@Composable
fun SeeCodeButton() {
    val context = LocalContext.current
    val githubLink =
        "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-video-metadata-creation"
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
