package com.android.ai.uicomponent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Tag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .padding(start = 2.dp, end = 6.dp, bottom = 4.dp)
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(size = 16.dp)),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .padding(2.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier.width(2.dp))
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 3.dp, bottom = 2.dp),
            maxLines = 1
        )
        Spacer(modifier.width(6.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun TagPreview() {
    Tag(text = "Gemini Nano", color = Color.Gray)
}


