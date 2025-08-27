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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.android.ai.samples.imagenediting.R
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint

@Composable
fun ImagenEditingGeneratedContent(
    uiState: ImagenEditingUIState,
    modifier: Modifier = Modifier,
    // onImageClick is kept if you still want an action for the base image tap
    // e.g., to clear the mask or select a different image.
    onImageClick: (Bitmap) -> Unit = {},
    // New callback for when the mask is drawn and finalized
    onMaskFinalized: (source: Bitmap, mask: Bitmap) -> Unit,
) {
    // States for drawing, moved from ImageMaskEditor
    var currentDrawingPath by remember { mutableStateOf(Path()) }
    var pathVersion by remember { mutableIntStateOf(0) }
    // To store the bitmap that is currently being masked
    var bitmapToMask by remember { mutableStateOf<Bitmap?>(null) }


    Box(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center,
    ) {
        when (uiState) {
            ImagenEditingUIState.Initial -> {
                Text(
                    text = stringResource(R.string.editing_placeholder_prompt_entry),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
                // Reset drawing path if UI state changes from a masked state
                currentDrawingPath = Path()
                pathVersion++
                bitmapToMask = null
            }

            ImagenEditingUIState.Loading -> {
                CircularProgressIndicator()
                // Reset drawing path
                currentDrawingPath = Path()
                pathVersion++
                bitmapToMask = null
            }

            is ImagenEditingUIState.ImageGenerated -> {
                // Set the bitmap that can be masked
                bitmapToMask = uiState.bitmap

                // Base Image
                Image(
                    bitmap = uiState.bitmap.asImageBitmap(),
                    contentDescription = uiState.contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            // Option 1: Clicking image starts a new mask drawing session for THIS image
                            currentDrawingPath = Path() // Reset path
                            pathVersion++
                            onImageClick(uiState.bitmap) // Notify if some other action is needed too
                        },
                )

                // Drawing Canvas overlay
                DrawingCanvas(
                    currentDrawingPath = currentDrawingPath,
                    pathVersion = pathVersion, // Pass version to trigger recomposition if canvas is separate
                    onPathUpdate = { newPath, newVersion ->
                        currentDrawingPath = newPath
                        pathVersion = newVersion
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Button to finalize the mask for the current bitmapToMask
                bitmapToMask?.let { currentSourceBitmap ->
                    Button(
                        onClick = {
                            val maskBitmap = createMaskBitmap(
                                currentSourceBitmap.width,
                                currentSourceBitmap.height,
                                currentDrawingPath,
                            )
                            onMaskFinalized(currentSourceBitmap, maskBitmap)
                            // Optionally reset the path after finalizing
                            currentDrawingPath = Path()
                            pathVersion++
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        enabled = !currentDrawingPath.isEmpty, // Enable only if something is drawn
                    ) {
                        Text(stringResource(R.string.editing_finalize_mask_button)) // Add to strings.xml
                    }
                }
            }

            is ImagenEditingUIState.ImageMasked -> {
                // Displaying original and mask overlay
                bitmapToMask = null // No active drawing on already masked image view by default

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = uiState.originalBitmap.asImageBitmap(),
                        contentDescription = uiState.contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                // Option: Clicking masked image could allow re-masking the original
                                bitmapToMask = uiState.originalBitmap
                                currentDrawingPath = Path() // Reset for new mask
                                pathVersion++
                                onImageClick(uiState.originalBitmap) // This click might now mean "prepare for re-masking"
                                // The ViewModel would then change state back to ImageGenerated or a new "ReMasking" state
                            },
                    )
                    Image(
                        bitmap = uiState.maskBitmap.asImageBitmap(),
                        contentDescription = "Mask Overlay",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.5f),
                    )
                }
            }

            is ImagenEditingUIState.Error -> {
                Text(
                    text = uiState.message ?: stringResource(R.string.editing_error_message_unknown),
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    textAlign = TextAlign.Center,
                )
                currentDrawingPath = Path()
                pathVersion++
                bitmapToMask = null
            }
        }
    }
}

@Composable
private fun DrawingCanvas(
    currentDrawingPath: Path,
    pathVersion: Int, // Used to trigger recomposition if path object itself doesn't change identity
    onPathUpdate: (Path, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var internalPath by remember(pathVersion) { mutableStateOf(currentDrawingPath) }
    var internalVersion by remember { mutableIntStateOf(pathVersion) }
    val pathToDraw = remember(internalVersion) { internalPath }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) { // Use Unit or a key that changes if you need to reset pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        internalPath = Path().apply { moveTo(offset.x, offset.y) }
                        internalVersion++
                        onPathUpdate(internalPath, internalVersion)
                    },
                    onDrag = { change, _ ->
                        // It's important to ensure currentDrawingPath is the one being modified
                        // or a new one is created based on it.
                        // For direct modification and state trigger:
                        internalPath.lineTo(change.position.x, change.position.y)
                        internalVersion++
                        onPathUpdate(internalPath, internalVersion) // Pass the modified path back
                        change.consume()
                    },
                )
            },
    ) {
        // This ensures canvas redraws when pathVersion from parent changes OR internal drawing happens

        if (!pathToDraw.isEmpty) {
            drawPath(
                path = pathToDraw,
                color = Color.White.copy(alpha = 0.7f), // Semi-transparent white for drawing
                style = Stroke(
                    width = 40f, // Stroke width for drawing
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}


// This function is now part of GeneratedContent.kt or accessible to it
private fun createMaskBitmap(width: Int, height: Int, composePath: Path?): Bitmap {
    val maskBitmap = createBitmap(width, height)
    val canvas = AndroidCanvas(maskBitmap)
    canvas.drawColor(android.graphics.Color.BLACK) // Mask is black where not drawn

    composePath?.let {
        if (!it.isEmpty) {
            val androidPath = it.asAndroidPath()
            val paint = AndroidPaint().apply {
                color = android.graphics.Color.WHITE // Drawn area is white in the mask
                isAntiAlias = true
                style = AndroidPaint.Style.STROKE
                strokeWidth = 40f // Stroke width for the actual mask bitmap
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }
            canvas.drawPath(androidPath, paint)
        }
    }
    return maskBitmap
}
