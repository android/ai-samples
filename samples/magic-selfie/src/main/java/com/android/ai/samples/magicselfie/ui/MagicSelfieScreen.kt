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
package com.android.ai.samples.magicselfie.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.ai.samples.magicselfie.R
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.PrimaryButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SecondaryButton
import com.android.ai.uicomponent.TextInput
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MagicSelfieScreen(viewModel: MagicSelfieViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selfieBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tempSelfiePhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraIntent = remember {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra("android.intent.extras.CAMERA_FACING", android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT)
            putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    val resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            tempSelfiePhotoFile?.let { file ->
                val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                selfieBitmap = rotateImageIfRequired(
                    file,
                    bitmap,
                )
            }
        }
    }

    if (uiState is MagicSelfieUiState.Error) {
        val errorMessage = (uiState as MagicSelfieUiState.Error).message ?: context.getString(R.string.unknown_error)
        LaunchedEffect(uiState) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.resetError()
        }
    }

    MagicSelfieScreen(
        uiState = uiState,
        selfieBitmap = selfieBitmap,
        snackbarHostState = snackbarHostState,
        onGenerateClick = viewModel::createMagicSelfie,
        onTakePictureClick = {
            val tempFile = File.createTempFile("tmp_selfie_picture", ".jpg", context.cacheDir)
            tempSelfiePhotoFile = tempFile
            val tempSelfiePhotoUri = FileProvider.getUriForFile(context, context.packageName + ".provider", tempFile)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempSelfiePhotoUri)
            resultLauncher.launch(cameraIntent)
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun MagicSelfieScreen(
    uiState: MagicSelfieUiState,
    selfieBitmap: Bitmap?,
    snackbarHostState: SnackbarHostState,
    onGenerateClick: (Bitmap, String) -> Unit,
    onTakePictureClick: () -> Unit,
) {
    val context = LocalContext.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.magic_selfie_title),
                sampleDescription = stringResource(R.string.magic_selfie_subtitle),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/magic-selfie",
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

            if (selfieBitmap == null) {
                PrimaryButton(
                    text = stringResource(R.string.add_image),
                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_img),
                    modifier = Modifier
                        .height(96.dp)
                        .padding(start = 24.dp, end = 24.dp)
                        .align(Alignment.Center),
                    onClick = onTakePictureClick,
                )
            } else {
                val displayBitmap = if (uiState is MagicSelfieUiState.Success) {
                    uiState.bitmap
                } else {
                    selfieBitmap
                }

                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                val textFieldState = rememberTextFieldState()
                val keyboardController = LocalSoftwareKeyboardController.current

                if (uiState is MagicSelfieUiState.GeneratingBackground) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.size(60.dp)
                            .align(Alignment.Center),
                    )
                }

                TextInput(
                    state = textFieldState,
                    placeholder = stringResource(R.string.prompt_placeholder),
                    modifier = Modifier
                        .padding(10.dp)
                        .height(80.dp)
                        .align(Alignment.BottomCenter),
                    primaryButton = {
                        GenerateButton(
                            text = "",
                            icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_bg),
                            enabled = textFieldState.text.isNotEmpty() &&
                                (uiState !is MagicSelfieUiState.RemovingBackground) &&
                                (uiState !is MagicSelfieUiState.GeneratingBackground),
                        ) {
                            onGenerateClick(selfieBitmap, textFieldState.text.toString())
                            keyboardController?.hide()
                        }
                    },
                    secondaryButton = {
                        SecondaryButton(
                            text = "",
                            icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_img),
                            enabled = (uiState !is MagicSelfieUiState.RemovingBackground) &&
                                (uiState !is MagicSelfieUiState.GeneratingBackground),
                            onClick = onTakePictureClick,
                        )
                    },
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MagicSelfieScreenPreview() {
    AISampleCatalogTheme {
        MagicSelfieScreen(
            uiState = MagicSelfieUiState.Initial,
            selfieBitmap = null,
            snackbarHostState = remember { SnackbarHostState() },
            onGenerateClick = { _, _ -> },
            onTakePictureClick = {},
        )
    }
}
