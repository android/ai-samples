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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.serialization.Serializable

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
            HomeScreen(
                onOpenFirebaseDialog = { isDialogOpened = true },
                onNavigate = { navController.navigate(it) },
                modifier = modifier,
            )
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
fun AppBarPill() {
    Row(
        modifier = Modifier
            .height(40.dp)
            .width(58.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(24.dp),
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.spark_android),
            contentDescription = null,
            modifier = Modifier.padding(10.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

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
