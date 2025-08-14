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
package com.android.ai.samples.geminivideometadatacreation.util

import com.android.ai.samples.geminivideometadatacreation.viewmodel.MetadataType

data class Prompt(
    val metadataType: MetadataType,
    val text: String,
)

val promptList = listOf(
    Prompt(
        metadataType = MetadataType.DESCRIPTION,
        text = "Provide a compelling and concise description for this video in about 7-8 lines. " +
            "Return only the description and nothing else. Don't assume if you don't know" +
            " The description should be engaging and accurately reflect the video\'s content. Don't assume if you don't know",
    ),
    Prompt(
        metadataType = MetadataType.THUMBNAILS,
        text =
        "Get three  engaging and visually appealing thumbnails for this video. Focus on capturing peak moments that create curiosity." +
            " Return only a comma separated list of timestamps in format \"hh:mm:ss\". Don\'t return any other text.",
    ),
    Prompt(
        metadataType = MetadataType.HASHTAGS,
        text = "Generate a list of relevant and trending hashtags for this video to maximize its visibility on social media platforms. " +
            "Return only the list of hashtags, separated by commas.",
    ),
    Prompt(
        metadataType = MetadataType.ACCOUNT_TAGS,
        text = "Suggest relevant accounts to tag in the video\'s description or comments to increase its reach and engagement. " +
            "Return only the list of accounts, separated by commas.",
    ),
    Prompt(
        metadataType = MetadataType.CHAPTERS,
        text = "Analyze the video and create a list of chapters with timestamps and descriptive titles. " +
            "This will help viewers navigate the video and find specific sections of interest.",
    ),
    Prompt(
        metadataType = MetadataType.LINKS,
        text = "Analyze the video and create a list of relevant links to be tagged. Return possible 3-4 links to be shared in the video.",
    ),
)
