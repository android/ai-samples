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
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.geminimultimodal.R

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GeminiMultimodalScreen(viewModel: GeminiMultimodalViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val promptPlaceHolder = stringResource(id = R.string.geminimultimodal_prompt_placeholder)
    var editTextValue by remember {
        mutableStateOf(promptPlaceHolder)
    }

    // Get the picture taken by the camera
    val cameraLauncher = rememberLauncherForActivityResult(TakePicturePreview()) { result ->
        result?.let {
            bitmap = it
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
                    Text(text = stringResource(id = R.string.geminimultimodal_title_bar))
                },
                actions = {
                    SeeCodeButton(context)
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(12.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            Card(
                modifier = Modifier
                    .size(
                        width = 450.dp,
                        height = 450.dp,
                    ),
            ) {
                val currentBitmap = bitmap
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.geminimultimodal_take_a_picture),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        cameraLauncher.launch(null)
                    },
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextField(
                value = editTextValue,
                onValueChange = { editTextValue = it },
                label = { Text("Prompt") },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val currentBitmap = bitmap
                    if (currentBitmap != null) {
                        viewModel.generate(currentBitmap, editTextValue)
                    }
                },
                enabled = uiState !is GeminiMultimodalUiState.Loading && bitmap != null,
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "Robot")
                Text(modifier = Modifier.padding(start = 8.dp), text = "Generate")
            }
            Spacer(
                modifier = Modifier
                    .height(24.dp),
            )

            when (uiState) {
                is GeminiMultimodalUiState.Initial -> {
                    Text(
                        text = stringResource(id = R.string.geminimultimodal_generation_placeholder),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is GeminiMultimodalUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is GeminiMultimodalUiState.Success -> {
                    Text(
                        text = (uiState as GeminiMultimodalUiState.Success).generatedText,
                    )
                }
                is GeminiMultimodalUiState.Error -> {
                    Text(
                        text = (uiState as GeminiMultimodalUiState.Error).errorMessage ?: stringResource(R.string.unknown_error),
                    )
                }
            }
        }
    }
}

@Composable
fun SeeCodeButton(context: Context) {
    val githubLink = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-multimodal"
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubLink))
            context.startActivity(intent)
        },
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Icon(Icons.Filled.Code, contentDescription = "See code")
        Text(
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 12.sp,
            text = stringResource(R.string.see_code),
        )
    }
}
