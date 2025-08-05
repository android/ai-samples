package com.android.ai.catalog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ai.catalog.R
import com.android.ai.catalog.domain.SampleCatalogItem
import com.android.ai.catalog.domain.SampleTags
import com.android.ai.uicomponent.Tag


@Composable
fun CatalogRowCard(
    catalogItem: SampleCatalogItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    ElevatedCard(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp
        ),
        onClick = {
            onClick()
        },
    ) {
        Row {
            Image(
                painter = painterResource(id = R.drawable.illo),
                contentDescription = "Illustration",
                modifier = Modifier
                    .height(92.dp)
                    .width(92.dp)
                    .padding(top = 12.dp, start = 12.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
            )
            Column {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    text = context.getString(catalogItem.title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 4.dp),
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatalogRowCardPreview() {
    val sampleItem = SampleCatalogItem(
        title = R.string.gemini_multimodal_sample_title,
        description = R.string.gemini_multimodal_sample_description,
        route = "GeminiMultimodalScreen",
        sampleEntryScreen = { },
        tags = listOf(SampleTags.GEMINI_2_0_FLASH, SampleTags.FIREBASE),
    )

    CatalogRowCard(
        catalogItem = sampleItem,
        onClick = { /* No-op for the preview */ },
    )
}