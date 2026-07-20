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

package com.example.jetpacker.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.jetpacker.core.ui.JetPackerTheme
import kotlin.math.roundToInt

internal class ToolbarState {
  var toolbarCoords: LayoutCoordinates? by mutableStateOf(null)
  var selectedBounds: Rect? by mutableStateOf(null)
}

internal val LocalToolbarState = compositionLocalOf<ToolbarState?> { null }

@Composable
fun JetPackerToolbar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
  val shape = RoundedCornerShape(32.dp)
  val state = remember { ToolbarState() }

  val indicatorOffset by
    animateOffsetAsState(
      targetValue = state.selectedBounds?.topLeft ?: Offset.Zero,
      label = "indicatorOffset",
    )
  val indicatorSize by
    animateSizeAsState(
      targetValue = state.selectedBounds?.size ?: Size.Zero,
      label = "indicatorSize",
    )

  Box(
    modifier =
      modifier
        .dropShadow(shape = shape) {
          radius = 3.dp.toPx()
          offset = Offset(0f, 1.dp.toPx())
          color = Color.Black.copy(alpha = 0.3f)
        }
        .dropShadow(shape = shape) {
          radius = 8.dp.toPx()
          offset = Offset(0f, 4.dp.toPx())
          spread = 3.dp.toPx()
          color = Color.Black.copy(alpha = 0.15f)
        }
        .background(MaterialTheme.colorScheme.onSurface, shape)
        .onGloballyPositioned { state.toolbarCoords = it }
  ) {
    if (state.selectedBounds != null) {
      Box(
        modifier =
          Modifier.offset {
              IntOffset(indicatorOffset.x.roundToInt(), indicatorOffset.y.roundToInt())
            }
            .size(
              width = with(LocalDensity.current) { indicatorSize.width.toDp() },
              height = with(LocalDensity.current) { indicatorSize.height.toDp() },
            )
            .background(MaterialTheme.colorScheme.primary, CircleShape)
      )
    }

    CompositionLocalProvider(LocalToolbarState provides state) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
      )
    }
  }
}

@Composable
fun JetPackerToolbarAction(
  icon: ImageVector,
  contentDescription: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
) {
  val state = LocalToolbarState.current
  var currentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

  val contentColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.surfaceContainer,
      label = "contentColor",
    )

  LaunchedEffect(selected, currentCoords, state?.toolbarCoords) {
    if (
      selected &&
        state != null &&
        currentCoords != null &&
        currentCoords!!.isAttached &&
        state.toolbarCoords != null &&
        state.toolbarCoords!!.isAttached
    ) {
      state.selectedBounds = state.toolbarCoords!!.localBoundingBoxOf(currentCoords!!)
    }
  }

  Row(
    modifier =
      modifier
        .height(48.dp)
        .onGloballyPositioned { coords ->
          currentCoords = coords
          if (
            selected &&
              state != null &&
              state.toolbarCoords != null &&
              coords.isAttached &&
              state.toolbarCoords!!.isAttached
          ) {
            state.selectedBounds = state.toolbarCoords!!.localBoundingBoxOf(coords)
          }
        }
        .clip(CircleShape)
        .clickable { onClick() }
        .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = contentColor,
      modifier = Modifier.size(24.dp),
    )
  }
}

@Preview
@Composable
fun JetPackerToolbarPreview() {
  JetPackerTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      JetPackerToolbar {
        JetPackerToolbarAction(
          icon = Icons.Rounded.Event,
          contentDescription = "Today",
          onClick = {},
          selected = false,
        )
        JetPackerToolbarAction(
          icon = Icons.Rounded.ConfirmationNumber,
          contentDescription = "Tickets",
          onClick = {},
          selected = false,
        )
        JetPackerToolbarAction(
          icon = Icons.Rounded.Wallet,
          contentDescription = "Expenses",
          onClick = {},
          selected = true,
        )
        JetPackerToolbarAction(
          icon = Icons.Rounded.AutoAwesome,
          contentDescription = "Genius",
          onClick = {},
          selected = false,
        )
      }
    }
  }
}
