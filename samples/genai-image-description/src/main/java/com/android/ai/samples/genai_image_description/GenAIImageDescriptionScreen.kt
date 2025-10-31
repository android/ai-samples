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

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.android.ai.samples.geminimultimodal.R
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.theme.extendedColorScheme
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.PrimaryButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.UndoButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenAIImageDescriptionScreen(viewModel: GenAIImageDescriptionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let {
            imageUri = it
        }
    }

    GenAIImageDescriptionScreen(
        uiState = uiState,
        imageUri = imageUri,
        onGenerateClick = viewModel::getImageDescription,
        onImagePickerClick = {
            photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        },
        onClearClick = {
            viewModel.clearGeneratedText()
            imageUri = null
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GenAIImageDescriptionScreen(
    uiState: GenAIImageDescriptionUiState,
    imageUri: Uri?,
    onGenerateClick: (Uri?) -> Unit,
    onImagePickerClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.genai_image_description_title),
                sampleDescription = stringResource(R.string.genai_image_description_subtitle),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/genai-image-description",
                onBackClick = { backDispatcher?.onBackPressed() },
            )
        },
    ) { innerPadding ->
        val imageBitmap = remember {
            val bitmap = BitmapFactory.decodeResource(context.resources, com.android.ai.uicomponent.R.drawable.img_fill)
            bitmap.asImageBitmap()
        }
        val imageShader = remember {
            ImageShader(
                image = imageBitmap,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
            )
        }

        val roundedCornerShape = RoundedCornerShape(40.dp)

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .padding(16.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        shape = roundedCornerShape,
                    )
                    .clip(roundedCornerShape)
                    .widthIn(max = 646.dp)
                    .fillMaxSize()
                    .background(ShaderBrush(imageShader)),
                contentAlignment = Alignment.Center,
            ) {

                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (uiState is GenAIImageDescriptionUiState.Initial) {
                        GenerateButton(
                            text = stringResource(R.string.genai_image_description_run_inference),
                            icon = painterResource(com.android.ai.uicomponent.R.drawable.ic_ai_edit),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 24.dp, bottom = 24.dp),
                        ) {
                            onGenerateClick(imageUri)
                        }
                    }
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.genai_image_description_add_image),
                        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_img),
                        modifier = Modifier
                            .height(96.dp)
                            .padding(start = 24.dp, end = 24.dp)
                            .align(Alignment.Center),
                        onClick = onImagePickerClick,
                    )
                }
                if (
                    uiState !is GenAIImageDescriptionUiState.Initial &&
                    uiState !is GenAIImageDescriptionUiState.CheckingFeatureStatus
                ) {
                    val outputText = when (val state = uiState) {
                        is GenAIImageDescriptionUiState.DownloadingFeature -> stringResource(
                            id = R.string.image_desc_downloading,
                            state.bytesDownloaded,
                            state.bytesToDownload,
                        )

                        is GenAIImageDescriptionUiState.Error -> stringResource(state.errorMessageStringRes)
                        is GenAIImageDescriptionUiState.Generating -> state.partialOutput
                        is GenAIImageDescriptionUiState.Success -> state.generatedOutput
                        else -> "" // Show nothing for the Initial state
                    }

                    UndoButton(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .padding(
                                top = 18.dp,
                                end = 18.dp,
                            ),
                    ) {
                        onClearClick()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        extendedColorScheme.startGradient,
                                    ),
                                ),
                            ),
                    ) {
                        Text(
                            text = outputText,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
                                .align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GenAIImageDescriptionScreenPreview() {
    AISampleCatalogTheme {
        GenAIImageDescriptionScreen(
            uiState = GenAIImageDescriptionUiState.Initial,
            imageUri = null,
            onGenerateClick = {},
            onImagePickerClick = {},
            onClearClick = {},
        )
    }
}
