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

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.core.ui.components.JetPackerExtendedFloatingActionButton
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.feature.create_trip.CreateTripViewModel
import java.time.Instant
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
private val initMediaQuery = run {
  ComposeUiFlags.isMediaQueryIntegrationEnabled = true
  true
}

/**
 * Composable representing the home dashboard of the application.
 * Displays a list of user trips, handles voice-note permissions, and provides links
 * to trip creation and application debugging screens.
 */
@SuppressLint("NewApi", "ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMediaQueryApi::class)
@Composable
fun HomeScreen(
  modifier: Modifier = Modifier,
  onTripClick: (String) -> Unit = {},
  onTripCreated: (String) -> Unit = {},
  onNavigateToDebug: () -> Unit = {},
  onCreateTripClick: () -> Unit = {},
  viewModel: HomeViewModel = hiltViewModel(),
  createTripViewModel: CreateTripViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val activity = LocalContext.current as? ComponentActivity
  val isHiltActivity =
    remember(activity) {
      activity != null &&
        (activity.javaClass.name.contains("Hilt_") ||
          activity.javaClass.superclass?.name?.contains("Hilt_") == true ||
          activity.javaClass.interfaces.any { it.name.contains("GeneratedComponent") })
    }
  val sharedCreateTripViewModel: CreateTripViewModel =
    if (isHiltActivity && activity != null) {
      hiltViewModel(activity)
    } else {
      createTripViewModel
    }
  val createTripUiState by sharedCreateTripViewModel.uiState.collectAsStateWithLifecycle()

  var tripToDelete by remember { mutableStateOf<Trip?>(null) }

  if (tripToDelete != null) {
    DeleteTripSheet(
      trip = tripToDelete!!,
      onConfirm = {
        viewModel.deleteTrip(tripToDelete!!)
        tripToDelete = null
      },
      onDismiss = { tripToDelete = null },
    )
  }

  val foldableBreakpoint by derivedMediaQuery { windowWidth >= 600.dp }
  val tabletBreakpoint by derivedMediaQuery { windowWidth >= 1200.dp }

  Scaffold(
    modifier = modifier,
    bottomBar = {
      if (!tabletBreakpoint) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(vertical = 16.dp, horizontal = 24.dp),
          contentAlignment = if (foldableBreakpoint) Alignment.CenterEnd else Alignment.Center,
        ) {
          JetPackerExtendedFloatingActionButton(
            icon = Icons.Rounded.Add,
            label = "Create trip",
            onClick = onCreateTripClick,
          )
        }
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize()) {
      AnimatedContent(targetState = uiState.isLoading, label = "HomeScreenState") { isLoading ->
        if (isLoading) {
          LoadingState()
        } else {
          HomeScreenContent(
            trips = uiState.trips,
            tripToDelete = tripToDelete,
            paddingValues = innerPadding,
            onTripClick = onTripClick,
            onSwipeToDelete = { tripToDelete = it },
            onNavigateToDebug = onNavigateToDebug,
            onCreateClick = onCreateTripClick,
          )
        }
      }

    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteTripSheet(trip: Trip, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  var isConfirmChecked by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 48.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier.size(80.dp)
            .background(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Delete,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(36.dp),
        )
      }

      Text(
        "Delete Trip?",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )

      Card(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      ) {
        val model = trip.imageUri ?: trip.imageRes
        AsyncImage(
          model = model,
          placeholder = (model as? Int)?.let { painterResource(it) },
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }

      Text(
        "This action is permanent and will delete all events in the itinerary for '${trip.title}'.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier =
            Modifier.clickable { isConfirmChecked = !isConfirmChecked }
              .padding(horizontal = 8.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = isConfirmChecked,
            colors = CheckboxDefaults.colors(
              checkedColor = MaterialTheme.colorScheme.error,
              checkmarkColor = MaterialTheme.colorScheme.onError,
            ),
            onCheckedChange = { isConfirmChecked = it }
          )
          Text(
            "I am sure I want to delete this trip permanently.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(
          colors =
            ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
          onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
        Button(
          onClick = onConfirm,
          enabled = isConfirmChecked,
          modifier = Modifier.weight(1f),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          shape = MaterialTheme.shapes.medium,
        ) {
          Text("Delete")
        }
      }
    }
  }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingState() {
  val infiniteTransition = rememberInfiniteTransition(label = "shape_loading")

  // A collection of fun Material 3 shapes to loop through
  val shapes = remember {
    listOf(MaterialShapes.Square, MaterialShapes.Clover4Leaf, MaterialShapes.Cookie6Sided)
  }

  // Progress for morphing through the list of shapes (1.5s per transition)
  val animProgress by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = shapes.size.toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(1500 * shapes.size, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "morph_loop",
    )

  // Continuous rotation for added dynamic effect
  val rotation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(3000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "rotation",
    )

  val currentIndex = animProgress.toInt() % shapes.size
  val nextIndex = (currentIndex + 1) % shapes.size
  val morphProgress = animProgress % 1f

  val shapeA = shapes[currentIndex]
  val shapeB = shapes[nextIndex]
  val morph = remember(shapeA, shapeB) { Morph(shapeA, shapeB) }
  val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Box(
      modifier =
        Modifier.size(160.dp)
          .graphicsLayer { rotationZ = rotation } // Rotating around the center of the Box
          .drawWithCache {
            val path = morph.toPath(morphProgress).asComposePath()
            val matrix = Matrix()
            matrix.scale(size.width, size.height)
            path.transform(matrix)
            onDrawBehind { drawPath(path, color = primaryContainerColor) }
          }
    )
  }
}

@SuppressLint("NewApi")
@OptIn(
  ExperimentalFoundationApi::class,
  ExperimentalGridApi::class,
  ExperimentalMediaQueryApi::class,
)
@Composable
fun HomeScreenContent(
  trips: List<Trip>,
  tripToDelete: Trip?,
  onTripClick: (String) -> Unit,
  onSwipeToDelete: (Trip) -> Unit,
  onNavigateToDebug: () -> Unit,
  onCreateClick: () -> Unit,
  modifier: Modifier = Modifier,
  paddingValues: PaddingValues = PaddingValues(0.dp),
  currentTimeMillis: Long = FeatureFlags.OVERRIDE_CURRENT_TIME_MILLIS ?: System.currentTimeMillis(),
) {
  val currentTrip = remember(trips, currentTimeMillis) {
    trips.find { it.startDate <= currentTimeMillis && currentTimeMillis <= it.endDate }
  }
  val pastTrips = remember(trips, currentTrip, currentTimeMillis) {
    trips.filter { it != currentTrip && it.endDate < currentTimeMillis }
  }
  val upcomingTrips = remember(trips, currentTrip, currentTimeMillis) {
    trips.filter { it != currentTrip && it.startDate > currentTimeMillis }
  }
  Box(modifier = modifier.fillMaxSize()) {
    val tabletBreakpoint by derivedMediaQuery { windowWidth >= 1200.dp }

    when {
      tabletBreakpoint -> {
        TabletLayout(
          currentTrip = currentTrip,
          tripToDelete = tripToDelete,
          onTripClick = onTripClick,
          onSwipeToDelete = onSwipeToDelete,
          onCreateClick = onCreateClick,
          pastTrips = pastTrips,
          upcomingTrips = upcomingTrips,
        )
      }
      else -> {
        PhoneLayout(
          pastTrips = pastTrips,
          paddingValues = paddingValues,
          tripToDelete = tripToDelete,
          onTripClick = onTripClick,
          onSwipeToDelete = onSwipeToDelete,
          currentTrip = currentTrip,
          upcomingTrips = upcomingTrips,
          onCreateClick = onCreateClick,
        )
      }
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(
  name = "Phone Light - Standard Font",
  fontScale = 1.0f,
  showBackground = true,
  device = "spec:width=411dp,height=891dp,dpi=440",
  uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
  name = "Phone Dark - Standard Font",
  fontScale = 1.0f,
  showBackground = true,
  device = "spec:width=411dp,height=891dp,dpi=440",
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
  name = "Phone Light - Large Font",
  fontScale = 1.5f,
  showBackground = true,
  device = "spec:width=411dp,height=891dp,dpi=440",
  uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
  name = "Tablet Light - Standard Font",
  fontScale = 1.0f,
  showBackground = true,
  device = "spec:width=1280dp,height=800dp,dpi=240",
  uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
  name = "Tablet Dark - Standard Font",
  fontScale = 1.0f,
  showBackground = true,
  device = "spec:width=1280dp,height=800dp,dpi=240",
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun HomeScreenMultipreview() {
  ComposeUiFlags.isMediaQueryIntegrationEnabled = true
  JetPackerTheme {
    HomeScreenContent(
      trips = DummyData.trips,
      tripToDelete = null,
      onTripClick = {},
      onSwipeToDelete = {},
      onNavigateToDebug = {},
      onCreateClick = {},
    )
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
  ComposeUiFlags.isMediaQueryIntegrationEnabled = true
  JetPackerTheme {
    HomeScreenContent(
      onNavigateToDebug = {},
      trips = DummyData.trips,
      tripToDelete = null,
      onTripClick = {},
      onSwipeToDelete = {},
      onCreateClick = {},
      currentTimeMillis = Instant.parse("2026-05-19T12:00:00Z").toEpochMilli(),
    )
  }
}
