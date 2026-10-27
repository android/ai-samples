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

package com.example.jetpacker.feature.assistant

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetpacker.data.itinerary.TravelDocument
import com.example.jetpacker.data.itinerary.TravelDocumentDao
import com.example.jetpacker.data.itinerary.TravelDocumentManager
import com.example.jetpacker.feature.assistant.documents.DocumentAppSearchManager
import com.example.jetpacker.feature.assistant.documents.DocumentSemanticSearch
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.embedding.EmbeddingMetadata
import com.google.mlkit.genai.embedding.EmbeddingModel
import com.google.mlkit.genai.embedding.EmbeddingRequest
import com.google.mlkit.genai.embedding.EmbeddingResult
import com.google.mlkit.genai.embedding.EmbeddingVector
import com.google.mlkit.genai.embedding.ImagePart
import com.google.mlkit.genai.embedding.Modality
import com.google.mlkit.genai.embedding.TextPart
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Covers the vault indexing contract behind Demo 1.B: documents must become retrievable as soon as
 * they are added (not only on the very first query), and must disappear from the index the moment
 * they are deleted. Also verifies multimodal `ImagePart`, PDF, and email (`.eml`) support.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DocumentSemanticSearchTest {

  private lateinit var context: Context
  private lateinit var documentDao: FakeSemanticSearchDocumentDao
  private lateinit var documentManager: TravelDocumentManager
  private var _appSearchManager: DocumentAppSearchManager? = null
  private val appSearchManager: DocumentAppSearchManager get() = checkNotNull(_appSearchManager)
  private lateinit var semanticSearch: DocumentSemanticSearch

  private fun assumeAppSearchAvailable() {
    Assume.assumeTrue(
      "AppSearch LocalStorage requires native libicing runtime; skipping on host JVM",
      _appSearchManager != null,
    )
  }

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    documentDao = FakeSemanticSearchDocumentDao()
    documentManager = TravelDocumentManager(context, documentDao)
    val manager =
      try {
        System.loadLibrary("icing")
        DocumentAppSearchManager(context)
      } catch (_: Throwable) {
        null
      }
    _appSearchManager = manager
    semanticSearch =
      DocumentSemanticSearch(
        travelDocumentDao = documentDao,
        travelDocumentManager = documentManager,
        appSearchManager = manager,
      )
  }

  @After fun tearDown() = runTest { _appSearchManager?.clearAll() }

  @Test
  fun ensureIndexUpToDate_indexesEveryStoredDocument() = runTest {
    assumeAppSearchAvailable()
    documentDao.documents += passport()
    documentDao.documents += hotelWifiNote()

    semanticSearch.ensureIndexUpToDate()

    assertThat(appSearchManager.getIndexedDocumentIds())
      .containsAtLeast("doc-passport", "doc-hotel-wifi")
  }

  @Test
  fun ensureIndexUpToDate_picksUpDocumentAddedAfterFirstQuery() = runTest {
    assumeAppSearchAvailable()
    documentDao.documents += passport()
    semanticSearch.findRelevantDocuments(query = "passport")

    // Simulates scanning a document live, after the index has already been warmed once.
    documentDao.documents += hotelWifiNote()
    semanticSearch.ensureIndexUpToDate()

    assertThat(appSearchManager.getIndexedDocumentIds()).contains("doc-hotel-wifi")
  }

  @Test
  fun indexDocument_makesSingleDocumentRetrievableImmediately() = runTest {
    assumeAppSearchAvailable()
    val scanned = hotelWifiNote()
    documentDao.documents += scanned

    semanticSearch.indexDocument(scanned)

    assertThat(appSearchManager.getIndexedDocumentIds()).contains("doc-hotel-wifi")
    val matches = semanticSearch.findRelevantDocuments(query = "wifi password")
    assertThat(matches.map { it.id }).contains("doc-hotel-wifi")
  }

  @Test
  fun removeFromIndex_dropsDeletedDocument() = runTest {
    assumeAppSearchAvailable()
    documentDao.documents += passport()
    documentDao.documents += hotelWifiNote()
    semanticSearch.ensureIndexUpToDate()

    documentDao.documents.removeAll { it.id == "doc-hotel-wifi" }
    semanticSearch.removeFromIndex("doc-hotel-wifi")

    val indexed = appSearchManager.getIndexedDocumentIds()
    assertThat(indexed).contains("doc-passport")
    assertThat(indexed).doesNotContain("doc-hotel-wifi")
  }

  @Test
  fun ensureIndexUpToDate_prunesDocumentsDeletedOutsideTheViewModel() = runTest {
    assumeAppSearchAvailable()
    documentDao.documents += passport()
    documentDao.documents += hotelWifiNote()
    semanticSearch.ensureIndexUpToDate()

    documentDao.documents.removeAll { it.id == "doc-passport" }
    semanticSearch.ensureIndexUpToDate()

    assertThat(appSearchManager.getIndexedDocumentIds()).doesNotContain("doc-passport")
  }

  @Test
  fun findRelevantDocuments_excludesOtherTrips() = runTest {
    documentDao.documents += hotelWifiNote(tripId = "trip-paris")
    documentDao.documents +=
      TravelDocument(
        id = "doc-tokyo-wifi",
        documentId = "doc-tokyo-wifi",
        title = "Tokyo Capsule Hotel Wifi",
        documentType = "note",
        notes = "Network TOKYO-GUEST, password sakura2026",
        tripId = "trip-tokyo",
      )
    documentDao.documents += passport()

    val matches =
      semanticSearch.findRelevantDocuments(query = "wifi password", tripId = "trip-paris")

    assertThat(matches.map { it.id }).doesNotContain("doc-tokyo-wifi")
  }

  @Test
  fun findRelevantDocuments_returnsEmptyForBlankQuery() = runTest {
    documentDao.documents += passport()

    assertThat(semanticSearch.findRelevantDocuments(query = "   ")).isEmpty()
  }

  @Test
  fun generateDocumentEmbedding_includesImagePartWhenModelSupportsImageModality() = runTest {
    val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    val savedImageDoc =
      documentManager.saveDocumentBitmap(
        bitmap = bitmap,
        title = "Boarding Pass Photo",
        documentType = "TICKET",
        notes = "Gate K42 Seat 14A",
      )
    val multimodalModel =
      FakeEmbeddingModel(supportedModalities = listOf(Modality.TEXT, Modality.IMAGE))

    val vector = semanticSearch.generateDocumentEmbedding(savedImageDoc, multimodalModel)

    assertThat(vector).isNotNull()
    val parts = multimodalModel.lastRequest?.content?.parts.orEmpty()
    assertThat(parts.any { it is TextPart }).isTrue()
    assertThat(parts.any { it is ImagePart }).isTrue()
  }

  @Test
  fun generateDocumentEmbedding_omitsImagePartWhenModelIsTextOnly() = runTest {
    val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    val savedImageDoc =
      documentManager.saveDocumentBitmap(
        bitmap = bitmap,
        title = "Boarding Pass Photo",
        documentType = "TICKET",
        notes = "Gate K42 Seat 14A",
      )
    val textOnlyModel = FakeEmbeddingModel(supportedModalities = listOf(Modality.TEXT))

    val vector = semanticSearch.generateDocumentEmbedding(savedImageDoc, textOnlyModel)

    assertThat(vector).isNotNull()
    val parts = textOnlyModel.lastRequest?.content?.parts.orEmpty()
    assertThat(parts).hasSize(1)
    assertThat(parts.single()).isInstanceOf(TextPart::class.java)
  }

  @Test
  fun importEmailDocument_parsesHeadersAndBodyAndIsSearchable() = runTest {
    val emlFile = File(context.cacheDir, "eurostar_booking.eml")
    emlFile.writeText(
      """
      From: bookings@eurostar.com
      To: alex.chen@example.com
      Subject: Eurostar Booking Confirmation #ES9921
      Date: Mon, 5 Oct 2026 09:00:00 +0000
      Content-Type: text/plain; charset="UTF-8"

      Your train from Paris Gare du Nord to London St Pancras is confirmed.
      Coach 08, Seat 61. PNR: ZX88Q1.
      """
        .trimIndent()
    )

    val imported = documentManager.importDocumentFromUri(Uri.fromFile(emlFile))
    semanticSearch.indexDocument(imported)

    assertThat(imported.title).isEqualTo("Eurostar Booking Confirmation #ES9921")
    assertThat(imported.documentType).isEqualTo("TICKET")
    val matches = semanticSearch.findRelevantDocuments(query = "St Pancras Coach 08")
    assertThat(matches.map { it.id }).contains(imported.id)
  }

  @Test
  fun importPdfDocument_extractsTextStreamAndIsSearchable() = runTest {
    val pdfFile = File(context.cacheDir, "flight_receipt.pdf")
    pdfFile.writeText(
      """
      %PDF-1.4
      1 0 obj << /Type /Page >> endobj
      2 0 obj << /Length 88 >>
      stream
      BT /F1 12 Tf 72 712 Td (Lufthansa Flight LH454 Boarding Gate Z50 Confirmation MUC2026) Tj ET
      endstream
      endobj
      %%EOF
      """
        .trimIndent()
    )

    val imported = documentManager.importDocumentFromUri(Uri.fromFile(pdfFile))
    semanticSearch.indexDocument(imported)

    assertThat(imported.documentType).isEqualTo("TICKET")
    assertThat(imported.notes).contains("Lufthansa Flight LH454")
    val matches = semanticSearch.findRelevantDocuments(query = "Lufthansa Gate Z50")
    assertThat(matches.map { it.id }).contains(imported.id)
  }

  @Test
  fun seedDefaultDocumentsIfEmpty_populatesAndIndexesDiverseFormatsAcrossModalities() = runTest {
    documentManager.seedDefaultDocumentsIfEmpty()
    semanticSearch.ensureIndexUpToDate()

    val seeded = documentDao.documents.toList()
    assertThat(seeded).hasSize(8)
    assertThat(seeded.map { it.mimeType }.toSet())
      .containsExactly("text/plain", "application/pdf", "message/rfc822", "image/jpeg")

    // Verify PDF text extraction works on the seeded PDF files
    val pdfDoc = seeded.first { it.title.contains("Zephyr Wheels") }
    val extractedPdfText = documentManager.extractPdfText(File(pdfDoc.filePath))
    assertThat(extractedPdfText).contains("Polestar 2 Long Range Dual Motor")

    // Verify searching across Email, PDF, Image, and Text seeded docs
    val emailMatches = semanticSearch.findRelevantDocuments(query = "SEQUOIA2026")
    assertThat(emailMatches.map { it.title }).contains("The Redwood Grove Hotel — Booking Email")

    val pdfMatches = semanticSearch.findRelevantDocuments(query = "FasTrak")
    assertThat(pdfMatches.map { it.title }).contains("Zephyr Wheels Confirmation")

    val imageMatches = semanticSearch.findRelevantDocuments(query = "SFMOMA")
    assertThat(imageMatches.map { it.title }).contains("SFMOMA VIP Museum & Exhibition Pass")
  }

  @Test
  fun syncIndex_whenVectorIsNull_doesNotCacheHashUntilEmbeddingModelAvailable() = runTest {
    assumeAppSearchAvailable()
    val doc = passport()
    documentDao.documents += doc

    // 1. Model is null (e.g. not loaded or unavailable)
    semanticSearch.setEmbeddingModelForTesting(null)
    semanticSearch.ensureIndexUpToDate()

    // Comment #4: Hash must not be cached when vector is null
    assertThat(semanticSearch.isDocumentHashIndexed(doc.id)).isFalse()

    // 2. Model becomes available later
    val model = FakeEmbeddingModel(supportedModalities = listOf(Modality.TEXT))
    semanticSearch.setEmbeddingModelForTesting(model)
    semanticSearch.ensureIndexUpToDate()

    // Now vector was generated, so hash is cached and embedding request occurred
    assertThat(semanticSearch.isDocumentHashIndexed(doc.id)).isTrue()
    assertThat(model.lastRequest).isNotNull()
  }

  private fun passport() =
    TravelDocument(
      id = "doc-passport",
      documentId = "doc-passport",
      title = "Dutch Passport",
      documentType = "passport",
      issuingCountry = "Netherlands",
      notes = "Issued 2021, valid ten years",
    )

  private fun hotelWifiNote(tripId: String? = null) =
    TravelDocument(
      id = "doc-hotel-wifi",
      documentId = "doc-hotel-wifi",
      title = "Hotel Lumiere Wifi",
      documentType = "note",
      notes = "Network LUMIERE-GUEST, password bonjour2026",
      tripId = tripId,
    )
}

private class FakeEmbeddingModel(private val supportedModalities: List<Int>) : EmbeddingModel {
  var lastRequest: EmbeddingRequest? = null

  override suspend fun checkStatus(): Int = FeatureStatus.AVAILABLE

  override fun download(): Flow<DownloadStatus> = flowOf(DownloadStatus.DownloadCompleted)

  override suspend fun warmup() {}

  override suspend fun getTokenLimit(): Int = 256

  override suspend fun generateEmbedding(request: EmbeddingRequest): EmbeddingResult {
    lastRequest = request
    return EmbeddingResult(
      embedding = EmbeddingVector(floatArrayOf(0.5f, 0.5f, 0.5f)),
      modelSignature = "fake-embedding-gemma-v2",
    )
  }

  override suspend fun getModelSignature(): String = "fake-embedding-gemma-v2"

  override suspend fun getMetadata(): EmbeddingMetadata =
    EmbeddingMetadata(dimension = 3, supportedModalities = supportedModalities)

  override suspend fun isDeprecated(): Boolean = false

  override fun close() {}
}

/** Minimal in-memory [TravelDocumentDao] so the tests never touch Room. */
private class FakeSemanticSearchDocumentDao : TravelDocumentDao {
  val documents = mutableListOf<TravelDocument>()

  override fun getAllDocuments(): Flow<List<TravelDocument>> = flowOf(documents.toList())

  override fun getDocumentsByType(documentType: String): Flow<List<TravelDocument>> =
    flowOf(documents.filter { it.documentType.equals(documentType, ignoreCase = true) })

  override fun getDocumentsForTrip(tripId: String): Flow<List<TravelDocument>> =
    flowOf(documents.filter { it.tripId == tripId })

  override fun getDocumentsForEvent(eventId: String): Flow<List<TravelDocument>> =
    flowOf(documents.filter { it.eventId == eventId })

  override suspend fun getDocumentById(documentId: String): TravelDocument? = documents.find {
    it.documentId == documentId || it.id == documentId
  }

  override suspend fun insertDocument(document: TravelDocument) {
    documents.add(document)
  }

  override suspend fun linkDocumentToEvent(documentId: String, eventId: String?, tripId: String?) {
    val index = documents.indexOfFirst { it.documentId == documentId || it.id == documentId }
    if (index != -1) {
      val existing = documents[index]
      documents[index] = existing.copy(eventId = eventId, tripId = tripId ?: existing.tripId)
    }
  }

  override suspend fun linkDocumentToTrip(documentId: String, tripId: String?) {
    val index = documents.indexOfFirst { it.documentId == documentId || it.id == documentId }
    if (index != -1) {
      documents[index] = documents[index].copy(tripId = tripId)
    }
  }

  override suspend fun deleteDocument(document: TravelDocument) {
    documents.remove(document)
  }

  override suspend fun deleteDocumentById(id: String) {
    documents.removeAll { it.id == id }
  }

  override suspend fun deleteAllDocuments() {
    documents.clear()
  }
}
