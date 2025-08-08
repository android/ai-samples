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
package com.android.ai.catalog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ai.catalog.R
import com.android.ai.catalog.domain.SampleCatalogItem
import com.android.ai.catalog.domain.SampleTags
import com.android.ai.uicomponent.Tag

@Composable
fun CatalogWideCard(catalogItem: SampleCatalogItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    ElevatedCard(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
        ),
        onClick = {
            onClick()
        },
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomEnd = 12.dp,
            bottomStart = 12.dp,
        ),
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.illo), // Assuming illo.png is in res/drawable
                contentDescription = "Illustration", // Add a content description
                modifier = Modifier
                    .fillMaxWidth()
                    .height(182.dp), // Add some padding below the image
                contentScale = ContentScale.FillWidth, // Or another ContentScale like Fit, FillBounds, etc.
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                text = context.getString(catalogItem.title),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            ) {
                catalogItem.tags.forEach {
                    Tag(text = it.label, color = it.backgroundColor)
                }
            }
            Text(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                text = context.getString(catalogItem.description),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatalogWideCardPreview() {
    val sampleItem = SampleCatalogItem(
        title = R.string.gemini_multimodal_sample_title,
        description = R.string.gemini_multimodal_sample_description,
        route = "GeminiMultimodalScreen",
        sampleEntryScreen = { },
        tags = listOf(SampleTags.GEMINI_2_0_FLASH, SampleTags.FIREBASE),
    )

    CatalogWideCard(
        catalogItem = sampleItem,
        onClick = { /* No-op for the preview */ },
    )
}
