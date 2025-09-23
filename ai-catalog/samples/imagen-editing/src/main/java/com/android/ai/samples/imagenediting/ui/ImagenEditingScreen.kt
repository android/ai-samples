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
package com.android.ai.samples.imagenediting.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.imagenediting.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagenEditingScreen(viewModel: ImagenEditingViewModel = hiltViewModel()) {
    val uiState: ImagenEditingUIState by viewModel.uiState.collectAsStateWithLifecycle()
    val showMaskEditor: Boolean by viewModel.showMaskEditor.collectAsStateWithLifecycle()
    val bitmapForMasking: Bitmap? by viewModel.bitmapForMasking.collectAsStateWithLifecycle()

    BackHandler(enabled = showMaskEditor) {
        viewModel.onCancelMasking()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ImagenEditingScreenContent(
            uiState = uiState,
            showMaskEditor = showMaskEditor,
            bitmapForMasking = bitmapForMasking,
            onGenerateClick = viewModel::generateImage,
            onInpaintClick = { source, mask, prompt -> viewModel.inpaintImage(source, mask, prompt) },
            onImageToMaskClicked = { bitmap -> viewModel.onStartMasking(bitmap) },
            onImageMaskReady = { source, mask -> viewModel.onImageMaskReady(source, mask) },
            onCancelMasking = viewModel::onCancelMasking,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ImagenEditingScreenContent(
    uiState: ImagenEditingUIState,
    showMaskEditor: Boolean,
    bitmapForMasking: Bitmap?,
    onGenerateClick: (String) -> Unit,
    onInpaintClick: (source: Bitmap, mask: Bitmap, prompt: String) -> Unit,
    onImageToMaskClicked: (Bitmap) -> Unit,
    onImageMaskReady: (source: Bitmap, mask: Bitmap) -> Unit,
    onCancelMasking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGenerating = uiState is ImagenEditingUIState.Loading

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(text = stringResource(R.string.editing_title_image_generation_screen))
                },
                actions = {
                    SeeCodeButton()
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp)
                .imePadding(),
        ) {
            ImagenEditingGeneratedContent(
                uiState = uiState,
                showMaskEditor = showMaskEditor,
                bitmapForMasking = bitmapForMasking,
                onImageClick = {
                    if (uiState is ImagenEditingUIState.ImageGenerated) {
                        onImageToMaskClicked(it)
                    }
                },
                onMaskFinalized = onImageMaskReady,
                onCancelMasking = onCancelMasking,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            GenerationInput(
                uiState = uiState,
                onGenerateClick = onGenerateClick,
                onInpaintClick = { prompt ->
                    if (uiState is ImagenEditingUIState.ImageMasked) {
                        onInpaintClick(uiState.originalBitmap, uiState.maskBitmap, prompt)
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
        }
    }
}

@Composable
fun ImagenEditingGeneratedContent(
    uiState: ImagenEditingUIState,
    showMaskEditor: Boolean,
    bitmapForMasking: Bitmap?,
    onImageClick: (Bitmap) -> Unit,
    onMaskFinalized: (source: Bitmap, mask: Bitmap) -> Unit,
    onCancelMasking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (showMaskEditor && bitmapForMasking != null) {
            ImagenEditingMaskEditor(
                sourceBitmap = bitmapForMasking,
                onMaskFinalized = { maskBitmap ->
                    onMaskFinalized(bitmapForMasking, maskBitmap)
                },
                onCancel = onCancelMasking,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            when (uiState) {
                is ImagenEditingUIState.Loading -> {
                    CircularProgressIndicator()
                }

                is ImagenEditingUIState.ImageGenerated -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = uiState.bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.editing_generated_image),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                        Button(
                            onClick = { onImageClick(uiState.bitmap) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        ) {
                            Text(text = stringResource(R.string.editing_edit_mask_button))
                        }
                    }
                }

                is ImagenEditingUIState.ImageMasked -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = uiState.originalBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.editing_generated_image),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                        Image(
                            bitmap = uiState.maskBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.editing_generated_mask),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color.Red.copy(alpha = 0.5f)),
                        )
                    }
                }

                is ImagenEditingUIState.Error -> {
                    uiState.message?.let { Text(text = it) }
                }

                else -> {
                    Text(text = stringResource(R.string.editing_placeholder_prompt))
                }
            }
        }
    }
}
