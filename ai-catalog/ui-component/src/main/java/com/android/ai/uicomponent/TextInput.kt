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
package com.android.ai.uicomponent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ai.theme.AISampleCatalogTheme

@Composable
fun TextInput(
    value: String,
    modifier: Modifier = Modifier,
    hint: String = "",
    placeholder: String = "",
    primaryButton: @Composable () -> Unit = {},
    secondaryButton: @Composable () -> Unit = {},
    onValueChange: (String) -> Unit,
) {
    val roundCornerShape = RoundedCornerShape(30.dp)

    Row(
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                shape = roundCornerShape,
            )
            .height(56.dp)
            .clip(
                shape = roundCornerShape,
            )
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        OutlinedTextField(
            value = value,
            label = { Text(text = hint) },
            placeholder = { Text(text = placeholder) },
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .padding(start = 12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
        secondaryButton()
        primaryButton()
    }
}

@Composable
@Preview
fun TextInputPreview() {
    AISampleCatalogTheme {
        TextInput(
            value = "",
            hint = "Message hint",
            placeholder = "Placeholder", onValueChange = {},
            primaryButton = {
                GenerateButton(
                    text = "",
                    icon = painterResource(id = R.drawable.send_spark),
                    modifier = Modifier.width(72.dp).padding(4.dp),
                    onClick = {},
                )
            },
            secondaryButton = {
                SecondaryButton(
                    text = "",
                    icon = painterResource(id = R.drawable.add_photo),
                    modifier = Modifier.width(72.dp).padding(4.dp),
                    onClick = {},
                )
            },
        )
    }
}
