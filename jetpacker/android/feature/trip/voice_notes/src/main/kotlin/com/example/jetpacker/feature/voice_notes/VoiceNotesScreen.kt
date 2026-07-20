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

package com.example.jetpacker.feature.voice_notes

import com.example.jetpacker.core.ui.components.JetPackerFabConfig
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.core.ui.SekuyaFontFamily
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.DummyData

data class VoiceNotePlaceholder(
  val id: String,
  val title: String,
  val date: String,
  val isError: Boolean = false,
  val errorMessage: String? = null,
)

@SuppressLint("NewApi", "GlobalCoroutineDispatchers")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNotesScreen(
  modifier: Modifier = Modifier,
  tripId: String,
  contentPadding: PaddingValues,
  onBack: () -> Unit = {},
  onFabConfigChange: (JetPackerFabConfig?) -> Unit = {},
  viewModel: VoiceNotesViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val events by viewModel.events.collectAsStateWithLifecycle()

  LaunchedEffect(tripId) {
    if (tripId.isNotEmpty()) {
      viewModel.loadForTrip(tripId)
    }
  }

  var hasRecordAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { isGranted -> hasRecordAudioPermission = isGranted },
    )

  val voiceInputManager = viewModel.voiceInputManager
  val voiceInputState by voiceInputManager.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(hasRecordAudioPermission) {
    voiceInputManager.setPermissionGranted(hasRecordAudioPermission)
  }

  val handleListenToggle: () -> Unit = {
    voiceInputManager.handleListenToggle(
      onPermissionRequired = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
      onResult = { originalText, translatedText ->
        viewModel.processVoiceNote(originalText, translatedText)
      },
    )
  }

  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary
  val onSecondary = MaterialTheme.colorScheme.onSecondary
  val onTertiary = MaterialTheme.colorScheme.onTertiary

  LaunchedEffect(voiceInputState.isReady, voiceInputState.isListening, voiceInputState.statusText) {
    onFabConfigChange(
      JetPackerFabConfig(
        onClick = handleListenToggle,
        containerColor = if (voiceInputState.isListening) tertiary else secondary,
        contentColor = if (voiceInputState.isListening) onTertiary else onSecondary,
        icon = if (voiceInputState.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
        contentDescription = if (voiceInputState.isListening) "Stop" else "Record",
      )
    )
  }

  DisposableEffect(Unit) { onDispose { voiceInputManager.cancelListening() } }

  var noteToDelete by remember {
    mutableStateOf<VoiceNoteEntity?>(null)
  }

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = {
        TopAppBar(
          title = {
            Text(
              "Voice notes",
              style =
                MaterialTheme.typography.titleLarge.copy(
                  fontFamily = SekuyaFontFamily,
                  fontSize = 22.sp,
                ),
              color = MaterialTheme.colorScheme.onSurface,
            )
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
          },
          scrollBehavior = scrollBehavior,
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
              scrolledContainerColor = Color.Transparent,
            ),
        )
      },
      containerColor = Color.Transparent,
    ) { innerPadding ->
      val voiceNotes by viewModel.voiceNotes.collectAsStateWithLifecycle(initialValue = emptyList())

      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (voiceNotes.isEmpty() && viewModel.processingNotes.isEmpty()) {
          EmptyVoiceNotesView(modifier = Modifier.fillMaxSize())
        } else {
          LazyColumn(
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
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
            contentPadding =
              PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(viewModel.processingNotes, key = { it.id }) { placeholder ->
              ProcessingVoiceNoteItem(placeholder = placeholder)
            }
            items(voiceNotes, key = { it.id }) { note ->
              VoiceNoteItem(
                note = note,
                events = events,
                isSwipedOff = noteToDelete == note,
                onDelete = { noteToDelete = note },
              )
            }
          }
        }
      }
    }

    if (noteToDelete != null) {
      DeleteVoiceNoteSheet(
        note = noteToDelete!!,
        onConfirm = {
          viewModel.deleteVoiceNote(noteToDelete!!)
          noteToDelete = null
        },
        onDismiss = { noteToDelete = null },
      )
    }

    // Transcription Overlay
    if (voiceInputState.showDialog || voiceInputState.isListening) {
      VoiceModeOverlay(
        contentPadding = contentPadding,
        statusText = voiceInputState.statusText,
        transcription = voiceInputState.transcription + voiceInputState.partialTranscription,
        translatedTranscription = voiceInputState.translatedTranscription,
        isListening = voiceInputState.isListening,
        isError = voiceInputState.isError,
        onDismiss = { voiceInputManager.cancelListening() },
      )
    }
  }
}

@Composable
fun VoiceModeOverlay(
  contentPadding: PaddingValues,
  statusText: String,
  transcription: String,
  translatedTranscription: String,
  isListening: Boolean,
  isError: Boolean,
  onDismiss: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "voice_note_gradient")
  val animationProgress by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(3000, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "animation_progress",
    )

  JetPackerTheme(darkTheme = false) {
    Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
      // 60% Surface Overlay (Scrim)
      Box(
        modifier =
          Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
      )

      // Animated Gradient Background (Aligned to bottom as per Figma)
      val errorContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
      val primary90 = MaterialTheme.colorScheme.primaryFixed
      val secondary90 = MaterialTheme.colorScheme.secondaryFixed
      val tertiary80 = MaterialTheme.colorScheme.tertiaryFixedDim

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(447.dp)
            .align(Alignment.BottomCenter)
            .blur(80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .background(
              Brush.linearGradient(
                0.0f to if (isError) errorContainer else primary90,
                0.36f to if (isError) errorContainer else secondary90,
                0.66f to if (isError) errorContainer else tertiary80,
                start = Offset(0f, 500f * animationProgress),
                end = Offset(1000f, 1000f - (500f * animationProgress)),
              )
            )
      )

      Column(
        modifier =
          Modifier.align(Alignment.BottomStart)
            .padding(horizontal = 32.dp)
            .padding(bottom = contentPadding.calculateBottomPadding() + 56.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
      ) {
        AnimatedContent(
          targetState =
            if (isError) "Something went wrong" else transcription.ifEmpty { statusText },
          transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { targetText ->
          Text(
            text = targetText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            color =
              if (isError) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          )
        }

        if (translatedTranscription.isNotEmpty() && !isError) {
          Spacer(modifier = Modifier.height(16.dp))
          AnimatedContent(
            targetState = translatedTranscription,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
          ) { targetTranslation ->
            Text(
              text = targetTranslation,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Medium,
              textAlign = TextAlign.Start,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isError) {
          Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun EmptyVoiceNotesView(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      Icons.Rounded.Mic,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "No voice notes yet",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Tap the mic button to start recording a note for your trip.",
      fontSize = 16.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun ProcessingVoiceNoteItem(placeholder: VoiceNotePlaceholder) {
  val shape = RoundedCornerShape(12.dp)
  Card(
    modifier =
      Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, shape).dropShadow(
        shape = shape
      ) {
        radius = 0f
        spread = 0f
        offset = Offset(x = 4.dp.toPx(), y = 6.dp.toPx())
        color = Color(0xFF20290A)
      },
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier.size(48.dp)
            .clip(CircleShape)
            .background(
              if (placeholder.isError) MaterialTheme.colorScheme.errorContainer
              else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center,
      ) {
        if (placeholder.isError) {
          Icon(
            Icons.Rounded.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
        } else {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (placeholder.isError) "Failed to process" else placeholder.title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.ExtraBold,
          color =
            if (placeholder.isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = placeholder.errorMessage ?: placeholder.date,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (placeholder.isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNoteItem(
  note: VoiceNoteEntity,
  events: List<TimelineEvent>,
  isSwipedOff: Boolean,
  onDelete: () -> Unit,
) {
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

  val shape = RoundedCornerShape(12.dp)
  var isExpanded by remember { mutableStateOf(false) }

  SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = false,
    backgroundContent = {
      val color =
        when (dismissState.dismissDirection) {
          SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
          else -> Color.Transparent
        }

      Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 16.dp).clip(shape).background(color),
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
    Card(
      modifier =
        Modifier.fillMaxWidth()
          .padding(bottom = 16.dp)
          .border(1.dp, MaterialTheme.colorScheme.outline, shape)
          .dropShadow(shape = shape) {
            radius = 0f
            spread = 0f
            offset = Offset(x = 4.dp.toPx(), y = 6.dp.toPx())
            color = Color(0xFF20290A)
          },
      shape = shape,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = note.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(8.dp),
              )
              .padding(12.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFF299EAF)),
            modifier = Modifier.padding(bottom = 8.dp),
          ) {
            Text(
              text = "Transcription",
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Medium,
            )
          }

          val separator = " |||| "
          val transParts = note.transcription.split(separator)
          val translatedText = transParts.getOrNull(0).orEmpty().trim()
          val originalText = transParts.getOrNull(1).orEmpty().trim()

          Text(
            text =
              if (translatedText.isNotEmpty()) "“$translatedText”" else "“${note.transcription}”",
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            style =
              MaterialTheme.typography.bodySmall.copy(
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.4.sp,
              ),
            color = MaterialTheme.colorScheme.onSurface,
          )

          if (originalText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "“$originalText”",
              maxLines = if (isExpanded) Int.MAX_VALUE else 3,
              overflow = TextOverflow.Ellipsis,
              style =
                MaterialTheme.typography.bodySmall.copy(
                  fontStyle = FontStyle.Italic,
                  letterSpacing = 0.4.sp,
                ),
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
          }

          TextButton(
            onClick = { isExpanded = !isExpanded },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(32.dp),
          ) {
            Text(
              text = if (isExpanded) "Collapse" else "Expand",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }

        val matches =
          remember(note.matchingEventsJson) {
            try {
              val json = org.json.JSONArray(note.matchingEventsJson)
              val list = mutableListOf<Pair<String, String>>()
              for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                list.add(Pair(obj.getString("eventId"), obj.getString("extract")))
              }
              list
            } catch (e: Exception) {
              emptyList<Pair<String, String>>()
            }
          }

        if (matches.isNotEmpty()) {
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Matching Events",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          matches.forEach { (eventId, extract) ->
            val matchedEvent = events.find { it.id == eventId }
            if (matchedEvent != null) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Text(
                  text = "• ${matchedEvent.title}: ",
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = extract,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteVoiceNoteSheet(
  note: VoiceNoteEntity,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
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
          imageVector = Icons.Rounded.Delete,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(36.dp),
        )
      }

      Text(
        "Delete Voice Note?",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )

      Text(
        "Are you sure you want to delete voice note '${note.title}'? This action cannot be undone.",
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
            "I am sure I want to delete this voice note.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(
          colors =
            ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
        ) {
          Text("Cancel")
        }
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

@Preview(showBackground = true)
@Composable
fun VoiceNoteItemPreview() {
  JetPackerTheme {
    VoiceNoteItem(
      note = DummyData.voiceNotes.first(),
      events = emptyList(),
      isSwipedOff = false,
      onDelete = {},
    )
  }
}
