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
import android.graphics.Canvas as AndroidCanvas // Keep this alias
import android.graphics.Paint as AndroidPaint // Keep this alias
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Path // Keep this import
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap

@Composable
fun ImageMaskEditor(sourceBitmap: Bitmap, onMaskGenerated: (source: Bitmap, mask: Bitmap) -> Unit) {
    // This will hold the path currently being drawn.
    var currentDrawingPath by remember { mutableStateOf(Path()) }
    // This state is used to trigger recomposition of the Canvas.
    var pathVersion by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = sourceBitmap.asImageBitmap(),
            contentDescription = "Source image to edit",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentDrawingPath = Path().apply {
                                // Start a brand new path
                                moveTo(offset.x, offset.y)
                            }
                            pathVersion++ // Trigger recomposition
                        },
                        onDrag = { change, _ ->
                            currentDrawingPath.lineTo(change.position.x, change.position.y)
                            pathVersion++ // Trigger recomposition by changing this state value
                            change.consume()
                        },
                        onDragEnd = {
                            // The currentDrawingPath is complete for this stroke
                        },
                    )
                },
        ) {
            // Read pathVersion to ensure recomposition when it changes
            val pathForDrawing = currentDrawingPath.apply { } // Simple way to use pathVersion indirectly
            if (!pathForDrawing.isEmpty) {
                drawPath(
                    path = pathForDrawing,
                    color = Color.White,
                    style = Stroke(
                        width = 40f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        Button(
            onClick = {
                val maskBitmap = createMaskBitmap(
                    sourceBitmap.width,
                    sourceBitmap.height,
                    currentDrawingPath,
                )
                onMaskGenerated(sourceBitmap, maskBitmap)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) {
            Text("Apply Generative Edit")
        }
    }
}

/**
 * Creates a mask Bitmap from the user's drawing path.
 * The mask is black everywhere except for the white area drawn by the user.
 */
private fun createMaskBitmap(width: Int, height: Int, composePath: Path?): Bitmap {
    val maskBitmap = createBitmap(width, height)
    val canvas = AndroidCanvas(maskBitmap)
    canvas.drawColor(android.graphics.Color.BLACK)

    composePath?.let {
        if (!it.isEmpty) {
            val androidPath = it.asAndroidPath()
            val paint = AndroidPaint().apply {
                color = android.graphics.Color.WHITE
                isAntiAlias = true
                style = AndroidPaint.Style.STROKE
                strokeWidth = 40f // Ensure this thickness is intended for the mask
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }
            canvas.drawPath(androidPath, paint)
        }
    }
    return maskBitmap
}
