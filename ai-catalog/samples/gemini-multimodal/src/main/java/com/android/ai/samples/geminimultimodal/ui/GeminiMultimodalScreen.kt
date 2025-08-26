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
package com.android.ai.samples.geminimultimodal.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.geminimultimodal.R
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.PrimaryButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SecondaryButton
import com.android.ai.uicomponent.TextInput

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GeminiMultimodalScreen(viewModel: GeminiMultimodalViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val cameraLauncher = rememberLauncherForActivityResult(TakePicturePreview()) { result ->
        result?.let {
            bitmap = it
        }
    }

    if (uiState is GeminiMultimodalUiState.Error) {
        val errorMessage = (uiState as GeminiMultimodalUiState.Error).errorMessage
            ?: stringResource(R.string.unknown_error)
        LaunchedEffect(uiState) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.resetError()
        }
    }

    GeminiMultimodalScreen(
        uiState = uiState,
        bitmap = bitmap,
        snackbarHostState = snackbarHostState,
        onGenerateClick = viewModel::generate,
        onTakePictureClick = {
            cameraLauncher.launch(null)
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GeminiMultimodalScreen(
    uiState: GeminiMultimodalUiState,
    bitmap: Bitmap?,
    snackbarHostState: SnackbarHostState,
    onGenerateClick: (Bitmap, String) -> Unit,
    onTakePictureClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.geminimultimodal_title),
                sampleDescription = stringResource(R.string.geminimultimodal_subtitle),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-multimodal",
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

        val gradientBrush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x88000000),
            ),
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .imePadding()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(40.dp),
                )
                .clip(RoundedCornerShape(40.dp))
                .background(ShaderBrush(imageShader)),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.geminimultimodal_take_a_picture),
                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_img),
                    modifier = Modifier
                        .height(96.dp)
                        .padding(start = 24.dp, end = 24.dp)
                        .align(Alignment.Center),
                    onClick = onTakePictureClick,
                )
            }

            when (uiState) {
                is GeminiMultimodalUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is GeminiMultimodalUiState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = gradientBrush,
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = (uiState as GeminiMultimodalUiState.Success).generatedText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 104.dp),
                        )
                    }
                }

                else -> {}
            }

            val textFieldState = rememberTextFieldState()
            val keyboardController = LocalSoftwareKeyboardController.current

            TextInput(
                state = textFieldState,
                placeholder = stringResource(R.string.geminimultimodal_prompt_placeholder),
                primaryButton = {
                    GenerateButton(
                        text = "",
                        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_img),
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp),
                        enabled = uiState !is GeminiMultimodalUiState.Loading && bitmap != null,
                        onClick = {
                            if (bitmap != null) {
                                onGenerateClick(bitmap, textFieldState.text.toString())
                            }
                            keyboardController?.hide()
                        },
                    )
                },
                secondaryButton = {
                    if (bitmap != null) {
                        SecondaryButton(
                            text = "",
                            enabled = uiState !is GeminiMultimodalUiState.Loading,
                            icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_add),
                            onClick = onTakePictureClick,
                        )
                    }
                },
                modifier = Modifier
                    .padding(10.dp)
                    .height(80.dp)
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GeminiMultimodalScreenPreview() {
    AISampleCatalogTheme {
        GeminiMultimodalScreen(
            uiState = GeminiMultimodalUiState.Initial,
            bitmap = null,
            snackbarHostState = remember { SnackbarHostState() },
            onGenerateClick = { _, _ -> },
            onTakePictureClick = {}
        )
    }
}
