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
package com.android.ai.samples.geminivideometadatacreation.ui

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.android.ai.samples.geminivideometadatacreation.R
import com.android.ai.samples.geminivideometadatacreation.util.sampleVideoList
import com.android.ai.samples.geminivideometadatacreation.viewmodel.MetadataCreationState
import com.android.ai.samples.geminivideometadatacreation.viewmodel.MetadataType
import com.android.ai.samples.geminivideometadatacreation.viewmodel.VideoMetadataCreationViewModel
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.VideoPickerData
import com.android.ai.uicomponent.VideoPickerDropdown
import com.android.ai.uicomponent.VideoPlayer

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

    LifecycleStartEffect(viewModel) {
        viewModel.createPlayer()
        onStopOrDispose { viewModel.releasePlayer() }
    }

    VideoMetadataCreationScreen(
        player = uiState.player,
        selectedVideoUri = uiState.selectedVideoUri,
        selectedMetadataType = uiState.selectedMetadataType,
        metadataCreationState = uiState.metadataCreationState,
        onVideoSelected = { uri: Uri ->
            viewModel.onVideoSelected(uri)
            viewModel.resetMetadataState()
        },
        onDismissError = viewModel::dismissError,
        onMetadataTypeClicked = { type: MetadataType ->
            viewModel.onMetadataTypeSelected(type)
            viewModel.generateMetadata(type)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoMetadataCreationScreen(
    player: Player?,
    selectedVideoUri: Uri?,
    selectedMetadataType: MetadataType?,
    metadataCreationState: MetadataCreationState,
    onVideoSelected: (Uri) -> Unit,
    onDismissError: () -> Unit,
    onMetadataTypeClicked: (MetadataType) -> Unit,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.video_metadata_creation_title),
                sampleDescription = stringResource(R.string.video_metadata_creation_title),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-video-metadata-creation",
                onBackClick = { backDispatcher?.onBackPressed() },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoPlayer(
                player = player,
                videoPicker = {
                    VideoPickerDropdown(
                        videoItems = sampleVideoList.map { VideoPickerData(context.getString(it.titleResId), it.uri) },
                        selectedVideo = selectedVideoUri,
                        isExpanded = isDropdownExpanded,
                        onDropdownExpandedChanged = { isDropdownExpanded = it },
                        onVideoSelected = { onVideoSelected(it.uri) },
                    )
                },
                forceShowControls = isDropdownExpanded,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.create), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            MetadataCreationSection(
                selectedMetadataType = selectedMetadataType,
                metadataCreationState = metadataCreationState,
                onDismissError = onDismissError,
                onMetadataTypeClicked = onMetadataTypeClicked,
            )
        }
    }
}

@Composable
private fun MetadataCreationSection(
    selectedMetadataType: MetadataType?,
    metadataCreationState: MetadataCreationState,
    onDismissError: () -> Unit,
    onMetadataTypeClicked: (MetadataType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ButtonRow(
            selectedMetadataType = selectedMetadataType,
            onMetadataCreationClicked = onMetadataTypeClicked,
        )
        Spacer(Modifier.height(16.dp))

        when (val metadataCreationState = metadataCreationState) {
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

            is MetadataCreationState.Success -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                        .padding(16.dp),
                ) {
                    metadataCreationState.generatedUi()
                }
            }

            MetadataCreationState.Idle -> {
                // Default state - No button is selected unless explicitly selected
            }
        }
    }
}

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoMetadataCreationScreenPreview() {
    VideoMetadataCreationScreen(
        player = null,
        selectedVideoUri = null,
        selectedMetadataType = MetadataType.DESCRIPTION,
        metadataCreationState = MetadataCreationState.Idle,
        onVideoSelected = {},
        onDismissError = {},
        onMetadataTypeClicked = {},
    )
}

@Preview
@Composable
fun MetadataCreationSectionPreview() {
    MetadataCreationSection(
        selectedMetadataType = MetadataType.DESCRIPTION,
        metadataCreationState = MetadataCreationState.Success(
            { Box(Modifier.size(100.dp).background(Color.Red)) },
        ),
        onDismissError = {},
        onMetadataTypeClicked = {},
    )
}
