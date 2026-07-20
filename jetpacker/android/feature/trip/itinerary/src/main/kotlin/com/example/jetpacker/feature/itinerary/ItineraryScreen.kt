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

package com.example.jetpacker.feature.itinerary

import com.example.jetpacker.core.ui.components.JetPackerFabConfig
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.core.ui.EventColors
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.core.ui.R
import com.example.jetpacker.core.ui.SekuyaFontFamily
import com.example.jetpacker.core.ui.TimelineAudioBorderBlue
import com.example.jetpacker.core.ui.TimelineNodeBorderGreen
import com.example.jetpacker.core.ui.components.JetPackerFab
import com.example.jetpacker.core.ui.components.JetPackerToolbar
import com.example.jetpacker.core.ui.components.JetPackerToolbarAction
import com.example.jetpacker.feature.itinerary.components.AddEventDialog
import com.example.jetpacker.feature.itinerary.components.DatePickerModal
import com.example.jetpacker.feature.itinerary.components.DeleteEventSheet
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.trips.Trip
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Composable screen displaying a list of events (flights, hotels, activities) in a chronological
 * timeline format. Includes swipe-to-dismiss deletion, inline audio notes, and floating action
 * button triggers for editing and creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
  modifier: Modifier = Modifier,
  tripId: String = "",
  contentPadding: PaddingValues = PaddingValues(0.dp),
  onBack: () -> Unit = {},
  onEditTripClick: () -> Unit = {},
  onEventClick: (String, EventType) -> Unit = { _, _ -> },
  onFabConfigChange: (JetPackerFabConfig?) -> Unit = {},
  onNavigateToDebug: () -> Unit = {},
  viewModel: ItineraryViewModel = hiltViewModel(),
) {
  LocalContext.current

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val fabContainerColor = MaterialTheme.colorScheme.secondary
  val fabContentColor = MaterialTheme.colorScheme.onSecondary

  var eventToDelete by remember { mutableStateOf<TimelineEvent?>(null) }

  LaunchedEffect(tripId) {
    if (tripId.isNotEmpty()) {
      viewModel.loadForTrip(tripId)
    }
  }

  LaunchedEffect(Unit) {
    onFabConfigChange(
      JetPackerFabConfig(
        icon = Icons.Rounded.Add,
        onClick = { viewModel.showAddEvent() },
        contentDescription = "Add Event",
        containerColor = fabContainerColor,
        contentColor = fabContentColor,
      )
    )
  }

  if (uiState.showAddEventDialog) {
    AddEventDialog(
      onDismiss = { viewModel.hideAddEvent() },
      onSave = { type, title, location, time, extraFields ->
        viewModel.addEvent(type, title, location, time, extraFields)
      },
    )
  }

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              uiState.trip?.title?.uppercase() ?: "ITINERARY",
              style =
                MaterialTheme.typography.titleLarge.copy(
                  fontSize = 22.sp,
                  fontFamily = SekuyaFontFamily,
                ),
              color = MaterialTheme.colorScheme.secondary,
              maxLines = 1,
              autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 22.sp),
            )

            uiState.trip?.location?.let { location ->
              Text(
                location,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = onEditTripClick) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit Trip")
          }
        },
        scrollBehavior = scrollBehavior,
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
      )
    },
    containerColor = Color.Transparent,
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize()) {
      AnimatedContent(
        targetState = uiState.items.isEmpty() && !uiState.isGenerating,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "itinerary_content",
      ) { isEmpty ->
        if (isEmpty) {
          EmptyItineraryView(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
          )
        } else {
          val expandedHeaders =
            remember(uiState.items) {
              val states = mutableStateMapOf<String, Boolean>()
              val referenceDate =
                FeatureFlags.OVERRIDE_CURRENT_TIME_MILLIS?.let {
                  Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                } ?: LocalDate.now()
              uiState.items.filterIsInstance<ItineraryUiListItem.Header>().forEach { header ->
                val localDate =
                  try {
                    LocalDate.parse(header.date)
                  } catch (_: Exception) {
                    null
                  }
                val isPast = localDate?.isBefore(referenceDate) ?: false
                states[header.date] = !isPast
              }
              if (states.values.none { it }) {
                states.keys.forEach { states[it] = true }
              }
              states
            }

          val groupedItems =
            remember(uiState.items) {
              val result = mutableListOf<Pair<ItineraryUiListItem.Header, List<TimelineEvent>>>()
              var currentHeader: ItineraryUiListItem.Header? = null
              var currentEvents = mutableListOf<TimelineEvent>()

              uiState.items.forEach { item ->
                when (item) {
                  is ItineraryUiListItem.Header -> {
                    currentHeader?.let { result.add(it to currentEvents) }
                    currentHeader = item
                    currentEvents = mutableListOf()
                  }
                  is ItineraryUiListItem.Event -> {
                    currentEvents.add(item.event)
                  }
                }
              }
              currentHeader?.let { result.add(it to currentEvents) }
              result
            }

          LazyColumn(
            modifier =
              Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .drawWithContent {
                  drawContent()
                  drawRect(
                    brush =
                      Brush.verticalGradient(
                        0f to Color.Black,
                        0.8f to Color.Black,
                        1f to Color.Transparent,
                      ),
                    blendMode = BlendMode.DstIn,
                  )
                },
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 120.dp),
          ) {
            val hasImage =
              uiState.trip?.imageUri != null ||
                (uiState.trip?.imageRes != null && uiState.trip?.imageRes != 0)
            if (hasImage) {
              item(key = "header_image") {
                HeaderImage(trip = uiState.trip, modifier = Modifier.animateItem())
              }
            }



            if (uiState.isGenerating && uiState.items.isEmpty()) {
              items(3, key = { "shimmer_$it" }) {
                TimelineShimmerItem(modifier = Modifier.animateItem())
              }
            }

            groupedItems.forEach { (header, events) ->
              val isExpanded = expandedHeaders[header.date] ?: true
              item(key = header.date) {
                DayHeader(
                  modifier = Modifier.animateItem(),
                  date = header.date,
                  dayNumber = header.dayNumber,
                  totalDays = header.totalDays,
                  isExpanded = isExpanded,
                  onToggle = { expandedHeaders[header.date] = !isExpanded },
                )
              }

              if (isExpanded) {
                items(events, key = { it.id }) { event ->
                  TimelineItem(
                    modifier = Modifier.animateItem(),
                    event = event,
                    isLastInDay = event == events.last(),
                    isSwipedOff = eventToDelete == event,
                    onClick = { onEventClick(event.id, event.type) },
                    onDelete = { eventToDelete = event },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (eventToDelete != null) {
    DeleteEventSheet(
      event = eventToDelete!!,
      onConfirm = {
        viewModel.deleteEvent(eventToDelete!!)
        eventToDelete = null
      },
      onDismiss = { eventToDelete = null },
    )
  }
}


@Composable
fun EmptyItineraryView(
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      Icons.Rounded.Flight,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Your itinerary is empty",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Start adding events using the + button below.",
      fontSize = 16.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun DayHeader(
  modifier: Modifier = Modifier,
  date: String,
  dayNumber: Int,
  totalDays: Int,
  isExpanded: Boolean,
  onToggle: () -> Unit,
) {
  val localDate =
    try {
      LocalDate.parse(date)
    } catch (_: Exception) {
      null
    }
  val dayOfWeek = localDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.US) ?: date
  val monthDay = localDate?.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)) ?: ""

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(onClick = onToggle)
        .padding(horizontal = 24.dp, vertical = 16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "Day $dayNumber/$totalDays",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
      )

      Spacer(Modifier.width(4.dp))

      Text(
        text = "•",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.width(4.dp))

      Text(
        text = dayOfWeek,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.width(4.dp))

      Text(
        text = "•",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.width(4.dp))

      Text(
        text = monthDay,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.weight(1f))

      Box(
        modifier =
          Modifier.size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "rotation")
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = if (isExpanded) "Collapse" else "Expand",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineItem(
  modifier: Modifier = Modifier,
  event: TimelineEvent,
  isLastInDay: Boolean,
  isSwipedOff: Boolean,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(
    modifier =
      modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp).height(IntrinsicSize.Max),
    verticalAlignment = Alignment.Top,
  ) {
    // Left timeline column
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(24.dp).fillMaxHeight(),
    ) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
          modifier =
            Modifier.size(16.dp)
              .border(4.dp, TimelineNodeBorderGreen, CircleShape)
              .padding(4.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary)
        )
      }

      if (!isLastInDay) {
        DashedLine(modifier = Modifier.weight(1f))
      } else {
        Spacer(modifier = Modifier.height(32.dp))
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    val dismissState =
      rememberSwipeToDismissBoxState(
        SwipeToDismissBoxValue.Settled,
        SwipeToDismissBoxDefaults.positionalThreshold,
      )

    LaunchedEffect(dismissState.currentValue) {
      if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
        onDelete()
      }
    }

    LaunchedEffect(isSwipedOff) {
      if (!isSwipedOff && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
      }
    }

    SwipeToDismissBox(
      state = dismissState,
      modifier = Modifier.weight(1f),
      enableDismissFromStartToEnd = false,
      backgroundContent = {
        val color =
          when (dismissState.dismissDirection) {
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
            else -> Color.Transparent
          }

        Box(
          modifier =
            Modifier.fillMaxSize()
              .padding(bottom = 24.dp)
              .clip(MaterialTheme.shapes.medium)
              .background(color),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(end = 16.dp),
          )
        }
      },
    ) {
      TimelineItemCard(
        event = event,
        onClick = onClick,
      )
    }
  }
}

/**
 * Card layout representing a timeline event inside the itinerary list.
 */
@Composable
fun TimelineItemCard(
  event: TimelineEvent,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
    shape = RoundedCornerShape(12.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      val (tagText, icon, eventColors) =
        when (event.type) {
          EventType.TRANSPORTATION ->
            Triple("TRANSIT", Icons.Rounded.Flight, EventColors.Flight)
          EventType.ACCOMMODATION -> Triple("STAY", Icons.Rounded.Hotel, EventColors.Hotel)
          EventType.FOOD_AND_DRINK -> Triple("EATS", Icons.Rounded.Restaurant, EventColors.Food)
          EventType.CAR_RENTAL -> Triple("RENTAL", Icons.Rounded.Event, EventColors.Activity)
          EventType.ACTIVITY -> Triple("ACTIVITY", Icons.Rounded.Star, EventColors.Activity)
          EventType.CULTURE -> Triple("CULTURE", Icons.Rounded.Star, EventColors.Museum)
          EventType.WORK -> Triple("WORK", Icons.Rounded.Event, EventColors.Activity)
        }

      Box(
        modifier =
          Modifier.size(50.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary, MaterialShapes.Burst.toShape()),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondary,
          modifier = Modifier.size(18.dp),
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        val tagColor = MaterialTheme.colorScheme.surface
        val tagTextColor = eventColors.content

        Text(
          text = tagText,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = tagTextColor,
          modifier =
            Modifier.background(tagColor, RoundedCornerShape(16.dp))
              .border(1.dp, tagTextColor, RoundedCornerShape(16.dp))
              .padding(horizontal = 8.dp, vertical = 2.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = event.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
        )

        event.description?.let { text ->
          if (text.isNotBlank()) {
            Text(
              text = text,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        event.audioNotes.forEach { extract ->
          if (extract.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHighest,
              border = BorderStroke(1.dp, TimelineAudioBorderBlue),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "“$extract”",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 0.4.sp,
                  ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(8.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun TimelineShimmerItem(modifier: Modifier = Modifier) {
  Row(
    modifier =
      modifier.fillMaxWidth().padding(start = 16.dp, end = 24.dp).height(IntrinsicSize.Max),
    verticalAlignment = Alignment.Top,
  ) {
    // Left timeline column
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(24.dp).fillMaxHeight(),
    ) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
          modifier =
            Modifier.size(16.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.outlineVariant),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
          )
        }
      }

      DashedLine(modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Content column
    Card(
      modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.elevatedCardColors(),
    ) {
      Column(modifier = Modifier.padding(24.dp)) {
        val brush = rememberShimmerBrush()

        Box(
          modifier =
            Modifier.size(width = 80.dp, height = 24.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(brush)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
          modifier =
            Modifier.fillMaxWidth(0.6f)
              .height(20.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(brush)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
          modifier =
            Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush)
        )
      }
    }
  }
}

@Composable
fun rememberShimmerBrush(): Brush {
  val shimmerColors =
    listOf(
      Color.LightGray.copy(alpha = 0.6f),
      Color.LightGray.copy(alpha = 0.2f),
      Color.LightGray.copy(alpha = 0.6f),
    )

  val transition = rememberInfiniteTransition(label = "shimmer")
  val translateAnim =
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

  return Brush.linearGradient(
    colors = shimmerColors,
    start = Offset(translateAnim.value - 500f, translateAnim.value - 500f),
    end = Offset(translateAnim.value, translateAnim.value),
  )
}

@Composable
fun DashedLine(modifier: Modifier = Modifier) {
  val dashedLineColor = MaterialTheme.colorScheme.onSurface

  Canvas(modifier = modifier.width(2.dp)) {
    val dashLength = 12.dp.toPx()
    val gapLength = 2.dp.toPx()
    drawLine(
      color = dashedLineColor,
      start = Offset(size.width / 2, 0f),
      end = Offset(size.width / 2, size.height),
      strokeWidth = 2.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f),
    )
  }
}

/**
 * Header image card for the trip itinerary list.
 */
@Composable
fun HeaderImage(
  trip: Trip?,
  modifier: Modifier = Modifier,
) {
  val model = trip?.imageUri ?: trip?.imageRes
  Box(modifier = modifier.fillMaxWidth().height(240.dp)) {
    AsyncImage(
      model = model,
      contentDescription = null,
      modifier =
        Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
          .fillMaxSize()
          .clip(RoundedCornerShape(40.dp)),
      contentScale = ContentScale.Crop,
    )
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ItineraryScreenPreview() {
  JetPackerTheme { ItineraryScreen() }
}
