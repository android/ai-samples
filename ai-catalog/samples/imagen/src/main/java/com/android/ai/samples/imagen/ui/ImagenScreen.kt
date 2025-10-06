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
package com.android.ai.samples.imagen.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.imagen.R
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.TextInput

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImagenScreen(viewModel: ImagenViewModel = hiltViewModel()) {
    val uiState: ImagenUIState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is ImagenUIState.Error) {
        Toast.makeText(LocalContext.current, (uiState as ImagenUIState.Error).message, Toast.LENGTH_SHORT).show()
    }

    ImagenScreen(
        uiState = uiState,
        onGenerateClick = viewModel::generateImage,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun ImagenScreen(uiState: ImagenUIState, onGenerateClick: (String) -> Unit) {
    val isGenerating = uiState is ImagenUIState.Loading
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.title_image_generation_screen),
                sampleDescription = stringResource(R.string.subtitle_image_generation_screen),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/imagen",
                onBackClick = { backDispatcher?.onBackPressed() },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) { innerPadding ->

        val context = LocalContext.current
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

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .padding(16.dp)
                    .imePadding()
                    .widthIn(max = 440.dp)
                    .fillMaxHeight(0.85f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(40.dp),
                    )
                    .clip(RoundedCornerShape(40.dp))
                    .background(ShaderBrush(imageShader)),
                contentAlignment = Alignment.Center,
            ) {

                when (uiState) {
                    is ImagenUIState.ImageGenerated -> Image(
                        bitmap = uiState.bitmap.asImageBitmap(),
                        contentDescription = uiState.contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ImagenUIState.Loading -> {
                        ContainedLoadingIndicator(
                            modifier = Modifier.size(60.dp)
                                .align(Alignment.Center),
                        )
                    }
                    else -> {}
                }

                val textFieldState = rememberTextFieldState()
                val keyboardController = LocalSoftwareKeyboardController.current

                TextInput(
                    state = textFieldState,
                    placeholder = stringResource(R.string.placeholder_prompt),
                    primaryButton = {
                        GenerateButton(
                            text = "",
                            icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_img),
                            modifier = Modifier
                                .width(72.dp)
                                .height(55.dp)
                                .padding(4.dp),
                            enabled = !isGenerating,
                            onClick = {
                                onGenerateClick(textFieldState.text.toString())
                                keyboardController?.hide()
                            },
                        )
                    },
                    modifier = Modifier
                        .widthIn(max = 646.dp)
                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                        .align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ImagenScreenPreview() {
    AISampleCatalogTheme {
        ImagenScreen(
            uiState = ImagenUIState.Initial,
            onGenerateClick = {},
        )
    }
}
