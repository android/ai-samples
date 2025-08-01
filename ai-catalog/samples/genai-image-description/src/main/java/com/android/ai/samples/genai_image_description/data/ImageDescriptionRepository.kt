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
package com.android.ai.samples.genai_image_description.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.android.ai.samples.geminimultimodal.R
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.imagedescription.ImageDescriber
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ImageDescriptionRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private var imageDescriber: ImageDescriber? = null

    fun describeImage(imageUri: Uri): Flow<String> = callbackFlow {
        val imageDescriberOptions = ImageDescriberOptions.builder(context).build()
        imageDescriber = ImageDescription.getClient(imageDescriberOptions)

        val job = launch {
            imageDescriber?.let { imageDescriber ->
                val featureStatus = imageDescriber.checkFeatureStatus().await()

                if (featureStatus == FeatureStatus.UNAVAILABLE) {
                    trySend(context.getString(R.string.genai_image_description_not_available))
                    close()
                    return@launch
                }

                if (featureStatus == FeatureStatus.DOWNLOADABLE ||
                    featureStatus == FeatureStatus.DOWNLOADING
                ) {
                    trySend(context.getString(R.string.genai_image_description_downloading))
                }

                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                val request = ImageDescriptionRequest.builder(bitmap).build()
                imageDescriber.runInference(request) { newText ->
                    trySend(newText)
                }
            }
        }

        awaitClose {
            job.cancel()
            imageDescriber?.close()
        }
    }
}
