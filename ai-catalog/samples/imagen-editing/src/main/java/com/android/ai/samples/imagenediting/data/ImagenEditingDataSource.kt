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
package com.android.ai.samples.imagenediting.data

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagenAspectRatio
import com.google.firebase.ai.type.ImagenBackgroundMask
import com.google.firebase.ai.type.ImagenEditMode
import com.google.firebase.ai.type.ImagenEditingConfig
import com.google.firebase.ai.type.ImagenGenerationConfig
import com.google.firebase.ai.type.ImagenImageFormat
import com.google.firebase.ai.type.ImagenMaskReference
import com.google.firebase.ai.type.ImagenRawImage
import com.google.firebase.ai.type.ImagenRawMask
import com.google.firebase.ai.type.ImagenStyleReference
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.Dimensions // Assuming Dimensions is available, if not, define a simple data class
import com.google.firebase.ai.type.toImagenInlineImage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagenEditingDataSource @Inject constructor() {
    @OptIn(PublicPreviewAPI::class)
    private val imagenModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).imagenModel(
        modelName = "imagen-4.0-generate-preview-06-06",
        generationConfig = ImagenGenerationConfig(
            numberOfImages = 1,
            aspectRatio = ImagenAspectRatio.SQUARE_1x1,
            imageFormat = ImagenImageFormat.jpeg(compressionQuality = 75),
        ),
    )

    @OptIn(PublicPreviewAPI::class)
    private val maskModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).imagenModel(
        modelName = "imagen-3.0-capability-001",
        generationConfig = ImagenGenerationConfig(
            numberOfImages = 1,
            aspectRatio = ImagenAspectRatio.SQUARE_1x1,
            imageFormat = ImagenImageFormat.jpeg(compressionQuality = 75),
        ),
    )

    @OptIn(PublicPreviewAPI::class)
    suspend fun generateImage(prompt: String): Bitmap {
        val imageResponse = imagenModel.generateImages(
            prompt = prompt,
        )
        val image = imageResponse.images.first()
        return image.asBitmap()
    }

    /**
     * Inpaints an image using a provided mask and prompt.
     * This uses the "full-featured path" allowing for explicit mask control.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun inpaintImageWithMask(
        sourceImage: Bitmap,
        maskImage: Bitmap,
        prompt: String,
        editSteps: Int = 50,
    ): Bitmap {
        val source = ImagenRawImage(sourceImage.toImagenInlineImage())
        val mask = ImagenRawMask(maskImage.toImagenInlineImage())

        val imageResponse = maskModel.editImage(
            referenceImages = listOf(
                source,
                mask,
            ),
            prompt = prompt,
            config = ImagenEditingConfig(
                editMode = ImagenEditMode.INPAINT_INSERTION,
                editSteps = editSteps,
            ),
        )
        return imageResponse.images.first().asBitmap()
    }

    /**
     * Inpaints an image by automatically detecting the background as the mask.
     * This uses the "happy path" for background inpainting.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun inpaintBackgroundImage(
        sourceImage: Bitmap,
        prompt: String,
        editSteps: Int = 50,
    ): Bitmap {
        val imageResponse = maskModel.inpaintImage(
            image = sourceImage.toImagenInlineImage(),
            prompt = prompt,
            mask = ImagenBackgroundMask(),
            config = ImagenEditingConfig(
                editMode = ImagenEditMode.INPAINT_INSERTION,
                editSteps = editSteps,
            ),
        )
        return imageResponse.images.first().asBitmap()
    }

    /**
     * Outpaints an image to the specified target dimensions.
     * This uses the "happy path" for outpainting.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun outpaintImageSimple(
        sourceImage: Bitmap,
        targetDimensions: Dimensions, // e.g., Dimensions(1024, 1024)
        prompt: String = "", // Prompt is often optional or implicit for outpainting
    ): Bitmap {
        // Note: The example shows `model.outpaintImage(...)`
        // If that specific helper isn't available, we use the general `editImage`
        // as shown in the "Outpainting full-featured path"
        val imageResponse = imagenModel.editImage(
            referenceImages = ImagenMaskReference.generateMaskAndPadForOutpainting(
                image = sourceImage.toImagenInlineImage(),
                newDimensions = targetDimensions,
            ),
            config = ImagenEditingConfig(editMode = ImagenEditMode.OUTPAINT),
            prompt = prompt,
        )
        return imageResponse.images.first().asBitmap()
    }

    /**
     * Performs style transfer on an image using a style reference image.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun transferStyle(
        sourceImage: Bitmap, // The image to apply the style to (implicitly used by the prompt)
        styleImage: Bitmap,
        styleStrength: Int = 1, // Example strength, adjust as needed
        styleGuidanceText: String, // e.g., "van gogh style"
        prompt: String, // e.g., "A cat flying through outer space, in the van gogh style[1]"
        editSteps: Int = 50,
    ): Bitmap {
        val imageResponse = imagenModel.editImage(
            referenceImages = listOf(
                // The source image for content is implied if not explicitly added as ImagenRawImage
                // The prompt will reference the style via "[1]"
                ImagenStyleReference(
                    styleImage.toImagenInlineImage(),
                    styleStrength,
                    styleGuidanceText,
                ),
            ),
            prompt = prompt, // Ensure prompt references the style image, e.g., "A photo of a dog in style [1]"
            config = ImagenEditingConfig(
                editSteps = editSteps,
            ),
        )
        return imageResponse.images.first().asBitmap()
    }
}
