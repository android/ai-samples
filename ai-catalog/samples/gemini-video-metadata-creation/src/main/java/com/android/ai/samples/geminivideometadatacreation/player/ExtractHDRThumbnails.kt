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

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExperimentalFrameExtractor
import com.android.ai.samples.geminivideometadatacreation.util.convertCommaSeparatedTimeStringsToTimestamps
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
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@UnstableApi
suspend fun extractFrame(context: Context, videoUri: Uri, timestamps: Long): Bitmap? {
    val mediaItem = MediaItem.fromUri(videoUri)

    return try {
        withContext(Dispatchers.IO) {
            // Enable HDR frames fi=or better image quality
            val configuration =
                ExperimentalFrameExtractor.Configuration.Builder().setExtractHdrFrames(true).build()
            val frameExtractor = ExperimentalFrameExtractor(
                context,
                configuration,
            )

            frameExtractor.setMediaItem(mediaItem, listOf())

            val frame = frameExtractor.getFrame(timestamps).await()
            frameExtractor.release()
            return@withContext frame.bitmap
        }
    } catch (e: Exception) {
        Log.e("extractFrame", "Error extracting frame", e)
        return null
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@UnstableApi
suspend fun extractListOfThumbnails(context: Context, videoUri: Uri, outputContent: String): List<Bitmap> {

    val timestamps: List<Long> = convertCommaSeparatedTimeStringsToTimestamps(outputContent)

    return withContext(Dispatchers.IO) {
        timestamps.mapNotNull { timestamp ->
            extractFrame(context, videoUri, timestamp)
        }
    }
}
