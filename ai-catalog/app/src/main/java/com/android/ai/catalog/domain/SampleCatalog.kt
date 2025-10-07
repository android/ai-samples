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
package com.android.ai.catalog.domain

import android.Manifest
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.ai.catalog.R
import com.android.ai.samples.geminichatbot.GeminiChatbotScreen
import com.android.ai.samples.geminiimagechat.GeminiImageChatScreen
import com.android.ai.samples.geminilivetodo.ui.TodoScreen
import com.android.ai.samples.geminimultimodal.ui.GeminiMultimodalScreen
import com.android.ai.samples.geminivideometadatacreation.ui.VideoMetadataCreationScreen
import com.android.ai.samples.geminivideosummary.ui.VideoSummarizationScreen
import com.android.ai.samples.genai_image_description.GenAIImageDescriptionScreen
import com.android.ai.samples.genai_summarization.GenAISummarizationScreen
import com.android.ai.samples.genai_writing_assistance.GenAIWritingAssistanceScreen
import com.android.ai.samples.imagen.ui.ImagenScreen
import com.android.ai.samples.imagenediting.ui.ImagenEditingScreen
import com.android.ai.samples.magicselfie.ui.MagicSelfieScreen
import com.android.ai.theme.extendedColorScheme

@RequiresPermission(Manifest.permission.RECORD_AUDIO)
val sampleCatalog = listOf(
    SampleCatalogItem(
        title = R.string.gemini_image_chat,
        description = R.string.gemini_image_chat_description,
        route = "GeminiImageChatScreen",
        sampleEntryScreen = { GeminiImageChatScreen() },
        tags = listOf(SampleTags.GEMINI_FLASH, SampleTags.FIREBASE),
        needsFirebase = true,
    ),
    SampleCatalogItem(
        title = R.string.gemini_multimodal_sample_title,
        description = R.string.gemini_multimodal_sample_description,
        route = "GeminiMultimodalScreen",
        sampleEntryScreen = { GeminiMultimodalScreen() },
        tags = listOf(SampleTags.GEMINI_FLASH, SampleTags.FIREBASE),
        needsFirebase = true,
        isFeatured = true,
        keyArt = R.drawable.img_keyart_multimodal,
    ),
    SampleCatalogItem(
        title = R.string.gemini_chatbot_sample_title,
        description = R.string.gemini_chatbot_sample_description,
        route = "GeminiChitchatScreen",
        sampleEntryScreen = { GeminiChatbotScreen() },
        tags = listOf(SampleTags.GEMINI_FLASH, SampleTags.FIREBASE),
        needsFirebase = true,
        keyArt = R.drawable.img_keyart_chatbot,
    ),
    SampleCatalogItem(
        title = R.string.genai_summarization_sample_title,
        description = R.string.genai_summarization_sample_description,
        route = "GenAISummarizationScreen",
        sampleEntryScreen = { GenAISummarizationScreen() },
        tags = listOf(SampleTags.GEMINI_NANO, SampleTags.ML_KIT),
        keyArt = R.drawable.img_keyart_summary,
    ),
    SampleCatalogItem(
        title = R.string.genai_image_description_sample_title,
        description = R.string.genai_image_description_sample_description,
        route = "GenAIImageDescriptionScreen",
        sampleEntryScreen = { GenAIImageDescriptionScreen() },
        tags = listOf(SampleTags.GEMINI_NANO, SampleTags.ML_KIT),
        keyArt = R.drawable.img_keyart_img_desc,
    ),
    SampleCatalogItem(
        title = R.string.genai_writing_assistance_sample_title,
        description = R.string.genai_writing_assistance_sample_description,
        route = "GenAIWritingAssistanceScreen",
        sampleEntryScreen = { GenAIWritingAssistanceScreen() },
        tags = listOf(SampleTags.GEMINI_NANO, SampleTags.ML_KIT),
        keyArt = R.drawable.img_keyart_text,
    ),
    SampleCatalogItem(
        title = R.string.imagen_sample_title,
        description = R.string.imagen_sample_description,
        route = "ImagenImageGenerationScreen",
        sampleEntryScreen = { ImagenScreen() },
        tags = listOf(SampleTags.IMAGEN, SampleTags.FIREBASE),
        needsFirebase = true,
        keyArt = R.drawable.img_keyart_imagen,
    ),
    SampleCatalogItem(
        title = R.string.imagen_editing_sample_title,
        description = R.string.imagen_editing_sample_description,
        route = "ImagenMaskEditing",
        sampleEntryScreen = { ImagenEditingScreen() },
        tags = listOf(SampleTags.IMAGEN, SampleTags.FIREBASE),
        needsFirebase = true,
        keyArt = R.drawable.img_keyart_imagen,
    ),
    SampleCatalogItem(
        title = R.string.magic_selfie_sample_title,
        description = R.string.magic_selfie_sample_description,
        route = "MagicSelfieScreen",
        sampleEntryScreen = { MagicSelfieScreen() },
        tags = listOf(SampleTags.IMAGEN, SampleTags.FIREBASE, SampleTags.ML_KIT),
        needsFirebase = true,
        keyArt = R.drawable.img_keyart_magic_selfie,
    ),
    SampleCatalogItem(
        title = R.string.gemini_video_summarization_sample_title,
        description = R.string.gemini_video_summarization_sample_description,
        route = "VideoSummarizationScreen",
        sampleEntryScreen = { VideoSummarizationScreen() },
        tags = listOf(SampleTags.GEMINI_FLASH, SampleTags.FIREBASE, SampleTags.MEDIA3),
        needsFirebase = true,
    ),
    SampleCatalogItem(
        title = R.string.gemini_video_metadata_creation_sample_title,
        description = R.string.gemini_video_metadata_creation_sample_description,
        route = "VideoMetadataCreationScreen",
        sampleEntryScreen = { VideoMetadataCreationScreen() },
        tags = listOf(SampleTags.GEMINI_FLASH, SampleTags.FIREBASE, SampleTags.MEDIA3),
        needsFirebase = true,
        keyArt = R.drawable.img_keyart_video_summary,
    ),
    SampleCatalogItem(
        title = R.string.gemini_live_todo_title,
        description = R.string.gemini_live_todo_description,
        route = "GeminiLiveTodoScreen",
        sampleEntryScreen = { TodoScreen() },
        tags = listOf(SampleTags.GEMINI_FLASH, SampleTags.FIREBASE),
        needsFirebase = true,
        keyArt = R.drawable.img_keyart_todo,
    ),

    // To create a new sample entry, add a new SampleCatalogItem here.
)

data class SampleCatalogItem(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val route: String,
    val sampleEntryScreen: @Composable () -> Unit,
    val tags: List<SampleTags> = emptyList(),
    val needsFirebase: Boolean = false,
    val isFeatured: Boolean = false,
    @DrawableRes val keyArt: Int? = null,
)

enum class SampleTags(
    val label: String,
    val backgroundColor: Color,
) {
    FIREBASE("Firebase", extendedColorScheme.firebase),
    GEMINI_FLASH("Gemini Flash", extendedColorScheme.geminiProFlash),
    GEMINI_NANO("Gemini Nano", extendedColorScheme.geminiNano),
    IMAGEN("Imagen", extendedColorScheme.imagen),
    MEDIA3("Media3", extendedColorScheme.media3),
    ML_KIT("ML Kit", extendedColorScheme.mLKit),
}
