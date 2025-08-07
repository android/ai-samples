package com.android.ai.uicomponent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ai.theme.AISampleCatalogTheme

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color =  MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = contentColor,
            containerColor = containerColor,
        ),
        onClick = { onClick() }) {
        if (icon != null) {
            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(width = 24.dp, height = 24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Preview
@Composable
fun PrimaryButtonSmallPreview() {
    AISampleCatalogTheme {
        PrimaryButton(
            text = "Primary button",
            icon = Icons.Default.AccountBox,
            onClick = {}
        )
    }
}

@Composable
fun GenerateButton(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color =  MaterialTheme.colorScheme.onTertiary,
    containerColor: Color = MaterialTheme.colorScheme.tertiary,
    enabled: Boolean = true,
    icon: ImageVector? = ImageVector.vectorResource(id = R.drawable.draw_auto),
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier
            .height(56.dp)
            .border(
                if (enabled){
                    BorderStroke(0.dp, Color.Transparent)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                },
                shape = RoundedCornerShape(30.dp)
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            contentColor = contentColor,
            containerColor = containerColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
        ),
        onClick = { onClick() }
    ) {
        if (icon != null) {
            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (enabled) contentColor else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(width = 24.dp, height = 24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview
@Composable
fun GenerateButtonPreview() {
    AISampleCatalogTheme {
        GenerateButton(
            text = "Generate",
            onClick = {}
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
            onClick = {}
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier.height(48.dp),
        colors = ButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = Color.Transparent,
            disabledContentColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant),
        onClick = { onClick() }) {
        if (icon != null) {
            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(width = 24.dp, height = 24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview
@Composable
fun SecondaryButtonPreview() {
    AISampleCatalogTheme {
        SecondaryButton(
            text = "Outlined button",
            icon = ImageVector.vectorResource(id = R.drawable.add_photo),
            onClick = {}
        )
    }
}
