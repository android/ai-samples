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

package com.example.jetpacker.feature.assistant.documents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.ui.AdsBorderBrush
import com.example.jetpacker.core.ui.AdsIconBadgeDark
import com.example.jetpacker.core.ui.AdsIconBadgeLight
import com.example.jetpacker.core.ui.components.JetPackerFabConfig
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TravelDocument
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

enum class DocumentCategory(val label: String) {
  ALL("All"),
  TICKETS("Tickets & Passes"),
  BOOKINGS("Reservations"),
  IDS("Passports & IDs"),
  NOTES("Notes & Info"),
}

fun matchesCategory(doc: TravelDocument, category: DocumentCategory): Boolean {
  if (category == DocumentCategory.ALL) return true
  val combined = (doc.documentType + " " + doc.title).uppercase()
  return when (category) {
    DocumentCategory.ALL -> true
    DocumentCategory.TICKETS ->
      combined.contains("TICKET") ||
        combined.contains("FLIGHT") ||
        combined.contains("BOARDING") ||
        combined.contains("MUSEUM") ||
        combined.contains("PASS") ||
        combined.contains("EXHIBITION") ||
        combined.contains("CRUISE") ||
        combined.contains("TOUR") ||
        combined.contains("EVENT")
    DocumentCategory.BOOKINGS ->
      combined.contains("HOTEL") ||
        combined.contains("LODGING") ||
        combined.contains("RESORT") ||
        combined.contains("CAR") ||
        combined.contains("RENTAL") ||
        combined.contains("VEHICLE") ||
        combined.contains("BOOKING") ||
        combined.contains("CONFIRMATION")
    DocumentCategory.IDS ->
      combined.contains("PASSPORT") ||
        combined.contains("DRIVER") ||
        combined.contains("LICENSE") ||
        combined.contains("VISA") ||
        combined.contains("ESTA") ||
        combined.contains("IDENTITY") ||
        combined.contains("BADGE")
    DocumentCategory.NOTES ->
      combined.contains("WIFI") ||
        combined.contains("NOTE") ||
        combined.contains("MAP") ||
        combined.contains("MENU") ||
        combined.contains("FOOD") ||
        doc.documentType == "OTHER"
  }
}

enum class DocumentValidity {
  VALID,
  EXPIRING_SOON,
  EXPIRED,
  NONE,
}

fun isIdentityDocument(docType: String, title: String): Boolean {
  val combined = (docType + " " + title).uppercase()
  return combined.contains("PASSPORT") ||
    combined.contains("DRIVER") ||
    combined.contains("LICENSE") ||
    combined.contains("VISA") ||
    combined.contains("ESTA")
}

fun calculateValidity(expiryMillis: Long, isIdentity: Boolean = false): DocumentValidity {
  if (expiryMillis <= 0L) return DocumentValidity.NONE
  val now = System.currentTimeMillis()
  if (expiryMillis < now) return DocumentValidity.EXPIRED
  if (isIdentity) {
    val sixMonthsMillis = 180L * 24 * 60 * 60 * 1000
    if (expiryMillis - now < sixMonthsMillis) return DocumentValidity.EXPIRING_SOON
  }
  return DocumentValidity.VALID
}

fun formatExpiryDate(expiryMillis: Long): String {
  if (expiryMillis <= 0L) return "No expiration date"
  return try {
    val date = Instant.ofEpochMilli(expiryMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    date.format(DateTimeFormatter.ISO_LOCAL_DATE)
  } catch (e: Exception) {
    "Unknown"
  }
}

enum class TripDocFilter {
  THIS_TRIP,
  ALL,
}

/**
 * Screen displaying the traveler's stored travel documents, passports, visas, flight vouchers, and
 * imported travel files in a clean, modern Material 3 interface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  tripId: String? = null,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  onFabConfigChange: ((JetPackerFabConfig?) -> Unit)? = null,
  viewModel: DocumentsViewModel = hiltViewModel(),
) {
  val allDocuments by viewModel.documents.collectAsStateWithLifecycle()
  val selectedDoc by viewModel.selectedDocument.collectAsStateWithLifecycle()
  val previewContent by viewModel.previewContent.collectAsStateWithLifecycle()
  val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()
  val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
  val vaultQuery by viewModel.vaultQuery.collectAsStateWithLifecycle()
  val vaultResults by viewModel.vaultResults.collectAsStateWithLifecycle()
  val vaultSearchStatus by viewModel.vaultSearchStatus.collectAsStateWithLifecycle()
  val isSearchingVault by viewModel.isSearchingVault.collectAsStateWithLifecycle()

  val tripEventsFlow =
    remember(tripId) {
      if (tripId != null) viewModel.getEventsForTrip(tripId) else flowOf(emptyList())
    }
  val tripEvents by tripEventsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

  var selectedFilter by remember { mutableStateOf(TripDocFilter.THIS_TRIP) }
  var selectedCategory by remember { mutableStateOf(DocumentCategory.ALL) }

  val filteredByScope =
    remember(allDocuments, tripId, selectedFilter) {
      if (tripId == null || selectedFilter == TripDocFilter.ALL) {
        allDocuments
      } else {
        allDocuments.filter { it.tripId == tripId }
      }
    }

  val browsableDocuments =
    remember(filteredByScope, selectedCategory) {
      if (selectedCategory == DocumentCategory.ALL) {
        filteredByScope
      } else {
        filteredByScope.filter { matchesCategory(it, selectedCategory) }
      }
    }

  val documents = vaultResults ?: browsableDocuments
  val isVaultSearchActive = vaultResults != null

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  var showAddDialog by remember { mutableStateOf(false) }
  var showFilterDialog by remember { mutableStateOf(false) }
  var showCreateTextDialog by remember { mutableStateOf(false) }
  var documentToDelete by remember { mutableStateOf<TravelDocument?>(null) }
  var documentToLink by remember { mutableStateOf<TravelDocument?>(null) }

  val hasActiveFilters =
    selectedCategory != DocumentCategory.ALL ||
      (tripId != null && selectedFilter != TripDocFilter.THIS_TRIP)

  if (onFabConfigChange != null) {
    LaunchedEffect(onFabConfigChange) {
      onFabConfigChange(
        JetPackerFabConfig(
          icon = Icons.Rounded.Add,
          onClick = { showAddDialog = true },
          contentDescription = "Add Document",
        )
      )
    }
  }

  val filePickerLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        viewModel.importDocument(uri = uri, tripId = tripId)
      }
    }

  val context = LocalContext.current
  var pendingPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }

  val cameraLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
      if (success) {
        pendingPhotoUri?.let { uriString ->
          val uri = Uri.parse(uriString)
          viewModel.scanDocumentFromUri(uri = uri, tripId = tripId)
        }
      }
    }

  val launchCamera: () -> Unit = {
    try {
      val capturesDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
      val photoFile =
        File(capturesDir, "capture_${System.currentTimeMillis()}.jpg").apply { createNewFile() }
      val uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
      pendingPhotoUri = uri.toString()
      cameraLauncher.launch(uri)
    } catch (e: Exception) {
      Log.e("DocumentsScreen", "Failed to launch camera capture", e)
    }
  }

  LaunchedEffect(userMessage) {
    userMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearUserMessage()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            if (tripId != null) "Trip Documents" else "Travel Documents",
            fontWeight = FontWeight.SemiBold,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { showAddDialog = true }) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Document")
          }
          IconButton(onClick = { viewModel.seedDefaultDocuments() }) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Demo Documents")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )
    },
    floatingActionButton = {
      if (onFabConfigChange == null) {
        ExtendedFloatingActionButton(
          onClick = { showAddDialog = true },
          icon = { Icon(Icons.Default.Add, contentDescription = null) },
          text = { Text("Add Document") },
        )
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
      )
    },
  ) { paddingValues ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            top = paddingValues.calculateTopPadding(),
            bottom =
              maxOf(paddingValues.calculateBottomPadding(), contentPadding.calculateBottomPadding()),
          )
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        DocumentSearchBar(
          query = vaultQuery,
          isSearching = isSearchingVault,
          statusLine = vaultSearchStatus,
          hasActiveFilters = hasActiveFilters,
          onQueryChange = { q ->
            viewModel.onVaultQueryChange(
              q,
              tripId = tripId.takeIf { selectedFilter == TripDocFilter.THIS_TRIP },
            )
          },
          onSearch = {
            viewModel.searchVault(tripId.takeIf { selectedFilter == TripDocFilter.THIS_TRIP })
          },
          onClear = viewModel::clearVaultSearch,
          onFilterClick = { showFilterDialog = true },
        )

        // Show a compact active filter badge row ONLY if filters are active and search is not overriding
        if (hasActiveFilters && !isVaultSearchActive) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (tripId != null && selectedFilter == TripDocFilter.ALL) {
              FilterChip(
                selected = true,
                onClick = { showFilterDialog = true },
                label = { Text("All Documents") },
                trailingIcon = {
                  IconButton(
                    onClick = { selectedFilter = TripDocFilter.THIS_TRIP },
                    modifier = Modifier.size(16.dp),
                  ) {
                    Icon(
                      Icons.Rounded.Close,
                      contentDescription = "Clear scope filter",
                      modifier = Modifier.size(12.dp),
                    )
                  }
                },
              )
            }
            if (selectedCategory != DocumentCategory.ALL) {
              FilterChip(
                selected = true,
                onClick = { showFilterDialog = true },
                label = { Text(selectedCategory.label) },
                trailingIcon = {
                  IconButton(
                    onClick = { selectedCategory = DocumentCategory.ALL },
                    modifier = Modifier.size(16.dp),
                  ) {
                    Icon(
                      Icons.Rounded.Close,
                      contentDescription = "Clear category filter",
                      modifier = Modifier.size(12.dp),
                    )
                  }
                },
              )
            }
          }
        }

        Box(modifier = Modifier.fillMaxSize()) {
          if (documents.isEmpty() && !isVaultSearchActive) {
            EmptyDocumentsState(
              onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
              onScanCamera = launchCamera,
              onAddText = { showCreateTextDialog = true },
              onSeedDefaults = { viewModel.seedDefaultDocuments() },
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding =
                PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              item(key = "header_summary") {
                Text(
                  text =
                    if (isVaultSearchActive) {
                      "Best matches for \u201C$vaultQuery\u201D"
                    } else {
                      "${documents.size} saved documents"
                    },
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
              }

              if (isVaultSearchActive && documents.isEmpty()) {
                item(key = "no_matches") {
                  Text(
                    text = "No matching documents found. Try different keywords or add a new document.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp),
                  )
                }
              }

              items(documents, key = { it.id }) { doc ->
                DocumentCard(
                  document = doc,
                  tripId = tripId,
                  events = tripEvents,
                  loadCardBitmap = viewModel::loadCardBitmap,
                  onInspect = { viewModel.selectDocumentForPreview(doc) },
                  onDelete = { documentToDelete = doc },
                  onLinkToEvent = { documentToLink = doc },
                  onUnlinkEvent = {
                    viewModel.linkDocumentToEvent(doc.documentId, null, tripId = tripId)
                  },
                  onAddToTrip = { viewModel.linkDocumentToTrip(doc.documentId, tripId) },
                  onCopyText = { text ->
                    val clipboard =
                      context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Document Info", text))
                    coroutineScope.launch {
                      snackbarHostState.showSnackbar("Copied $text to clipboard")
                    }
                  },
                  onShareDocument = {
                    val shareIntent =
                      Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, doc.title)
                        putExtra(
                          Intent.EXTRA_TEXT,
                          "${doc.title}\n${doc.documentType} • ${doc.issuingCountry}\n${doc.notes}",
                        )
                      }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                  },
                )
              }
            }
          }
        }
      }

      if (isProcessing) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      }
    }
  }

  if (selectedDoc != null) {
    DocumentPreviewSheet(
      document = selectedDoc!!,
      content = previewContent,
      bitmap = previewBitmap,
      onDismiss = { viewModel.selectDocumentForPreview(null) },
      onLinkToEvent = {
        val doc = selectedDoc
        viewModel.selectDocumentForPreview(null)
        documentToLink = doc
      },
      onDelete = {
        val doc = selectedDoc
        viewModel.selectDocumentForPreview(null)
        documentToDelete = doc
      },
      onCopyText = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Document Info", text))
        coroutineScope.launch { snackbarHostState.showSnackbar("Copied $text to clipboard") }
      },
      onShare = {
        val doc = selectedDoc!!
        val shareIntent =
          Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, doc.title)
            putExtra(Intent.EXTRA_TEXT, previewContent ?: "${doc.title}\n${doc.notes}")
          }
        context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
      },
    )
  }

  if (showAddDialog) {
    AddDocumentOptionsDialog(
      onDismiss = { showAddDialog = false },
      onPickFile = {
        showAddDialog = false
        filePickerLauncher.launch(arrayOf("*/*"))
      },
      onScanCamera = {
        showAddDialog = false
        launchCamera()
      },
      onCreateText = {
        showAddDialog = false
        showCreateTextDialog = true
      },
    )
  }

  if (showFilterDialog) {
    DocumentFilterDialog(
      tripId = tripId,
      selectedFilter = selectedFilter,
      selectedCategory = selectedCategory,
      onFilterChange = { selectedFilter = it },
      onCategoryChange = { selectedCategory = it },
      onReset = {
        selectedFilter = TripDocFilter.THIS_TRIP
        selectedCategory = DocumentCategory.ALL
      },
      onDismiss = { showFilterDialog = false },
    )
  }

  if (showCreateTextDialog) {
    CreateTextDocumentDialog(
      onDismiss = { showCreateTextDialog = false },
      events = tripEvents,
      onSave = { title, content, type, country, expiry, number, eventId ->
        viewModel.createTextDocument(
          title = title,
          content = content,
          documentType = type,
          issuingCountry = country,
          expiryDate = expiry,
          documentNumberMasked = number,
          tripId = tripId,
          eventId = eventId,
        )
        showCreateTextDialog = false
      },
    )
  }

  if (documentToLink != null) {
    AlertDialog(
      onDismissRequest = { documentToLink = null },
      title = { Text("Link Document to Event") },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Select an event to link to \"${documentToLink!!.title}\":",
            style = MaterialTheme.typography.bodyMedium,
          )
          if (tripEvents.isEmpty()) {
            Text(
              text = "No events in this trip yet.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            tripEvents.forEach { event ->
              OutlinedCard(
                onClick = {
                  viewModel.linkDocumentToEvent(
                    documentToLink!!.documentId,
                    event.id,
                    tripId = tripId,
                  )
                  documentToLink = null
                },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = event.title,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "${event.type.name.replace('_', ' ')} • ${event.location}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = { TextButton(onClick = { documentToLink = null }) { Text("Cancel") } },
    )
  }

  if (documentToDelete != null) {
    AlertDialog(
      onDismissRequest = { documentToDelete = null },
      title = { Text("Delete Document") },
      text = {
        Text(
          "Are you sure you want to delete \"${documentToDelete!!.title}\"? The local file will be removed."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            documentToDelete?.let { viewModel.deleteDocument(it) }
            documentToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { documentToDelete = null }) { Text("Cancel") } },
    )
  }
}

/** Clean, modern search bar for querying documents with integrated filter button. */
@Composable
private fun DocumentSearchBar(
  query: String,
  isSearching: Boolean,
  statusLine: String?,
  hasActiveFilters: Boolean,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  onClear: () -> Unit,
  onFilterClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.weight(1f),
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        placeholder = { Text("Search tickets, bookings, IDs...", maxLines = 1) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search documents",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        trailingIcon = {
          when {
            isSearching ->
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            query.isNotEmpty() ->
              IconButton(onClick = onClear) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear search")
              }
          }
        },
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
          ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
      )

      Surface(
        onClick = onFilterClick,
        shape = CircleShape,
        color =
          if (hasActiveFilters) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
          },
        contentColor =
          if (hasActiveFilters) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.size(52.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Filter documents",
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }

    if (statusLine != null && query.isNotEmpty()) {
      Text(
        text = statusLine,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
      )
    }
  }
}

/** Dialog for scoping and categorizing travel documents cleanly without cluttering the screen. */
@Composable
private fun DocumentFilterDialog(
  tripId: String?,
  selectedFilter: TripDocFilter,
  selectedCategory: DocumentCategory,
  onFilterChange: (TripDocFilter) -> Unit,
  onCategoryChange: (DocumentCategory) -> Unit,
  onReset: () -> Unit,
  onDismiss: () -> Unit,
) {
  var tempFilter by remember { mutableStateOf(selectedFilter) }
  var tempCategory by remember { mutableStateOf(selectedCategory) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Filter Documents", style = MaterialTheme.typography.titleLarge)
        if (tempFilter != TripDocFilter.THIS_TRIP || tempCategory != DocumentCategory.ALL) {
          TextButton(
            onClick = {
              tempFilter = TripDocFilter.THIS_TRIP
              tempCategory = DocumentCategory.ALL
            }
          ) {
            Text("Reset")
          }
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        if (tripId != null) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Trip Scope",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { tempFilter = TripDocFilter.THIS_TRIP }
                  .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = tempFilter == TripDocFilter.THIS_TRIP,
                onClick = { tempFilter = TripDocFilter.THIS_TRIP },
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "This trip only",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                )
                Text(
                  "Only show documents added for this trip",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { tempFilter = TripDocFilter.ALL }
                  .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = tempFilter == TripDocFilter.ALL,
                onClick = { tempFilter = TripDocFilter.ALL },
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "All documents",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                )
                Text(
                  "Show documents from all trips and global storage",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          HorizontalDivider()
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )

          val categories =
            listOf(
              Triple(DocumentCategory.ALL, "All Categories", Icons.Default.FolderOpen),
              Triple(DocumentCategory.TICKETS, "Tickets & Passes", Icons.Default.ConfirmationNumber),
              Triple(DocumentCategory.BOOKINGS, "Reservations", Icons.Default.Hotel),
              Triple(DocumentCategory.IDS, "Passports & IDs", Icons.Default.Badge),
              Triple(DocumentCategory.NOTES, "Notes & Info", Icons.Default.Notes),
            )

          categories.forEach { (cat, label, catIcon) ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { tempCategory = cat }
                  .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = tempCategory == cat,
                onClick = { tempCategory = cat },
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = catIcon,
                contentDescription = null,
                tint =
                  if (tempCategory == cat) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (tempCategory == cat) FontWeight.SemiBold else FontWeight.Normal,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onFilterChange(tempFilter)
          onCategoryChange(tempCategory)
          onDismiss()
        }
      ) {
        Text("Apply")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

/** Tonal, modern document card with intuitive layout, quick copy, and overflow options. */
@Composable
private fun DocumentCard(
  document: TravelDocument,
  onInspect: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  tripId: String? = null,
  events: List<TimelineEvent> = emptyList(),
  loadCardBitmap: suspend (TravelDocument) -> Bitmap? = { null },
  onLinkToEvent: () -> Unit = {},
  onUnlinkEvent: () -> Unit = {},
  onAddToTrip: () -> Unit = {},
  onCopyText: (String) -> Unit = {},
  onShareDocument: () -> Unit = {},
) {
  val isIdentity = isIdentityDocument(document.documentType, document.title)
  val validity = calculateValidity(document.expiryDate, isIdentity = isIdentity)
  val icon = getDocumentIcon(document.documentType, document.mimeType, document.title)
  val cardBitmap by
    produceState<Bitmap?>(initialValue = null, document.filePath, document.mimeType) {
      value = loadCardBitmap(document)
    }

  var showMenu by remember { mutableStateOf(false) }
  val linkedEvent = events.find { it.id == document.eventId }

  Card(
    onClick = onInspect,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, AdsBorderBrush),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      // Top header row: Icon + Title/Subtitle + Overflow Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = AdsIconBadgeLight,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AdsIconBadgeDark,
                modifier = Modifier.size(22.dp),
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = document.title.ifBlank { document.documentId },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )

            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 2.dp),
            ) {
              Text(
                text = document.documentType.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                Text(
                  text = formatDocumentModalityLabel(document.mimeType, document.filePath),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
              }
              if (document.issuingCountry.isNotBlank()) {
                Text(
                  text = "• ${document.issuingCountry}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        Box {
          IconButton(onClick = { showMenu = true }) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (tripId != null) {
              if (document.tripId == tripId) {
                if (linkedEvent != null) {
                  DropdownMenuItem(
                    text = { Text("Unlink from Event") },
                    onClick = {
                      showMenu = false
                      onUnlinkEvent()
                    },
                    leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                  )
                }
                DropdownMenuItem(
                  text = { Text(if (linkedEvent != null) "Change Event" else "Link to Event") },
                  onClick = {
                    showMenu = false
                    onLinkToEvent()
                  },
                  leadingIcon = { Icon(Icons.Rounded.Event, contentDescription = null) },
                )
              } else {
                DropdownMenuItem(
                  text = { Text("Add to this Trip") },
                  onClick = {
                    showMenu = false
                    onAddToTrip()
                  },
                  leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                )
              }
            }
            DropdownMenuItem(
              text = { Text("Share") },
              onClick = {
                showMenu = false
                onShareDocument()
              },
              leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            )
            DropdownMenuItem(
              text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
              onClick = {
                showMenu = false
                onDelete()
              },
              leadingIcon = {
                Icon(
                  Icons.Default.DeleteOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
              },
            )
          }
        }
      }

      // Middle metadata chips row: Document number with quick-copy + Validity
      if (document.documentNumberMasked.isNotBlank() || validity != DocumentValidity.NONE) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (document.documentNumberMasked.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              onClick = { onCopyText(document.documentNumberMasked) },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text =
                    if (document.documentType == "WIFI") "WiFi: ${document.documentNumberMasked}"
                    else "ID: ${document.documentNumberMasked}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copy",
                  modifier = Modifier.size(12.dp),
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

          when (validity) {
            DocumentValidity.EXPIRING_SOON -> {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF3B2A08),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(13.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Expires soon (${formatExpiryDate(document.expiryDate)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
            DocumentValidity.EXPIRED -> {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF421114),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFB4AB),
                    modifier = Modifier.size(13.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Expired (${formatExpiryDate(document.expiryDate)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFB4AB),
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
            DocumentValidity.VALID -> {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF063E20),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF6DD58C),
                    modifier = Modifier.size(13.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Valid (${formatExpiryDate(document.expiryDate)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6DD58C),
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
            DocumentValidity.NONE -> {}
          }
        }
      }

      // Visual bitmap preview (image / rendered PDF)
      if (cardBitmap != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier =
            Modifier.fillMaxWidth().heightIn(max = 170.dp).clip(RoundedCornerShape(12.dp)),
        ) {
          Image(
            bitmap = cardBitmap!!.asImageBitmap(),
            contentDescription = document.title,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      // Notes summary
      if (document.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = document.notes,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Linked event tag
      if (linkedEvent != null && tripId != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Event,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Linked to: ${linkedEvent.title}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

/** Comprehensive, modern bottom sheet for inspecting travel documents, passes, and emails. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentPreviewSheet(
  document: TravelDocument,
  content: String?,
  bitmap: Bitmap?,
  onDismiss: () -> Unit,
  onLinkToEvent: () -> Unit,
  onDelete: () -> Unit,
  onCopyText: (String) -> Unit,
  onShare: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val icon = getDocumentIcon(document.documentType, document.mimeType, document.title)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 32.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(48.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = document.title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                "${document.documentType.replace('_', ' ')} • ${formatDocumentModalityLabel(document.mimeType, document.filePath)} • ${document.issuingCountry.ifBlank { "Travel Document" }}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (document.documentNumberMasked.isNotBlank()) {
          FilledTonalButton(
            onClick = { onCopyText(document.documentNumberMasked) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy ID", maxLines = 1)
          }
        }
        OutlinedButton(
          onClick = onShare,
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share", maxLines = 1)
        }
      }

      // Visual bitmap display
      if (bitmap != null) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = document.title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
          )
        }
      }

      // Formatted content (for text files, emails, or fallback when no rendered bitmap)
      if (bitmap == null) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (content == null) {
              CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
              SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (content.contains("Subject:") || content.contains("From:")) {
                    // Formatted email card
                    val lines = content.lines()
                    val subjectLine = lines.find { it.startsWith("Subject:", ignoreCase = true) }
                    val fromLine = lines.find { it.startsWith("From:", ignoreCase = true) }
                    val dateLine = lines.find { it.startsWith("Date:", ignoreCase = true) }

                    if (subjectLine != null) {
                      Text(
                        text = subjectLine.substringAfter(':').trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                    if (fromLine != null || dateLine != null) {
                      Text(
                        text =
                          listOfNotNull(
                              fromLine?.substringAfter(':')?.trim(),
                              dateLine?.substringAfter(':')?.trim(),
                            )
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                  }

                  Text(
                    text = content,
                    style =
                      MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
      }

      // Bottom actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onDelete,
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
          Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete")
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onDismiss) { Text("Done") }
      }
    }
  }
}

@Composable
private fun EmptyDocumentsState(
  onPickFile: () -> Unit,
  onScanCamera: () -> Unit,
  onAddText: () -> Unit,
  onSeedDefaults: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(72.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.FolderOpen,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(36.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "No Travel Documents Yet",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text =
        "Keep your flight confirmations, tickets, hotel bookings, and travel IDs organized in one place for easy access on your trip.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onScanCamera, modifier = Modifier.fillMaxWidth(0.85f)) {
      Icon(Icons.Default.PhotoCamera, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Scan Document")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onPickFile, modifier = Modifier.fillMaxWidth(0.85f)) {
      Icon(Icons.Default.UploadFile, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Import Document File")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onAddText, modifier = Modifier.fillMaxWidth(0.85f)) {
      Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Create Text Document")
    }
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onSeedDefaults) {
      Icon(Icons.Default.Refresh, contentDescription = null)
      Spacer(modifier = Modifier.width(6.dp))
      Text("Load Sample Documents")
    }
  }
}

@Composable
private fun AddDocumentOptionsDialog(
  onDismiss: () -> Unit,
  onPickFile: () -> Unit,
  onScanCamera: () -> Unit,
  onCreateText: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Travel Document") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedCard(onClick = onScanCamera, modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.PhotoCamera,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text("Scan Document with Camera", fontWeight = FontWeight.Bold)
              Text(
                "Capture passport, driver's license, ticket, or confirmation photo",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }

        OutlinedCard(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.UploadFile,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text("Import File from Device", fontWeight = FontWeight.Bold)
              Text(
                "Import PDF, image, email (.eml / HTML), or text voucher",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }

        OutlinedCard(onClick = onCreateText, modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.AutoMirrored.Filled.NoteAdd,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text("Create Text Note / Confirmation", fontWeight = FontWeight.Bold)
              Text(
                "Paste booking confirmations, voucher text, or passport info",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun CreateTextDocumentDialog(
  onDismiss: () -> Unit,
  events: List<TimelineEvent> = emptyList(),
  onSave:
    (
      title: String,
      content: String,
      type: String,
      country: String,
      expiry: Long,
      number: String,
      eventId: String?,
    ) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var type by remember { mutableStateOf("OTHER") }
  var content by remember { mutableStateOf("") }
  var country by remember { mutableStateOf("") }
  var number by remember { mutableStateOf("") }
  var selectedEventId by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Travel Document") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title (e.g. Flight Confirmation UA 991)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OutlinedButton(
            onClick = { type = "PASSPORT" },
            modifier = Modifier.weight(1f),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor =
                  if (type == "PASSPORT") MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
              ),
            contentPadding = PaddingValues(horizontal = 4.dp),
          ) {
            Text("Passport", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = { type = "DRIVER_LICENSE" },
            modifier = Modifier.weight(1.1f),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor =
                  if (type == "DRIVER_LICENSE") MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
              ),
            contentPadding = PaddingValues(horizontal = 4.dp),
          ) {
            Text("License", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = { type = "FLIGHT" },
            modifier = Modifier.weight(1f),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor =
                  if (type == "FLIGHT") MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
              ),
            contentPadding = PaddingValues(horizontal = 4.dp),
          ) {
            Text("Flight", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = { type = "HOTEL" },
            modifier = Modifier.weight(0.9f),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor =
                  if (type == "HOTEL") MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
              ),
            contentPadding = PaddingValues(horizontal = 4.dp),
          ) {
            Text("Hotel", style = MaterialTheme.typography.labelSmall)
          }
        }

        OutlinedTextField(
          value = country,
          onValueChange = { country = it },
          label = { Text("Country / Airline / Venue (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = number,
          onValueChange = { number = it },
          label = { Text("Confirmation / Document Number (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        if (events.isNotEmpty()) {
          Text(
            text = "Link to Itinerary Event (optional):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = selectedEventId == null,
              onClick = { selectedEventId = null },
              label = { Text("None") },
            )
            events.forEach { ev ->
              FilterChip(
                selected = selectedEventId == ev.id,
                onClick = { selectedEventId = ev.id },
                label = { Text(ev.title) },
              )
            }
          }
        }

        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Content / Confirmation Details") },
          minLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank() && content.isNotBlank()) {
            onSave(
              title.trim(),
              content.trim(),
              type,
              country.trim(),
              0L,
              number.trim(),
              selectedEventId,
            )
          }
        },
        enabled = title.isNotBlank() && content.isNotBlank(),
      ) {
        Text("Save Document")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

private fun getDocumentIcon(
  documentType: String,
  mimeType: String,
  title: String = "",
): ImageVector {
  val upper = (documentType + " " + title).uppercase()
  return when {
    upper.contains("MUSEUM") || upper.contains("EXHIBITION") || upper.contains("ART") ->
      Icons.Default.Museum
    upper.contains("HOTEL") ||
      upper.contains("LODGING") ||
      upper.contains("RESORT") ||
      upper.contains("ACCOMMODATION") -> Icons.Default.Hotel
    upper.contains("CAR") ||
      upper.contains("RENTAL") ||
      upper.contains("VEHICLE") ||
      upper.contains("POLESTAR") ||
      upper.contains("ZEPHYR") -> Icons.Default.DirectionsCar
    upper.contains("DRIVER") || upper.contains("LICENSE") -> Icons.Default.DirectionsCar
    upper.contains("PASSPORT") || upper.contains("ESTA") -> Icons.Default.Badge
    upper.contains("FLIGHT") ||
      upper.contains("AIRLINE") ||
      upper.contains("BOARDING") ||
      upper.contains("AIRPORT") -> Icons.Default.Flight
    upper.contains("TRAIN") ||
      upper.contains("TRANSIT") ||
      upper.contains("METRO") ||
      upper.contains("SUBWAY") -> Icons.Default.Train
    upper.contains("WIFI") || upper.contains("INTERNET") -> Icons.Default.Wifi
    upper.contains("VISA") -> Icons.Default.CheckCircle
    upper.contains("TICKET") ||
      upper.contains("PASS") ||
      upper.contains("CRUISE") ||
      upper.contains("TOUR") ||
      upper.contains("EVENT") -> Icons.Default.ConfirmationNumber
    upper.contains("PARKING") -> Icons.Default.LocalParking
    upper.contains("MENU") ||
      upper.contains("FOOD") ||
      upper.contains("RESTAURANT") ||
      upper.contains("DINING") -> Icons.Default.Restaurant
    upper.contains("MAP") || upper.contains("TRAIL") -> Icons.Default.Map
    upper.contains("LUGGAGE") || upper.contains("BAGGAGE") -> Icons.Default.Luggage
    mimeType.contains("pdf") -> Icons.Default.Description
    else -> Icons.Default.Description
  }
}

private fun formatDocumentModalityLabel(mimeType: String, filePath: String): String {
  val lowerMime = mimeType.lowercase()
  val lowerExt = filePath.substringAfterLast('.', "").lowercase()
  return when {
    lowerMime == "application/pdf" || lowerExt == "pdf" -> "PDF"
    lowerMime == "message/rfc822" || lowerExt == "eml" || lowerExt == "msg" -> "EMAIL"
    lowerMime.startsWith("image/") || lowerExt in setOf("jpg", "jpeg", "png", "webp") -> "IMAGE"
    lowerMime == "text/html" || lowerExt in setOf("html", "htm") -> "HTML"
    else -> "TEXT"
  }
}
