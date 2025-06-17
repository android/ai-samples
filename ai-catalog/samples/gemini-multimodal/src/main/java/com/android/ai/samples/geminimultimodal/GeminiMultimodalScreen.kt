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
package com.android.ai.samples.geminimultimodal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GeminiMultimodalScreen(viewModel: GeminiMultimodalViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val textResponse by viewModel.textGenerated.collectAsState()
    val isGenerating by viewModel.isGenerating.observeAsState(false)
    var pictureAvailable by remember { mutableStateOf(false) }

    val promptPlaceHolder = stringResource(id = R.string.geminimultimodal_prompt_placeholder)
    var editTextValue by remember {
        mutableStateOf(promptPlaceHolder)
    }

    // Get the picture taken by the camera
    val cameraLauncher = rememberLauncherForActivityResult(TakePicturePreview()) { result ->
        result?.let {
            bitmap = it
            pictureAvailable = true
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
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
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
                    if (bitmap != null) {
                        viewModel.generate(bitmap!!, editTextValue)
                    }
                },
                enabled = !isGenerating && pictureAvailable,
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "Robot")
                Text(modifier = Modifier.padding(start = 8.dp), text = "Generate")
            }
            Spacer(
                modifier = Modifier
                    .height(24.dp),
            )

            if (isGenerating) {
                Text(
                    text = stringResource(R.string.geminimultimodal_generating),
                )
            } else {
                Text(
                    text = textResponse,
                )
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
