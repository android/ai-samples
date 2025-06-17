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
package com.android.ai.samples.magicselfie

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MagicSelfieScreen(viewModel: MagicSelfieViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)

    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT)
    cameraIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
    cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
    val currentContext = LocalContext.current
    val tempSelfiePhoto = File.createTempFile("tmp_selfie_picture", ".jpg", currentContext.cacheDir)
    val tempSelfiePhotoUri = FileProvider.getUriForFile(currentContext, currentContext.packageName + ".provider", tempSelfiePhoto)

    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempSelfiePhotoUri)
    cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    var selfieBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val progress by viewModel.progress.observeAsState(null)
    val generatedBitmap by viewModel.foregroundBitmap.collectAsState()
    var editTextValue by remember { mutableStateOf("A very scenic view from the edge of the grand canyon") }

    val resultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                selfieBitmap = rotateImageIfRequired(
                    tempSelfiePhoto,
                    MediaStore.Images.Media.getBitmap(currentContext.contentResolver, tempSelfiePhotoUri),
                )
            }
        }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(text = stringResource(id = R.string.magic_selfie))
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Card(
                modifier = Modifier
                    .size(
                        width = 450.dp,
                        height = 450.dp,
                    ),
            ) {

                if (generatedBitmap != null) {
                    Image(
                        bitmap = generatedBitmap!!.asImageBitmap(),
                        contentDescription = "Picture",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (selfieBitmap != null) {
                    Image(
                        bitmap = selfieBitmap!!.asImageBitmap(),
                        contentDescription = "Picture",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        resultLauncher.launch(cameraIntent)
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

            Button(
                modifier = Modifier.padding(vertical = 8.dp),
                onClick = {
                    if (selfieBitmap != null) {
                        viewModel.createMagicSelfie(selfieBitmap!!, editTextValue)
                    }
                },
                enabled = progress == null,
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "Robot")
                Text(modifier = Modifier.padding(start = 8.dp), text = "Generate")
            }

            if (progress != null) {
                Spacer(
                    modifier = Modifier
                        .height(30.dp)
                        .padding(12.dp),
                )
                Text(
                    text = progress!!,
                )
            }
        }
    }
}

fun rotateImageIfRequired(imageFile: File, bitmap: Bitmap): Bitmap {
    val ei = ExifInterface(imageFile.absolutePath)
    val orientation = ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL,
    )

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipImage(bitmap, true, false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipImage(bitmap, false, true)
        ExifInterface.ORIENTATION_TRANSPOSE -> flipImage(rotateImage(bitmap, 90f), true, false)
        ExifInterface.ORIENTATION_TRANSVERSE -> flipImage(rotateImage(bitmap, 270f), true, false)
        else -> bitmap
    }
}

fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun flipImage(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
    val matrix = Matrix()
    val scaleX = if (horizontal) -1f else 1f
    val scaleY = if (vertical) -1f else 1f
    matrix.setScale(scaleX, scaleY)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
fun SeeCodeButton(context: Context) {
    val githubLink = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/magic-selfie"
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
