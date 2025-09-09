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
import com.google.firebase.ai.type.Dimensions
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
import com.google.firebase.ai.type.toImagenInlineImage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A data source that provides methods for interacting with the Firebase Imagen API
 * for various image generation and editing tasks.
 *
 * This class encapsulates the logic for initializing Imagen models and calling
 * their respective functions for image generation, inpainting, outpainting, and style transfer.
 * It leverages the Firebase AI SDK for seamless integration with Vertex AI backends.
 *
 * Note: This class uses `@OptIn(PublicPreviewAPI::class)` as Imagen features
 * are currently in public preview.
 */
@Singleton
class ImagenEditingDataSource @Inject constructor() {
    private companion object {
        const val IMAGEN_MODEL_NAME = "imagen-4.0-ultra-generate-001"
        const val IMAGEN_EDITING_MODEL_NAME = "imagen-3.0-capability-001"
        const val DEFAULT_EDIT_STEPS = 50
        const val DEFAULT_STYLE_STRENGTH = 1
    }

    @OptIn(PublicPreviewAPI::class)
    private val imagenModel =
        Firebase.ai(backend = GenerativeBackend.vertexAI()).imagenModel(
            IMAGEN_MODEL_NAME,
            generationConfig = ImagenGenerationConfig(
                numberOfImages = 1,
                aspectRatio = ImagenAspectRatio.SQUARE_1x1,
                imageFormat = ImagenImageFormat.jpeg(compressionQuality = 75),
            ),
        )

    @OptIn(PublicPreviewAPI::class)
    private val editingModel =
        Firebase.ai(backend = GenerativeBackend.vertexAI()).imagenModel(
            IMAGEN_EDITING_MODEL_NAME,
            generationConfig = ImagenGenerationConfig(
                numberOfImages = 1,
                aspectRatio = ImagenAspectRatio.SQUARE_1x1,
                imageFormat = ImagenImageFormat.jpeg(compressionQuality = 75),
            ),
        )

    /**
     * Generates an image based on the provided prompt.
     *
     * This function uses the Imagen model to generate an image from a textual description.
     * It returns the generated image as a Bitmap.
     *
     * @param prompt The textual description to generate the image from.
     * @return The generated image as a [Bitmap].
     * @throws Exception if the image generation fails.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun generateImage(prompt: String): Bitmap {
        val imageResponse = imagenModel.generateImages(
            prompt = prompt,
        )
        val image = imageResponse.images.first()
        return image.asBitmap()
    }

    /**
     * Performs inpainting on a source image using a provided mask and prompt.
     *
     * This function utilizes the Imagen editing model to fill in the masked areas
     * of the source image based on the textual prompt.
     *
     * @param sourceImage The original image to be inpainted.
     * @param maskImage A bitmap representing the mask, where white areas indicate
     *                  regions to be inpainted and black areas indicate regions to be preserved.
     * @param prompt A textual description of what should be generated in the masked areas.
     * @param editSteps The number of editing steps to perform. Defaults to `DEFAULT_EDIT_STEPS`.
     * @return A [Bitmap] representing the inpainted image.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun inpaintImageWithMask(sourceImage: Bitmap, maskImage: Bitmap, prompt: String, editSteps: Int = DEFAULT_EDIT_STEPS): Bitmap {
        val imageResponse = editingModel.editImage(
            referenceImages = listOf(
                ImagenRawImage(sourceImage.toImagenInlineImage()),
                ImagenRawMask(maskImage.toImagenInlineImage()),
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
     * Inpaints the background of a source image using a text prompt.
     *
     * This function utilizes the Imagen model's inpainting capability to modify the background
     * of the provided [sourceImage] based on the [prompt]. The [editSteps] parameter
     * controls the intensity of the inpainting process.
     *
     * @param sourceImage The original bitmap image to be modified.
     * @param prompt A text description of the desired background.
     * @param editSteps The number of editing steps to perform (default is 50).
     *                  Higher values may lead to more significant changes.
     * @return A [Bitmap] object representing the image with the inpainted background.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun inpaintBackgroundImage(sourceImage: Bitmap, prompt: String, editSteps: Int = 50): Bitmap {
        val imageResponse = editingModel.inpaintImage(
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
     * Outpaints an image to the target dimensions using the Firebase Imagen API.
     * This function extends the original image by generating content around it
     * based on the provided prompt and target dimensions.
     *
     * @param sourceImage The original bitmap image to be outpainted.
     * @param targetDimensions The desired dimensions of the outpainted image.
     * @param prompt An optional text prompt to guide the outpainting process.
     * @return The outpainted bitmap image.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun outpaintImageSimple(sourceImage: Bitmap, targetDimensions: Dimensions, prompt: String = ""): Bitmap {
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
     * Transfers the style from a style image to a source image.
     *
     * This function uses the Imagen model to apply the artistic style of the [styleImage]
     * to the [sourceImage]. The process is guided by the [prompt] and [styleGuidanceText],
     * with the intensity of the style transfer controlled by [styleStrength]. The [editSteps]
     * parameter determines the number of refinement steps in the style transfer process.
     *
     * @param sourceImage The original image to which the style will be applied.
     * @param styleImage The image from which the artistic style will be extracted.
     * @param styleStrength An integer controlling the intensity of the style transfer.
     *                      Defaults to `DEFAULT_STYLE_STRENGTH`. Higher values result in a stronger style.
     * @param styleGuidanceText Text that provides additional guidance on how the style should be applied.
     * @param prompt A text prompt describing the desired outcome of the style transfer.
     * @param editSteps The number of editing steps to perform during the style transfer.
     *                  Defaults to 50. More steps can lead to a more refined result.
     * @return A [Bitmap] representing the [sourceImage] with the style of the [styleImage] applied.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun transferStyle(
        sourceImage: Bitmap,
        styleImage: Bitmap,
        styleStrength: Int = DEFAULT_STYLE_STRENGTH,
        styleGuidanceText: String,
        prompt: String,
        editSteps: Int = 50,
    ): Bitmap {
        val imageResponse = imagenModel.editImage(
            referenceImages = listOf(
                ImagenStyleReference(
                    styleImage.toImagenInlineImage(),
                    styleStrength,
                    styleGuidanceText,
                ),
            ),
            prompt = prompt,
            config = ImagenEditingConfig(
                editSteps = editSteps,
            ),
        )
        return imageResponse.images.first().asBitmap()
    }
}
