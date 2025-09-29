package com.android.ai.uicomponent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
) {

    RichText(
        modifier = modifier,
        style = RichTextStyle(
            stringStyle = RichTextStringStyle(),
        ),
    ) {
        Markdown(text)
    }
}
