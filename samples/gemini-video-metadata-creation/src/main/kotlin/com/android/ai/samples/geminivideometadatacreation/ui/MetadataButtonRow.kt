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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.ai.samples.geminivideometadatacreation.viewmodel.MetadataType

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ButtonRow(selectedMetadataType: MetadataType?, onMetadataCreationClicked: (MetadataType) -> Unit, modifier: Modifier = Modifier) {
    val metadataTypes = MetadataType.entries

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),

    ) {
        Spacer(Modifier.width(10.dp))
        metadataTypes.forEach { metadataType ->
            val isSelected = selectedMetadataType == metadataType
            OutlinedToggleButton(
                checked = isSelected,
                onCheckedChange = { onMetadataCreationClicked(metadataType) },
                colors = ToggleButtonDefaults.outlinedToggleButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                Icon(
                    painterResource(metadataType.iconRes),
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(metadataType.titleRes).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
    }
}
