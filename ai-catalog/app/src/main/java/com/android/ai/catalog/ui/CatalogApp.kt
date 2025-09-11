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

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.ai.catalog.R
import com.android.ai.catalog.domain.sampleCatalog
import com.android.ai.uicomponent.PrimaryButton
import com.google.firebase.FirebaseApp
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CatalogApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var isDialogOpened by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = HomeScreen,
    ) {
        composable<HomeScreen> {
            val topAppBarState = rememberTopAppBarState()
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TwoRowsTopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = { expanded ->
                            if (expanded) {
                                Row (
                                    modifier = Modifier.height(40.dp).width(58.dp).background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(com.android.ai.uicomponent.R.drawable.ic_spark),
                                        contentDescription = null,
                                        modifier = Modifier.padding(10.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = stringResource(id = if (expanded) R.string.top_bar_title_expanded else R.string.top_bar_title),
                                style = if (expanded) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (expanded) 2 else 1,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
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
                                isDialogOpened = true
                            } else {
                                navController.navigate(it.route)
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
        }
        sampleCatalog.forEach {
            val catalogItem = it
            composable(catalogItem.route) {
                catalogItem.sampleEntryScreen()
            }
        }
    }

    if (isDialogOpened) {
        FirebaseRequiredAlert(
            onDismiss = { isDialogOpened = false },
            onOpenFirebaseDocClick = {
                isDialogOpened = false
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://firebase.google.com/docs/vertex-ai/get-started#no-existing-firebase".toUri(),
                )
                context.startActivity(intent)
            },
        )
    }
}

@Serializable
object HomeScreen

@Composable
fun FirebaseRequiredAlert(onDismiss: () -> Unit = {}, onOpenFirebaseDocClick: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Text(text = stringResource(R.string.firebase_required))
        },
        text = {
            Text(stringResource(R.string.firebase_required_description))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.close))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOpenFirebaseDocClick()
                },
            ) {
                Text(stringResource(R.string.firebase_doc_button))
            }
        },
    )
}

fun isFirebaseInitialized(): Boolean {
    return try {
        val firebaseApp = FirebaseApp.getInstance()
        return firebaseApp.options.projectId != "mock_project"
    } catch (e: IllegalStateException) {
        Log.e("CatalogScreen", "Firebase is not initialized")
        return false
    }
}
