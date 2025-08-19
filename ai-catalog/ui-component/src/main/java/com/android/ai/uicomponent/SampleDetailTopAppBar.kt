package com.android.ai.uicomponent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ai.theme.AISampleCatalogTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SampleDetailTopAppBar(
    sampleName: String,
    sampleDescription: String,
    sourceCodeUrl: String,
    modifier: Modifier = Modifier,
    topAppBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState),
) {
    val hasScrolled by remember { derivedStateOf { scrollBehavior.state.heightOffset != 0F } }
    TwoRowsTopAppBar(
        colors = topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.primary,
            scrolledContainerColor = Color.Transparent,
            subtitleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { expanded ->
            if (expanded) {
                Text(
                    text = sampleName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(
                    text = sampleName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        subtitle = { expanded ->
            if (expanded) {
                Text(
                    text = sampleDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(
                    text = sampleDescription,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        navigationIcon = { BackButton {} },
        actions = {
            SeeCodeButton(
                sourceCodeUrl = sourceCodeUrl,
                withText = !hasScrolled,
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(backgroundColor = 0XFF000000, showBackground = true)
@Composable
fun SampleDetailTopAppBarPreview() {
    AISampleCatalogTheme {
        SampleDetailTopAppBar(
            sampleName = "Sample Name",
            sampleDescription = "Sample Description",
            sourceCodeUrl = "https://example.com/source-code"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun SampleDetailTopAppBarPreview_CollapseWhenContentIsScrolled() {
    AISampleCatalogTheme {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SampleDetailTopAppBar(
                    sampleName = "Sample Name",
                    sampleDescription = "Sample Description",
                    sourceCodeUrl = "https://example.com/source-code",
                    topAppBarState = topAppBarState,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            val gradient = Brush.verticalGradient(listOf(Color.LightGray, Color.DarkGray))
            Box(
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .requiredHeight(1000.dp)
                    .background(brush = gradient)
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun SampleDetailTopAppBarPreview_CollapseWhenToolbarIsScrolled() {
    AISampleCatalogTheme {
        Scaffold(
            topBar = {
                SampleDetailTopAppBar(
                    sampleName = "Sample Name",
                    sampleDescription = "Sample Description",
                    sourceCodeUrl = "https://example.com/source-code",
                )
            }
        ) { innerPadding ->
            val gradient = Brush.verticalGradient(listOf(Color.LightGray, Color.DarkGray))
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(brush = gradient)
            )
        }

    }
}

