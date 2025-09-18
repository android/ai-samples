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

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ai.catalog.R
import com.android.ai.catalog.domain.sampleCatalog
import com.android.ai.theme.AISampleCatalogTheme
import com.google.ai.samples.agentassistant.AgentAssistantScreenContent
import com.google.firebase.FirebaseApp


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onOpenFirebaseDialog: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    Scaffold(
        topBar = { HomeScreenTopBar(scrollBehavior) },
        floatingActionButton = { HomeScreenFAB(onClick = { showBottomSheet = true }) },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Image(
            painter = painterResource(id = R.drawable.img_bg_landing),
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
        )
        LazyColumn(
            contentPadding = innerPadding,
        ) {
            items(sampleCatalog) {
                val onClick = {
                    if (it.needsFirebase && !isFirebaseInitialized()) {
                        onOpenFirebaseDialog()
                    } else {
                        onNavigate(it.route)
                    }
                }
                if (it.isFeatured) {
                    CatalogWideCard(catalogItem = it, onClick = onClick)
                } else {
                    CatalogRowCard(catalogItem = it, onClick = onClick)
                }
            }
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
        ) {
            AgentAssistantScreenContent(Modifier.imePadding())
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun HomeScreenTopBar(scrollBehavior: TopAppBarScrollBehavior) {
    TwoRowsTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { expanded ->
            if (expanded) {
                AppBarPill()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.top_bar_title_expanded),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            } else {
                Row {
                    AppBarPill()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.top_bar_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }

        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreenFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MediumFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialShapes.ClamShell.toShape(),
        containerColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Icon(
            painter = painterResource(com.android.ai.uicomponent.R.drawable.ic_ai_question),
            contentDescription = null,
            modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
        )
    }
}

private fun isFirebaseInitialized(): Boolean {
    return try {
        val firebaseApp = FirebaseApp.getInstance()
        firebaseApp.options.projectId != "mock_project"
    } catch (_: IllegalStateException) {
        Log.e("CatalogScreen", "Firebase is not initialized")
        false
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    AISampleCatalogTheme {
        HomeScreen(
            onOpenFirebaseDialog = {},
            onNavigate = {},
            modifier = Modifier,
        )
    }
}
