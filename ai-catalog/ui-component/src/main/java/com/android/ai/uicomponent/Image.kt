package com.android.ai.uicomponent

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ai.theme.AISampleCatalogTheme

@Composable
fun ImageInput(
    buttonText: String,
    modifier: Modifier = Modifier,
    image: Bitmap? = null,
    hint: String = "",
    onAddImage: () -> Unit
) {
    Column(
        modifier = modifier
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(40.dp)
            )
            .padding(24.dp)
    ) {
        if (image!=null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
            )
        } else {
            Spacer(
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = buttonText,
                onClick = onAddImage,
                icon = ImageVector.vectorResource(id = R.drawable.send_spark),
                modifier = Modifier
                    .height(64.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(
                modifier = Modifier.weight(1f)
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
fun ImageInputPreview() {
    AISampleCatalogTheme {
        ImageInput(
            buttonText = "Add Image",
            hint = "Optional hint text: Add a prompt to generate an image.",
            onAddImage = {},
            modifier = Modifier.height(300.dp)
        )
    }
}