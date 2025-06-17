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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.ai.catalog.R
import com.android.ai.catalog.ui.domain.SampleCatalogItem
import com.android.ai.catalog.ui.domain.sampleCatalog
import com.google.firebase.FirebaseApp
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
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
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Text(text = stringResource(id = R.string.top_bar_title))
                        },
                    )
                },
            ) { innerPadding ->
                LazyColumn(
                    contentPadding = innerPadding,
                ) {
                    items(sampleCatalog) {
                        CatalogListItem(catalogItem = it) {
                            if (it.needsFirebase && !isFirebaseInitialized()) {
                                isDialogOpened = true
                            } else {
                                navController.navigate(it.route)
                            }
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

@Composable
fun CatalogListItem(catalogItem: SampleCatalogItem, onButtonClick: () -> Unit) {
    val context = LocalContext.current
    ElevatedCard(
        modifier = Modifier.padding(18.dp),
        onClick = {
            onButtonClick()
        },
    ) {
        Column(
            Modifier.padding(15.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                text = context.getString(catalogItem.title),
            )
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = context.getString(catalogItem.description),
            )
            Row {
                Spacer(Modifier.weight(1f))
                catalogItem.tags.forEach {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = it.backgroundColor,
                                shape = RoundedCornerShape(
                                    8.dp,
                                ),
                            )
                            .padding(start = 4.dp, end = 4.dp),
                    ) {
                        Text(
                            fontSize = 9.sp,
                            text = it.label,
                            color = it.textColor,
                        )
                    }
                }
            }
        }
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
