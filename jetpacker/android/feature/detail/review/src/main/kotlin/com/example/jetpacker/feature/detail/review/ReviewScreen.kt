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

package com.example.jetpacker.feature.detail.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jetpacker.core.ui.EventColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
  placeName: String,
  placeId: String,
  onBack: () -> Unit = {},
  viewModel: ReviewScreenViewModel = hiltViewModel()
) {
  val selectedTopics by viewModel.selectedTopics.collectAsStateWithLifecycle()
  val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
  val generatedReviewText by viewModel.generatedReviewText.collectAsStateWithLifecycle()

  ReviewScreenContent(
    placeName = placeName,
    topics = viewModel.topics,
    selectedTopics = selectedTopics,
    isGenerating = isGenerating,
    generatedReviewText = generatedReviewText,
    onTopicSelected = { topic, positive -> viewModel.toggleTopic(topic, positive) },
    onGenerateReview = { viewModel.generateReview(placeName) },
    onGeneratedReviewTextChange = { viewModel.onGeneratedReviewTextChange(it) },
    onPostReview = { context -> viewModel.addReview(placeId, context) },
    onBack = onBack
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreenContent(
  placeName: String,
  topics: List<String>,
  selectedTopics: Set<Topic>,
  isGenerating: Boolean,
  generatedReviewText: String,
  onTopicSelected: (String, Boolean) -> Unit,
  onGenerateReview: () -> Unit,
  onGeneratedReviewTextChange: (String) -> Unit,
  onPostReview: (android.content.Context) -> Unit,
  onBack: () -> Unit = {}
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              "REVIEW",
              fontWeight = FontWeight.ExtraBold,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              placeName,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
      contentPadding = innerPadding,
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.elevatedCardColors(),
        ) {
          Column(modifier = Modifier.padding(24.dp)) {
            Text(
              text = "Review Topics",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Select topics to include in your AI generated review:",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            TopicSelection(
              topics = topics,
              selectedTopics = selectedTopics,
              onTopicSelected = onTopicSelected,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
              onClick = onGenerateReview,
              modifier = Modifier.fillMaxWidth().height(56.dp),
              shape = MaterialTheme.shapes.medium,
              enabled = selectedTopics.isNotEmpty() && !isGenerating,
            ) {
              if (isGenerating) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Review")
              }
            }

            AnimatedVisibility(visible = generatedReviewText.isNotBlank() || isGenerating) {
              Column {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                  value = generatedReviewText,
                  onValueChange = onGeneratedReviewTextChange,
                  modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                  placeholder = { Text("AI generated review...") },
                  shape = MaterialTheme.shapes.medium,
                )
                Spacer(modifier = Modifier.height(16.dp))

                val context = LocalContext.current
                Button(
                  onClick = { onPostReview(context) },
                  modifier = Modifier.align(Alignment.End),
                  shape = MaterialTheme.shapes.medium,
                ) {
                  Text("Post Review")
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(42.dp))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSelection(
  topics: List<String>,
  selectedTopics: Set<Topic>,
  onTopicSelected: (String, Boolean) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
    topics.forEach { topic ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = topic,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          val isPositiveSelected = selectedTopics.any { it.name == topic && it.positiveOpinion }
          val isNegativeSelected = selectedTopics.any { it.name == topic && !it.positiveOpinion }

          val negativeColors = EventColors.Shopping
          val positiveColors = EventColors.Food

          FilterChip(
            selected = isNegativeSelected,
            onClick = { onTopicSelected(topic, false) },
            label = {
              Icon(
                Icons.Filled.ThumbDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            shape = CircleShape,
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = negativeColors.container,
                selectedLabelColor = negativeColors.content,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                containerColor = Color.Transparent,
              ),
            border =
              FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isNegativeSelected,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = negativeColors.content,
                borderWidth = 1.dp,
              ),
          )

          FilterChip(
            selected = isPositiveSelected,
            onClick = { onTopicSelected(topic, true) },
            label = {
              Icon(Icons.Filled.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            shape = CircleShape,
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = positiveColors.container,
                selectedLabelColor = positiveColors.content,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                containerColor = Color.Transparent,
              ),
            border =
              FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isPositiveSelected,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = positiveColors.content,
                borderWidth = 1.dp,
              ),
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun TopicSelectionPreview() {
  MaterialTheme {
    TopicSelection(
      topics = listOf("Crowds", "Exhibits", "Guides", "Audio"),
      selectedTopics =
        setOf(
          Topic(name = "Crowds", positiveOpinion = false),
          Topic(name = "Exhibits", positiveOpinion = true),
        ),
      onTopicSelected = { _, _ -> },
    )
  }
}
