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
    onValueChange: (String) -> Unit
) {
    Row (
       modifier = modifier
           .border(
               1.dp,
               MaterialTheme.colorScheme.outline,
               shape = RoundedCornerShape(30.dp)
           )
           .height(56.dp)
           .clip(
               shape = RoundedCornerShape(30.dp),
           )
           .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
    ){
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
                )
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
            primaryButton = { GenerateButton(
                text = "",
                icon = ImageVector.vectorResource(id = R.drawable.send_spark),
                modifier = Modifier.width(72.dp).padding(4.dp),
                onClick = {}
            ) },
            secondaryButton = { SecondaryButton(
                text = "",
                icon = ImageVector.vectorResource(id = R.drawable.add_photo),
                modifier = Modifier.width(72.dp).padding(4.dp),
                onClick = {}
            ) },
        )
    }
}


