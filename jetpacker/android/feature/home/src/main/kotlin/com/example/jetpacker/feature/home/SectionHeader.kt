/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetpacker.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jetpacker.core.ui.SekuyaFontFamily

@Composable
fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  isExpandable: Boolean = false,
  isExpanded: Boolean = false,
  drawBackground: Boolean = true,
  contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp),
  onToggle: () -> Unit = {},
) {
  var stickiness by remember { mutableStateOf(0f) }
  val containerColor = MaterialTheme.colorScheme.surfaceContainer
  val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

  val topPaddingAnimated by
    animateDpAsState(if (stickiness >= 1f) statusBarHeight else 12.dp, label = "topPadding")
  val bottomPaddingAnimated by
    animateDpAsState(if (stickiness >= 1f) 8.dp else 12.dp, label = "bottomPadding")

  val drawBackgroundModifier =
    if (drawBackground) {
      Modifier.onGloballyPositioned { coordinates ->
          val y = coordinates.positionInParent().y
          // As y approaches 0, stickiness goes from 0 to 1. Using a 100px threshold.
          stickiness = (1f - (y / 100f)).coerceIn(0f, 1f)
        }
        .drawBehind {
          if (stickiness > 0f && drawBackground) {
            // Calculate height including status bar (approximate 48dp) to bleed to top
            val extraBleed = 100.dp.toPx()
            drawRect(
              color = containerColor,
              topLeft = Offset(-extraBleed, size.height * (1f - stickiness) - extraBleed),
              size = Size(size.width + (extraBleed * 2), size.height * stickiness + extraBleed),
            )
          }
        }
    } else {
      Modifier
    }

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .then(drawBackgroundModifier)
        .then(if (isExpandable) Modifier.clickable(onClick = onToggle) else Modifier)
        .padding(contentPadding)
        .padding(top = topPaddingAnimated, bottom = bottomPaddingAnimated),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title.uppercase(),
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      fontFamily = SekuyaFontFamily,
    )

    if (isExpandable) {
      Box(
        modifier =
          Modifier.size(40.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        AnimatedContent(
          targetState = isExpanded,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "SectionHeaderIcon",
        ) { expanded ->
          Icon(
            imageVector = if (expanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
          )
        }
      }
    }
  }
}
