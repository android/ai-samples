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

package com.example.jetpacker.feature.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.core.ui.R as CoreUiR
import com.example.jetpacker.core.ui.SekuyaFontFamily
import com.example.jetpacker.data.trips.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMediaQueryApi::class, ExperimentalFlexBoxApi::class)
@Composable
fun TripCard(
  trip: Trip,
  isSwipedOff: Boolean,
  modifier: Modifier = Modifier,
  useHorizontalLayout: Boolean = true,
  canSwipeToDelete: Boolean = true,
  showTitle: Boolean = true,
  onClick: () -> Unit = {},
  onSwipeToDelete: () -> Unit = {},
) {
  val dismissState = rememberSwipeToDismissBoxState()

  LaunchedEffect(dismissState.currentValue) {
    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
      onSwipeToDelete()
    }
  }

  LaunchedEffect(isSwipedOff) {
    if (!isSwipedOff && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
      dismissState.reset()
    }
  }

  SwipeToDismissBox(
    modifier = modifier,
    state = dismissState,
    enableDismissFromStartToEnd = false,
    enableDismissFromEndToStart = canSwipeToDelete,
    backgroundContent = {
      val color = MaterialTheme.colorScheme.errorContainer
      // Fill the entire background if the item is considered swiped off/deleted to avoid gaps.
      Box(
        Modifier.fillMaxSize()
          .clip(RoundedCornerShape(40.dp))
          .background(if (isSwipedOff) color else Color.Transparent)
      ) {
        val offset =
          try {
            dismissState.requireOffset()
          } catch (_: Exception) {
            0f
          }
        val p = (abs(offset) / 600f).coerceIn(0f, 1f)

        if (!isSwipedOff) {
          Spacer(
            Modifier.align(Alignment.CenterEnd)
              .padding(end = 24.dp)
              .size(24.dp)
              .graphicsLayer {
                val s = 1f + 50f * (p * p)
                scaleX = s
                scaleY = s
                alpha = if (p > 0.01f) 1f else 0f
              }
              .background(color, CircleShape)
          )
        }

        Icon(
          imageVector = Icons.Filled.Delete,
          contentDescription = "Delete",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp),
        )
      }
    },
  ) {
    val participants =
      remember(trip.participants) {
        trip.participants.map { name -> Participant(name = name, avatar = getAvatarForName(name)) }
      }

    FlexBox(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(40.dp))
          .then(modifier)
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(40.dp))
          .clickable(onClick = onClick)
          .padding(16.dp),
      config = {
        if (useHorizontalLayout) {
          direction(FlexDirection.Row)
        } else {
          direction(FlexDirection.Column)
        }

        if (useHorizontalLayout) {
          gap(32.dp)
        } else {
          gap(24.dp)
        }

        alignItems(FlexAlignItems.Stretch)
      },
    ) {
      val hasImage = trip.imageUri != null || (trip.imageRes != null && trip.imageRes != 0)
      if (hasImage) {
        val model = trip.imageUri ?: trip.imageRes
        val imageShape = RoundedCornerShape(32.dp)
        val imageModifier =
          if (useHorizontalLayout) {
            Modifier.flex { basis(0.5f) }.aspectRatio(1.31f)
          } else {
            Modifier.fillMaxWidth().requiredHeight(200.dp)
          }

        if (model is Int) {
          Image(
            painter = painterResource(id = model),
            contentDescription = trip.title,
            modifier = imageModifier.clip(imageShape),
            contentScale = ContentScale.Crop,
          )
        } else {
          AsyncImage(
            model = model,
            contentDescription = trip.title,
            modifier = imageModifier.clip(imageShape),
            contentScale = ContentScale.Crop,
          )
        }
      }

      val tabletBreakpoint by derivedMediaQuery { windowWidth >= 1200.dp }
      val spacers = if (tabletBreakpoint) 16.dp else 8.dp

      Column(
        modifier = Modifier.flex { grow(1f) },
        verticalArrangement = Arrangement.spacedBy(spacers),
      ) {
        val titleTextStyle =
          if (tabletBreakpoint) {
            MaterialTheme.typography.displaySmall
          } else {
            MaterialTheme.typography.titleLarge
          }

        if (showTitle) {
          Text(
            text = trip.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = titleTextStyle,
            fontFamily = SekuyaFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        val sdf = remember { SimpleDateFormat("MMM dd", Locale.US) }
        val sdfWithYear = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
        val formattedStart =
          try {
            sdf.format(Date(trip.startDate))
          } catch (_: Exception) {
            ""
          }
        val formattedEnd =
          try {
            sdfWithYear.format(Date(trip.endDate))
          } catch (_: Exception) {
            ""
          }

        TripInfoItem(icon = Icons.Rounded.DateRange, text = "$formattedStart – $formattedEnd")

        if (trip.location.isNotEmpty()) {
          TripInfoItem(icon = Icons.Rounded.LocationOn, text = trip.location)
        }

        if (participants.isNotEmpty()) {
          Spacer(Modifier.weight(1f))

          OverlappingParticipants(participants = participants)
        } else {
          Box(Modifier.requiredSize(40.dp))
        }
      }
    }
  }
}

@OptIn(ExperimentalMediaQueryApi::class)
@Composable
private fun TripInfoItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Surface(
      modifier = Modifier.size(32.dp),
      color = MaterialTheme.colorScheme.primary,
      shape = RoundedCornerShape(12.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onPrimary,
        )
      }
    }
    val tabletBreakpoint by derivedMediaQuery { windowWidth >= 1200.dp }
    val textStyle =
      if (tabletBreakpoint) {
        MaterialTheme.typography.bodyLarge
      } else {
        MaterialTheme.typography.bodyMedium
      }

    Text(text = text, style = textStyle, color = MaterialTheme.colorScheme.onSurface)
  }
}

sealed interface AvatarSource {
  data class Url(val url: String) : AvatarSource

  data class Resource(val resId: Int) : AvatarSource
}

private val participantAvatars =
  mapOf(
    "Alice" to AvatarSource.Resource(CoreUiR.drawable.avatar_01),
    "Bob" to AvatarSource.Resource(CoreUiR.drawable.avatar_02),
    "Charlie" to AvatarSource.Resource(CoreUiR.drawable.avatar_03),
    "David" to AvatarSource.Resource(CoreUiR.drawable.avatar_04),
    "Eva" to AvatarSource.Resource(CoreUiR.drawable.avatar_05),
    "Frank" to AvatarSource.Resource(CoreUiR.drawable.avatar_06),
    "Grace" to AvatarSource.Resource(CoreUiR.drawable.avatar_07),
    "Hank" to AvatarSource.Resource(CoreUiR.drawable.avatar_08),
    "Ivy" to AvatarSource.Resource(CoreUiR.drawable.avatar_09),
    "Jack" to AvatarSource.Resource(CoreUiR.drawable.avatar_10),
  )

private fun getAvatarForName(name: String): AvatarSource? = participantAvatars[name]

data class Participant(val name: String, val avatar: AvatarSource? = null)

@Composable
fun ParticipantAvatar(
  participant: Participant,
  modifier: Modifier = Modifier,
  cutoutNext: Boolean = false,
) {
  val model =
    when (val avatar = participant.avatar) {
      is AvatarSource.Url -> avatar.url
      is AvatarSource.Resource -> avatar.resId
      null -> null
    }

  Box(
    modifier =
      modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        if (cutoutNext) {
          val strokeWidth = 2.dp.toPx()
          val avatarRadius = size.width / 2
          val overlap = 10.dp.toPx()
          val nextCenter = size.width - overlap + avatarRadius

          drawCircle(
            color = Color.Transparent,
            radius = avatarRadius + strokeWidth,
            center = Offset(nextCenter, size.height / 2),
            blendMode = BlendMode.Clear,
          )
        }
      }
  ) {
    if (model != null) {
      AsyncImage(
        model = model,
        contentDescription = participant.name,
        modifier = Modifier.fillMaxSize().clip(CircleShape),
        contentScale = ContentScale.Crop,
      )
    } else {
      Box(
        modifier =
          Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondary, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = participant.name.firstOrNull()?.uppercase() ?: "",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSecondary,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
fun OverlappingParticipants(participants: List<Participant>, modifier: Modifier = Modifier) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
    participants.take(5).forEachIndexed { index, participant ->
      ParticipantAvatar(
        participant = participant,
        modifier = Modifier.requiredSize(40.dp),
        cutoutNext = index < participants.size - 1 && index < 4,
      )
    }
    if (participants.size > 5) {
      Box(
        modifier =
          Modifier.requiredSize(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "+${participants.size - 5}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun TripCardPreview() {
  JetPackerTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      TripCard(
        trip =
          Trip(
            id = "1",
            title = "California Summer Trip",
            location = "California",
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis() + 10 * 24 * 3600 * 1000L,
            imageRes = CoreUiR.drawable.img_california,
            participants = listOf("John", "Jane", "Bob", "Alice"),
          ),
        isSwipedOff = false,
        useHorizontalLayout = false,
        onClick = {},
        onSwipeToDelete = {},
      )
    }
  }
}
