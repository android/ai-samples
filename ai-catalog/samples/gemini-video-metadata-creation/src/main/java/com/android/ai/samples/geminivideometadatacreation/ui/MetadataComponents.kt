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

import android.content.Intent
import android.graphics.Bitmap
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.android.ai.samples.geminivideometadatacreation.AccountTag
import com.android.ai.samples.geminivideometadatacreation.AccountTags
import com.android.ai.samples.geminivideometadatacreation.Chapter
import com.android.ai.samples.geminivideometadatacreation.Chapters

@Composable
fun AccountTagsUi(accountTags: AccountTags) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        accountTags.forEach { accountTag ->
            AssistChip(
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, accountTag.url.toUri())
                    context.startActivity(browserIntent)
                },
                label = { Text(text = accountTag.tag) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            )
        }
    }
}

@Composable
fun ChaptersUi(chapters: Chapters, onChapterClicked: (timestamp: Long) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(IntrinsicSize.Max),
    ) {
        chapters.forEach { chapter ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onChapterClicked(chapter.timestamp) })
                    .padding(16.dp),
            ) {
                Text(
                    DateUtils.formatElapsedTime(chapter.timestamp / 1000),
                    textDecoration = TextDecoration.Underline,
                )
                Spacer(Modifier.width(12.dp))
                Text(chapter.title)
            }
        }
    }
}

@Composable
fun DescriptionUi(responseText: String) {
    Text(AnnotatedString.fromHtml(responseText))
}

@Composable
fun HashtagsUi(hashtags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally),
    ) {
        hashtags.forEach { tag ->
            AssistChip(
                onClick = {},
                label = { Text(text = "#$tag") },
            )
        }
    }
}

@Composable
fun LinksUi(links: List<String>) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        links.forEach { link ->
            Text(
                text = link,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val browserIntent = Intent(Intent.ACTION_VIEW, link.toUri())
                    context.startActivity(browserIntent)
                },
            )
        }
    }
}

@Composable
fun ThumbnailsUi(thumbnailTimestamps: List<Long>, thumbnailImages: List<Bitmap>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        thumbnailTimestamps.forEachIndexed { i, timestamp ->
            Column {
                Text(DateUtils.formatElapsedTime(timestamp / 1000))
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = thumbnailImages[i].asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
fun ErrorUi(blockReasonMessage: String?) {
    Text(
        """
                    There was a problem generating the description. Here is some information that might help you debug:
                    Block reason message: $blockReasonMessage
        """.trimIndent(),
        color = MaterialTheme.colorScheme.error,
    )
}

@Preview(widthDp = 320)
@Composable
fun AccountTagsUiPreview() {
    AccountTagsUi(
        accountTags = listOf(
            AccountTag("@Google", "https://google.com"),
            AccountTag("@Android", "https://android.com"),
        ),
    )
}

@Preview(widthDp = 320)
@Composable
fun ChaptersUiPreview() {
    ChaptersUi(
        chapters = listOf(
            Chapter(title = "Introduction", timestamp = 0),
            Chapter(title = "Main Topic", timestamp = 60000),
            Chapter(title = "Conclusion", timestamp = 120000),
        ),
        onChapterClicked = {},
    )
}

@Preview(widthDp = 320)
@Composable
fun DescriptionUiPreview() {
    DescriptionUi(responseText = "<b>This is a bolded description</b> with <i>italic</i> and <u>underlined</u> text.")
}

@Preview(widthDp = 320)
@Composable
fun HashtagsUiPreview() {
    HashtagsUi(hashtags = listOf("AndroidDev", "Compose", "Kotlin", "AI"))
}

@Preview(widthDp = 320)
@Composable
fun LinksUiPreview() {
    LinksUi(links = listOf("https://google.com", "https://android.com"))
}

@Preview(widthDp = 320)
@Composable
fun ThumbnailsUiPreview() {
    val dummyBitmap = createBitmap(120, 120)
    ThumbnailsUi(
        thumbnailTimestamps = listOf(0, 60000, 120000),
        thumbnailImages = listOf(dummyBitmap, dummyBitmap, dummyBitmap),
    )
}

@Preview(widthDp = 320)
@Composable
fun ErrorTextPreview() {
    ErrorUi(blockReasonMessage = "Something went wrong")
}
