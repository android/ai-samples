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

@file:OptIn(
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalMaterial3Api::class,
)

package com.example.jetpacker.feature.itinerary_enrichment

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun TripSummaryAndTipsCard(modifier: Modifier = Modifier, isLoading: Boolean, summary: String?, trip: Trip? = null) {
  val isLoaded = !isLoading && summary != null
  var expanded by remember { mutableStateOf(false) }
  var containerSize by remember { mutableStateOf(IntSize.Zero) }
  var contentHeight by remember { mutableStateOf(0) }

  val density = LocalDensity.current
  val maxHeight = 120.dp
  val maxHeightPx = with(density) { maxHeight.toPx() }
  val isScrollable = isLoaded && contentHeight > maxHeightPx

  val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
  val rotation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 4000, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "glow_rotation",
    )

  val glowBrush =
    remember(rotation, containerSize) {
      val width = containerSize.width.toFloat()
      if (width <= 0f)
        return@remember Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))

      val xOffset = width * (rotation / 360f)
      Brush.linearGradient(
        colors =
          listOf(
            Color(0xFF9CEFFF), // Cyan
            Color(0xFFD2E4FF), // Blue
            Color(0xFFFFB59F), // Pink
          ),
        start = Offset(xOffset - width, 0f),
        end = Offset(xOffset, 0f),
        tileMode = TileMode.Mirror,
      )
    }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .onSizeChanged { containerSize = it }
        .dropShadow(RoundedCornerShape(24.dp)) {
          if (isLoaded) {
            brush = glowBrush
            radius = 24.dp.toPx()
            offset = Offset(x = 0f, y = 16.dp.toPx())
          }
        }
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
        .border(
          2.dp,
          if (isLoaded) Color.Transparent else Color.Black.copy(alpha = 0.45f),
          RoundedCornerShape(24.dp),
        )
        .clip(RoundedCornerShape(24.dp))
        .animateContentSize()
        .clickable(enabled = isLoaded) { expanded = !expanded }
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .then(if (!expanded && isScrollable) Modifier.height(maxHeight) else Modifier)
          .graphicsLayer(clip = true)
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .wrapContentHeight(unbounded = true, align = Alignment.Top)
            .onSizeChanged { contentHeight = it.height }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        val shape = MaterialShapes.Gem
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary

        Box(
          modifier =
            Modifier.size(50.dp)
              .background(
                brush =
                  Brush.linearGradient(
                    colors = listOf(primary, secondary, tertiary)
                  ),
                shape = shape.toShape(),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.size(24.dp),
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = getTripMessage(trip),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(4.dp))

          if (isLoading) {
            ShimmerEffect()
          } else if (summary != null) {
            Text(
              text = parseMarkdownToAnnotatedString(summary),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = if (expanded) Int.MAX_VALUE else 3,
              overflow = TextOverflow.Ellipsis,
            )
          } else {
            Text(
              text = "Add more events to your itinerary to generate trip summaries and tips!",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
          }
        }

        if (isLoaded) {
          Box(
            modifier =
              Modifier.size(24.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")
            Icon(
              imageVector = Icons.Rounded.KeyboardArrowDown,
              contentDescription = if (expanded) "Collapse" else "Expand",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
          }
        }
      }

      if (!expanded && isScrollable) {
        Box(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .fillMaxWidth()
              .height(40.dp)
              .background(
                Brush.verticalGradient(
                  listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceBright)
                )
              )
        )
      }
    }
  }
}

@Composable
fun ShimmerEffect() {
  val shimmerColors =
    listOf(
      Color.LightGray.copy(alpha = 0.6f),
      Color.LightGray.copy(alpha = 0.2f),
      Color.LightGray.copy(alpha = 0.6f),
    )

  val transition = rememberInfiniteTransition(label = "shimmer")
  val translateAnim by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 2000f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "shimmer_translate",
    )

  val brush =
    Brush.linearGradient(
      colors = shimmerColors,
      start = Offset(translateAnim - 500f, translateAnim - 500f),
      end = Offset(translateAnim, translateAnim),
    )

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Spacer(
      modifier =
        Modifier.height(14.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(brush)
    )
    Spacer(
      modifier =
        Modifier.height(14.dp).fillMaxWidth(0.9f).clip(RoundedCornerShape(4.dp)).background(brush)
    )
    Spacer(
      modifier =
        Modifier.height(14.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(4.dp)).background(brush)
    )
  }
}

fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
  return buildAnnotatedString {
    val regex = Regex("""\*\*(.*?)\*\*|\*(?!\s)(.*?)(?<!\s)\*""")
    var lastIndex = 0
    regex.findAll(text).forEach { matchResult ->
      append(text.substring(lastIndex, matchResult.range.first))

      val boldGroup = matchResult.groups[1]
      val italicGroup = matchResult.groups[2]

      if (boldGroup != null) {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(boldGroup.value) }
      } else if (italicGroup != null) {
        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) { append(italicGroup.value) }
      }
      lastIndex = matchResult.range.last + 1
    }
    if (lastIndex < text.length) {
      append(text.substring(lastIndex))
    }
  }
}

fun getTripMessage(trip: Trip?): String {
    // val now = System.currentTimeMillis()
    // to test for May 20, 2026, 12:15pm Pacific time, time of I/O live talk
    val now = ZonedDateTime.of(
    2026, 5, 20, 12, 15, 0, 0, ZoneId
      .of("America/Los_Angeles")).toInstant().toEpochMilli()
    if (trip == null) return "Get ready for your trip"
    return when {
        now > trip.endDate -> "Trip summary"
        now in trip.startDate..trip.endDate -> "Tips for the next few days"
        else -> "Get ready for your trip"
    }
}

@Preview(showBackground = true)
@Composable
fun TripSummaryAndTipsCardPreview() {
  MaterialTheme {
    TripSummaryAndTipsCard(
      isLoading = false,
      summary = "Your trip is looking **amazing**! Get ready for a perfect balance of exploration and relaxation.",
      trip = DummyData.trips.first()
    )
  }
}

@Preview(showBackground = true)
@Composable
fun TripSummaryAndTipsCardLoadingPreview() {
  MaterialTheme { TripSummaryAndTipsCard(isLoading = true, summary = null, trip = DummyData.trips.first()) }
}
