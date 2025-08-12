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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ai.theme.AISampleCatalogTheme

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    icon: Painter? = null,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = contentColor,
            containerColor = containerColor,
        ),
        onClick = { onClick() },
    ) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(width = 24.dp, height = 24.dp),
            )
        }
        if (text.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Preview
@Composable
fun PrimaryButtonPreview() {
    AISampleCatalogTheme {
        PrimaryButton(
            text = "Primary button",
            icon = rememberVectorPainter(Icons.Default.AccountBox),
            onClick = {},
        )
    }
}

@Preview
@Composable
fun PrimaryButtonWithIconPreview() {
    AISampleCatalogTheme {
        PrimaryButton(
            text = "",
            icon = rememberVectorPainter(Icons.Default.AccountBox),
            onClick = {},
        )
    }
}

@Composable
fun GenerateButton(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onTertiary,
    containerColor: Color = MaterialTheme.colorScheme.tertiary,
    enabled: Boolean = true,
    icon: Painter? = painterResource(id = R.drawable.draw_auto),
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier
            .height(56.dp)
            .border(
                if (enabled) {
                    BorderStroke(0.dp, Color.Transparent)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                },
                shape = RoundedCornerShape(30.dp),
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            contentColor = contentColor,
            containerColor = containerColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
        ),
        onClick = { onClick() },
    ) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (enabled) contentColor else MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.size(width = 24.dp, height = 24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
fun GenerateButtonPreview() {
    AISampleCatalogTheme {
        GenerateButton(
            text = "Generate",
            onClick = {},
        )
    }
}

@Preview
@Composable
fun GenerateButtonDisabledPreview() {
    AISampleCatalogTheme {
        GenerateButton(
            text = "Generate",
            enabled = false,
            onClick = {},
        )
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: Painter? = null, onClick: () -> Unit) {
    OutlinedButton(
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant),
        onClick = { onClick() },
    ) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(width = 24.dp, height = 24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
fun SecondaryButtonPreview() {
    AISampleCatalogTheme {
        SecondaryButton(
            text = "Outlined button",
            icon = painterResource(id = R.drawable.add_photo),
            onClick = {},
        )
    }
}

@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier.height(48.dp).width(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = { onClick() },
    ) {
        Image(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(width = 24.dp, height = 24.dp),
            )
    }
}


@Preview
@Composable
fun BackButtonPreview() {
    AISampleCatalogTheme {
        BackButton(
            onClick = {},
        )
    }
}

