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
package com.android.ai.samples.imagen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagenScreen(viewModel: ImagenViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isGenerating by viewModel.isGenerating.observeAsState(false)
    val generatedBitmap by viewModel.imageGenerated.collectAsState()

    val placeholder = stringResource(R.string.placeholder_prompt)
    var editTextValue by remember { mutableStateOf(placeholder) }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(text = stringResource(R.string.title_image_generation_screen))
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
                modifier = Modifier.size(
                    width = 400.dp,
                    height = 400.dp,
                ).align(Alignment.CenterHorizontally),
            ) {
                generatedBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Picture",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (isGenerating) {
                    Text(
                        text = stringResource(R.string.generating_label),
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextField(
                value = editTextValue,
                onValueChange = { editTextValue = it },
                label = { Text(stringResource(R.string.prompt_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                Button(
                    modifier = Modifier.padding(vertical = 8.dp),
                    onClick = {
                        viewModel.generateImage(editTextValue)
                    },
                    enabled = !isGenerating,
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "Robot")
                    Text(modifier = Modifier.padding(start = 8.dp), text = stringResource(R.string.generate_button))
                }
            }
        }
    }
}

@Composable
fun SeeCodeButton(context: Context) {
    val githubLink = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/imagen"
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
