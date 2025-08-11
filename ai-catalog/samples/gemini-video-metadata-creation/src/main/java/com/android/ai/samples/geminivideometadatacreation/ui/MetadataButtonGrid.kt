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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.ai.samples.geminivideometadatacreation.viewmodel.MetadataType

/**
 * A Composable that displays a grid of buttons for each [MetadataType].
 *
 * This function dynamically creates a button for every entry in the [MetadataType] enum.
 * It uses a [FlowRow] to arrange the buttons, allowing them to wrap to the next line
 * if they exceed the available horizontal space. The currently selected button is
 * highlighted with the primary color.
 */
@Composable
fun ButtonGrid(selectedMetadataType: MetadataType?, onMetadataCreationClicked: (MetadataType) -> Unit, modifier: Modifier = Modifier) {
    val metadataTypes = MetadataType.entries

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        metadataTypes.forEach { metadataType ->
            val isSelected = selectedMetadataType == metadataType
            Button(
                onClick = { onMetadataCreationClicked(metadataType) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = metadataType.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                )
            }
        }
    }
}
