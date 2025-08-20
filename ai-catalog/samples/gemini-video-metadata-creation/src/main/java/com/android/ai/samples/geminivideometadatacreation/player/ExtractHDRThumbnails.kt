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
package com.android.ai.samples.geminivideometadatacreation.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExperimentalFrameExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

/**
 *  Extracts a single video frame as a HDR [Bitmap] at a specific timestamp.
 *  *
 *  * This function uses the experimental [ExperimentalFrameExtractor] from Media3 and is designed
 *  * to run on a background thread. It handles the creation, configuration, and release
 *  * of the extractor.
 *  *
 */
@UnstableApi
@SuppressLint("UnsafeOptInUsageError", "NewApi")
suspend fun extractFrame(context: Context, videoUri: Uri, timestamps: Long): Bitmap? {
    val mediaItem = MediaItem.fromUri(videoUri)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return try {
            withContext(Dispatchers.IO) {
                // Enable HDR frames for better image quality
                val configuration = ExperimentalFrameExtractor.Configuration.Builder().setExtractHdrFrames(true).build()
                val frameExtractor = ExperimentalFrameExtractor(
                    context,
                    configuration,
                )

                frameExtractor.setMediaItem(mediaItem, listOf())
                try {
                    frameExtractor.setMediaItem(mediaItem, listOf())
                    val frame = frameExtractor.getFrame(timestamps).await()
                    return@withContext frame.bitmap
                } finally {
                    frameExtractor.release()
                }
            }
        } catch (e: Exception) {
            Log.e("extractFrame", "Error extracting frame", e)
            return null
        }
    } else {
        Log.e("extractFrame", "HDR thumbnails only supported on Android 14 and above")
        return null
    }
}

@OptIn(UnstableApi::class)
suspend fun extractListOfThumbnails(context: Context, videoUri: Uri, timestamps: List<Long>): List<Bitmap> {
    return withContext(Dispatchers.IO) {
        timestamps.mapNotNull { timestamp ->
            extractFrame(context, videoUri, timestamp)
        }
    }
}
