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
package com.android.ai.samples.geminivideometadatacreation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.android.ai.samples.geminivideometadatacreation.viewmodel.ThumbnailState

/**
 * A Composable that displays the UI related to video thumbnail generation.
 *
 * This screen observes the [ThumbnailState] and renders the appropriate UI:
 * - A loading indicator when thumbnails are being generated.
 * - A horizontal row of generated images on success.
 * - An error message if the generation fails.
 */
@Composable
fun ThumbnailScreen(thumbnailState: ThumbnailState) {

    when (thumbnailState) {

        is ThumbnailState.Loading -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is ThumbnailState.Success -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                thumbnailState.bitmaps.forEach { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                }
            }
        }

        is ThumbnailState.Error -> {
            Text(text = thumbnailState.message)
        }

        ThumbnailState.Idle -> {
            // Empty state - Thumbnails should only be shown when Thumbnail button is selected
        }
    }
}
