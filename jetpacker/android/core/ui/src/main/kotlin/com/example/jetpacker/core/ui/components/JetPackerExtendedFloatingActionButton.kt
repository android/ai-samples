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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetpacker.core.ui.JetPackerTheme

@Composable
fun JetPackerExtendedFloatingActionButton(
  onClick: () -> Unit,
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
  isListening: Boolean = false,
  audioLevel: Float = 0f,
  trailingContent: @Composable (RowScope.() -> Unit)? = null,
) {
  val shape = CircleShape
  val inverseSurface = MaterialTheme.colorScheme.inverseSurface

  Row(
    modifier =
      modifier
        .dropShadow(shape = shape) {
          radius = 0f
          spread = 0f
          offset = Offset(x = 2.dp.toPx(), y = 3.dp.toPx())
          color = inverseSurface
        }
        .background(MaterialTheme.colorScheme.primary, shape)
        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, shape)
        .clip(shape)
        .padding(horizontal = 8.dp, vertical = 12.dp)
        .height(IntrinsicSize.Min),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    AnimatedContent(
      targetState = isListening,
      transitionSpec = { fadeIn().togetherWith(fadeOut()) },
      label = "JetPackerExtendedFloatingActionButtonContent",
    ) { listening ->
      if (listening) {
        VoiceEqualizer(
          audioLevel = audioLevel,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
      } else {
        Row(
          modifier =
            Modifier.clip(shape).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }
    }

    trailingContent?.invoke(this)
  }
}

@Composable
private fun VoiceEqualizer(audioLevel: Float, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val durations = listOf(450, 600, 500, 700, 550, 650, 400, 750)
    val maxScales = listOf(0.8f, 1.0f, 0.7f, 0.9f, 1.0f, 0.8f, 0.6f, 1.0f)

    repeat(durations.size) { index ->
      val smoothedAudioLevel by
        animateFloatAsState(
          targetValue = audioLevel / 100f,
          animationSpec =
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
          label = "SmoothedAudioLevel",
        )

      val heightScale by
        rememberInfiniteTransition(label = "VoiceEqualizer")
          .animateFloat(
            initialValue = 0.2f,
            targetValue = maxScales[index],
            animationSpec =
              infiniteRepeatable(
                animation =
                  tween(
                    durationMillis = durations[index],
                    easing = LinearEasing,
                    delayMillis = index * 50,
                  ),
                repeatMode = RepeatMode.Reverse,
              ),
            label = "BarHeight",
          )

      Box(
        modifier =
          Modifier
            .clip(CircleShape)
            .width(4.dp)
            .height(24.dp)
            .graphicsLayer { scaleY = (heightScale * smoothedAudioLevel).coerceAtLeast(0.1f) }
            .background(color = MaterialTheme.colorScheme.onPrimary, shape = CircleShape)
      )
    }
  }
}

@Preview
@Composable
fun JetPackerExtendedFloatingActionButtonPreview() {
  JetPackerTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      JetPackerExtendedFloatingActionButton(
        onClick = {},
        icon = Icons.Rounded.Add,
        label = "Create trip",
      )
    }
  }
}

@Preview
@Composable
fun JetPackerExtendedFloatingActionButtonListeningPreview() {
  JetPackerTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      JetPackerExtendedFloatingActionButton(
        onClick = {},
        icon = Icons.Rounded.Add,
        label = "Create trip",
        isListening = true,
      )
    }
  }
}
