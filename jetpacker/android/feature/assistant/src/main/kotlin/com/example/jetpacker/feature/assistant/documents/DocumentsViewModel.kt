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

import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TravelDocument
import com.example.jetpacker.data.itinerary.TravelDocumentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing user travel documents, file imports, and document content previews.
 *
 * Every mutation is written through to the on-device AppSearch vault index so that a document
 * scanned or created moments ago is immediately retrievable by natural language, both from the
 * vault search bar and from the Nano assistant.
 */
@HiltViewModel
class DocumentsViewModel
@Inject
constructor(
  private val travelDocumentManager: TravelDocumentManager,
  private val eventDao: EventDao,
  private val documentVisionScanner: DocumentVisionScanner,
  private val documentSemanticSearch: DocumentSemanticSearch,
) : ViewModel() {

  fun getEventsForTrip(tripId: String): Flow<List<TimelineEvent>> {
    return eventDao.getEventsForTrip(tripId)
  }

  val documents: StateFlow<List<TravelDocument>> =
    travelDocumentManager.documents.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  private val _selectedDocument = MutableStateFlow<TravelDocument?>(null)
  val selectedDocument: StateFlow<TravelDocument?> = _selectedDocument.asStateFlow()

  private val _previewContent = MutableStateFlow<String?>(null)
  val previewContent: StateFlow<String?> = _previewContent.asStateFlow()

  private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
  val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  private val _isProcessing = MutableStateFlow(false)
  val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

  /** Text currently typed into the vault search bar. */
  private val _vaultQuery = MutableStateFlow("")
  val vaultQuery: StateFlow<String> = _vaultQuery.asStateFlow()

  /** Ranked vault matches, or null when no search is active and the full list should show. */
  private val _vaultResults = MutableStateFlow<List<TravelDocument>?>(null)
  val vaultResults: StateFlow<List<TravelDocument>?> = _vaultResults.asStateFlow()

  /** Human readable provenance line, e.g. "3 matches · 84 ms · on-device". */
  private val _vaultSearchStatus = MutableStateFlow<String?>(null)
  val vaultSearchStatus: StateFlow<String?> = _vaultSearchStatus.asStateFlow()

  private val _isSearchingVault = MutableStateFlow(false)
  val isSearchingVault: StateFlow<Boolean> = _isSearchingVault.asStateFlow()

  private var vaultSearchJob: Job? = null

  init {
    travelDocumentManager.semanticSearchProvider = documentSemanticSearch
    viewModelScope.launch {
      travelDocumentManager.seedDefaultDocumentsIfEmpty()
      syncVaultIndex()
    }
  }

  /**
   * Runs a natural language search across the on-device vault.
   *
   * @param tripId Restricts results to the active trip plus documents not tied to any trip.
   */
  fun searchVault(tripId: String? = null) {
    val query = _vaultQuery.value.trim()
    vaultSearchJob?.cancel()
    if (query.isBlank()) {
      clearVaultSearch()
      return
    }

    vaultSearchJob = viewModelScope.launch {
      performVaultSearch(query, tripId)
    }
  }

  fun onVaultQueryChange(query: String, tripId: String? = null) {
    _vaultQuery.value = query
    if (query.isBlank()) {
      clearVaultSearch()
      return
    }
    vaultSearchJob?.cancel()
    vaultSearchJob = viewModelScope.launch {
      delay(350)
      performVaultSearch(_vaultQuery.value.trim(), tripId)
    }
  }

  private suspend fun performVaultSearch(query: String, tripId: String?) {
    if (query.isBlank()) {
      clearVaultSearch()
      return
    }
    _isSearchingVault.value = true
    val startMs = SystemClock.elapsedRealtime()
    try {
      val matches =
        documentSemanticSearch.findRelevantDocuments(
          query = query,
          tripId = tripId,
          maxResults = VAULT_SEARCH_MAX_RESULTS,
        )
      val elapsedMs = SystemClock.elapsedRealtime() - startMs
      _vaultResults.value = matches
      _vaultSearchStatus.value =
        if (matches.isEmpty()) {
          "No matching documents (100% on-device, ${elapsedMs}ms)"
        } else {
          "${matches.size} match${if (matches.size == 1) "" else "es"} (100% on-device, ${elapsedMs}ms)"
        }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.w(TAG, "Vault search failed for query: $query", e)
      _vaultResults.value = null
      _vaultSearchStatus.value = "Vault search unavailable"
    } finally {
      _isSearchingVault.value = false
    }
  }

  /** Dismisses the active search and restores the full document list. */
  fun clearVaultSearch() {
    vaultSearchJob?.cancel()
    vaultSearchJob = null
    _vaultQuery.value = ""
    _vaultResults.value = null
    _vaultSearchStatus.value = null
    _isSearchingVault.value = false
  }

  /**
   * Brings the AppSearch vault index in line with Room, adding new docs and pruning deleted ones.
   */
  private suspend fun syncVaultIndex() {
    try {
      documentSemanticSearch.ensureIndexUpToDate()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to sync vault index", e)
    }
  }

  /** Indexes a single document, never letting an indexing failure break the user-visible flow. */
  private suspend fun indexInVault(doc: TravelDocument) {
    try {
      documentSemanticSearch.indexDocument(doc)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to index document ${doc.id} in vault", e)
    }
  }

  /**
   * Imports an external document (image, PDF, email, or text) from a file picker or share [Uri].
   */
  fun importDocument(
    uri: Uri,
    overrideTitle: String? = null,
    documentType: String = "OTHER",
    tripId: String? = null,
    eventId: String? = null,
  ) {
    viewModelScope.launch {
      _isProcessing.value = true
      try {
        var imported =
          travelDocumentManager.importDocumentFromUri(
            uri = uri,
            overrideTitle = overrideTitle,
            documentType = documentType,
            tripId = tripId,
            eventId = eventId,
          )
        if (
          imported.notes.isBlank() &&
            travelDocumentManager.isVisualDocument(imported) &&
            runCatching { documentVisionScanner.isSupported() }.getOrDefault(false)
        ) {
          val bitmap = travelDocumentManager.loadDocumentBitmap(imported, maxDimension = 1920)
          if (bitmap != null) {
            val extracted =
              runCatching { documentVisionScanner.extractDocument(bitmap) }.getOrNull()
            if (extracted != null && extracted.summary.isNotBlank()) {
              imported =
                imported.copy(
                  title =
                    if (
                      overrideTitle.isNullOrBlank() &&
                        extracted.title.isNotBlank() &&
                        extracted.title != "Scanned Document"
                    ) {
                      extracted.title
                    } else {
                      imported.title
                    },
                  documentType =
                    if (imported.documentType == "OTHER" && extracted.documentType != "OTHER") {
                      extracted.documentType
                    } else {
                      imported.documentType
                    },
                  issuingCountry = imported.issuingCountry.ifBlank { extracted.issuingCountry },
                  expiryDate =
                    if (imported.expiryDate > 0L) imported.expiryDate
                    else extracted.expiryDateEpochMs,
                  documentNumberMasked =
                    imported.documentNumberMasked.ifBlank { extracted.documentNumber },
                  notes = extracted.summary,
                )
              travelDocumentManager.updateDocument(imported)
            }
          }
        }
        indexInVault(imported)
        _userMessage.value = "Imported \"${imported.title}\""
      } catch (e: Exception) {
        Log.e(TAG, "Failed to import document from $uri", e)
        _userMessage.value = "Failed to import document: ${e.message}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  suspend fun isScannerSupported(): Boolean = documentVisionScanner.isSupported()

  /**
   * Scans a travel document photo with on-device Gemini Nano vision, extracts metadata, and saves
   * it.
   */
  fun scanDocumentFromBitmap(bitmap: Bitmap, tripId: String? = null, eventId: String? = null) {
    viewModelScope.launch {
      _isProcessing.value = true
      try {
        val extracted = documentVisionScanner.extractDocument(bitmap)
        val saved =
          travelDocumentManager.saveDocumentBitmap(
            bitmap = bitmap,
            title = extracted.title,
            documentType = extracted.documentType,
            issuingCountry = extracted.issuingCountry,
            expiryDate = extracted.expiryDateEpochMs,
            documentNumberMasked = extracted.documentNumber,
            notes = extracted.summary,
            tripId = tripId,
            eventId = eventId,
          )
        indexInVault(saved)
        _userMessage.value = "Scanned and saved: ${saved.title}"
      } catch (e: Exception) {
        Log.e(TAG, "Failed to scan document", e)
        _userMessage.value = "Failed to scan document: ${e.message}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  /**
   * Scans a high-resolution captured photo from an image [Uri] with on-device Gemini Nano vision.
   */
  fun scanDocumentFromUri(uri: Uri, tripId: String? = null, eventId: String? = null) {
    viewModelScope.launch {
      _isProcessing.value = true
      try {
        val bitmap = travelDocumentManager.loadBitmapFromUri(uri)
        if (bitmap == null) {
          _userMessage.value = "Failed to load captured image."
          return@launch
        }
        val extracted = documentVisionScanner.extractDocument(bitmap)
        val saved =
          travelDocumentManager.saveDocumentBitmap(
            bitmap = bitmap,
            title = extracted.title,
            documentType = extracted.documentType,
            issuingCountry = extracted.issuingCountry,
            expiryDate = extracted.expiryDateEpochMs,
            documentNumberMasked = extracted.documentNumber,
            notes = extracted.summary,
            tripId = tripId,
            eventId = eventId,
          )
        indexInVault(saved)
        _userMessage.value = "Scanned and saved: ${saved.title}"
      } catch (e: Exception) {
        Log.e(TAG, "Failed to scan document from $uri", e)
        _userMessage.value = "Failed to scan document: ${e.message}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  /** Creates an in-app text document (e.g. flight confirmation or travel notes). */
  fun createTextDocument(
    title: String,
    content: String,
    documentType: String = "OTHER",
    issuingCountry: String = "",
    expiryDate: Long = 0L,
    documentNumberMasked: String = "",
    notes: String = "",
    tripId: String? = null,
    eventId: String? = null,
  ) {
    viewModelScope.launch {
      _isProcessing.value = true
      try {
        val created =
          travelDocumentManager.createTextDocument(
            title = title,
            content = content,
            documentType = documentType,
            issuingCountry = issuingCountry,
            expiryDate = expiryDate,
            documentNumberMasked = documentNumberMasked,
            notes = notes,
            tripId = tripId,
            eventId = eventId,
          )
        indexInVault(created)
        _userMessage.value = "Saved \"${created.title}\""
      } catch (e: Exception) {
        Log.e(TAG, "Failed to create text document", e)
        _userMessage.value = "Failed to create document: ${e.message}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  /** Links or unlinks a document to/from a specific timeline event. */
  fun linkDocumentToEvent(documentId: String, eventId: String?, tripId: String? = null) {
    viewModelScope.launch {
      try {
        travelDocumentManager.linkDocumentToEvent(documentId, eventId, tripId)
        // Trip/event linkage is part of the indexed payload, so the entry must be refreshed.
        syncVaultIndex()
        _userMessage.value =
          if (eventId != null) "Document linked to event" else "Document unlinked"
      } catch (e: Exception) {
        Log.e(TAG, "Failed to link document $documentId to event $eventId", e)
        _userMessage.value = "Failed to update event link: ${e.message}"
      }
    }
  }

  /** Links or unlinks a document to/from a specific trip. */
  fun linkDocumentToTrip(documentId: String, tripId: String?) {
    viewModelScope.launch {
      try {
        travelDocumentManager.linkDocumentToTrip(documentId, tripId)
        // The trip ID drives search scoping, so the index must follow the new linkage.
        syncVaultIndex()
        _userMessage.value =
          if (tripId != null) "Document added to trip" else "Document removed from trip"
      } catch (e: Exception) {
        Log.e(TAG, "Failed to link document $documentId to trip $tripId", e)
        _userMessage.value = "Failed to update trip link: ${e.message}"
      }
    }
  }

  /** Deletes a travel document and its private storage file. */
  fun deleteDocument(document: TravelDocument) {
    viewModelScope.launch {
      try {
        travelDocumentManager.deleteDocument(document)
        // Prune before surfacing success so a deleted document can never resurface in search.
        documentSemanticSearch.removeFromIndex(document.id)
        _vaultResults.value = _vaultResults.value?.filterNot { it.id == document.id }
        _userMessage.value = "Deleted \"${document.title}\""
        if (_selectedDocument.value?.documentId == document.documentId) {
          _selectedDocument.value = null
          _previewContent.value = null
          _previewBitmap.value = null
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete document ${document.documentId}", e)
        _userMessage.value = "Failed to delete document: ${e.message}"
      }
    }
  }

  /** Loads a thumbnail or rendered page 0 bitmap for visual documents (images and PDFs). */
  suspend fun loadCardBitmap(document: TravelDocument): Bitmap? {
    if (!travelDocumentManager.isVisualDocument(document)) return null
    return withContext(Dispatchers.IO) {
      try {
        travelDocumentManager.loadDocumentBitmap(document, maxDimension = 768)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        null
      }
    }
  }

  /** Selects a document to inspect its visual bitmap (image/PDF) and text content in a dialog. */
  fun selectDocumentForPreview(document: TravelDocument?) {
    _selectedDocument.value = document
    if (document == null) {
      _previewContent.value = null
      _previewBitmap.value = null
      return
    }

    viewModelScope.launch {
      _previewBitmap.value =
        if (travelDocumentManager.isVisualDocument(document)) {
          withContext(Dispatchers.IO) {
            try {
              travelDocumentManager.loadDocumentBitmap(document, maxDimension = 1280)
            } catch (e: CancellationException) {
              throw e
            } catch (e: Exception) {
              null
            }
          }
        } else {
          null
        }
      try {
        val content = withContext(Dispatchers.IO) {
          travelDocumentManager.readDocumentContent(document)
        }
        _previewContent.value = content
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Could not read content for ${document.documentId}", e)
        _previewContent.value = "Unable to read document contents."
      }
    }
  }

  /** Resets and re-seeds the diverse multimodal demo documents (PDF, Email, Image, Text). */
  fun seedDefaultDocuments() {
    viewModelScope.launch {
      _isProcessing.value = true
      try {
        clearVaultSearch()
        travelDocumentManager.resetDefaultDocuments()
        syncVaultIndex()
        _userMessage.value = "Loaded 8 diverse demo documents (PDF, Email, Image, Text)"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  fun clearUserMessage() {
    _userMessage.value = null
  }

  companion object {
    private const val TAG = "DocumentsViewModel"

    /** The vault is a small personal corpus, so a short ranked list is the useful answer. */
    private const val VAULT_SEARCH_MAX_RESULTS = 5
  }
}
