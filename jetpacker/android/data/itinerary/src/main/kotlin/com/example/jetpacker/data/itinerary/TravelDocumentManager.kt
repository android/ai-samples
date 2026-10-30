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

@file:Suppress("GlobalCoroutineDispatchers")

package com.example.jetpacker.data.itinerary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.zip.Inflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Manages travel documents, local app-private file storage, and default seed documents. */
open class TravelDocumentManager(
  private val context: Context,
  private val travelDocumentDao: TravelDocumentDao,
) {

  private val documentsDir: File by lazy {
    File(context.filesDir, "travel_documents").apply { mkdirs() }
  }

  val documents: Flow<List<TravelDocument>> = travelDocumentDao.getAllDocuments()

  /**
   * Optional semantic search provider using on-device embedding models (Embedding Gemma v2)
   * and AppSearch vector indexing.
   */
  @Volatile
  var semanticSearchProvider: DocumentSemanticSearchProvider? = null

  /**
   * Executes a semantic search across travel documents using the registered [semanticSearchProvider].
   */
  open suspend fun searchDocumentsSemantically(
    query: String,
    tripId: String? = null,
    maxResults: Int = 10,
  ): List<TravelDocument> {
    val provider = semanticSearchProvider ?: return emptyList()
    return try {
      provider.findRelevantDocuments(query, tripId, maxResults)
    } catch (e: Exception) {
      Log.w(TAG, "Semantic document search failed for query: $query", e)
      emptyList()
    }
  }

  /**
   * Imports a document from an external or shared [Uri] into app-private storage and saves it to
   * Room.
   */
  open suspend fun importDocumentFromUri(
    uri: Uri,
    overrideTitle: String? = null,
    documentType: String = "OTHER",
    issuingCountry: String = "",
    expiryDate: Long = 0L,
    documentNumberMasked: String = "",
    notes: String = "",
    tripId: String? = null,
    eventId: String? = null,
  ): TravelDocument =
    withContext(Dispatchers.IO) {
      val rawFilename = queryFilename(uri) ?: "document_${System.currentTimeMillis()}"
      val originalFilename = File(rawFilename).name.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
      val mimeType =
        context.contentResolver.getType(uri) ?: getMimeTypeFromFilename(originalFilename)
      val safePrefix = UUID.randomUUID().toString().take(8)
      val destFile = File(documentsDir, "${safePrefix}_$originalFilename")
      check(destFile.canonicalFile.startsWith(documentsDir.canonicalFile)) {
        "Invalid destination file path"
      }

      var copySuccess = false
      try {
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output ->
            val bytes = input.copyTo(output)
            copySuccess = bytes > 0L || destFile.exists()
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to copy URI stream to private file", e)
      }

      if (!copySuccess || !destFile.exists()) {
        destFile.delete()
        error("Failed to copy document from URI: $uri")
      }

      val extractedText = extractTextFromFile(destFile, mimeType)
      val emailSubject =
        if (isEmailMimeOrExt(mimeType, destFile.extension)) {
          runCatching { parseEmailContent(destFile.readText()).subject }.getOrNull()
        } else {
          null
        }

      val finalTitle =
        overrideTitle?.takeIf { it.isNotBlank() }
          ?: emailSubject?.takeIf { it.isNotBlank() }
          ?: originalFilename
      val resolvedType =
        if (documentType == "OTHER") {
          detectDocumentType(originalFilename, destFile, extractedText)
        } else {
          documentType
        }
      val resolvedNotes = notes.ifBlank {
        extractedText?.take(MAX_AUTO_NOTES_CHARS)?.trim().orEmpty()
      }

      val doc =
        TravelDocument(
          documentId = "doc_${UUID.randomUUID().toString().take(8)}",
          documentType = resolvedType,
          issuingCountry = issuingCountry,
          expiryDate = expiryDate,
          documentNumberMasked = documentNumberMasked,
          notes = resolvedNotes,
          title = finalTitle,
          filePath = destFile.absolutePath,
          mimeType = mimeType,
          tripId = tripId,
          eventId = eventId,
        )

      travelDocumentDao.insertDocument(doc)
      doc
    }

  /**
   * Creates an in-app text document (e.g. itinerary notes, passport text, booking voucher, or
   * shared email) in private storage.
   */
  open suspend fun createTextDocument(
    title: String,
    content: String,
    documentType: String = "OTHER",
    issuingCountry: String = "",
    expiryDate: Long = 0L,
    documentNumberMasked: String = "",
    notes: String = "",
    tripId: String? = null,
    eventId: String? = null,
  ): TravelDocument =
    withContext(Dispatchers.IO) {
      val parsedEmail =
        if (looksLikeEmailOrHtml(content)) {
          parseEmailContent(content)
        } else {
          null
        }
      val cleanedContent = parsedEmail?.formattedSummary()?.ifBlank { content } ?: content
      val resolvedTitle =
        if (
          (title.isBlank() || title.equals("Shared Document", ignoreCase = true)) &&
            !parsedEmail?.subject.isNullOrBlank()
        ) {
          parsedEmail?.subject ?: title.ifBlank { "Untitled Document" }
        } else {
          title.ifBlank { "Untitled Document" }
        }

      val safePrefix = UUID.randomUUID().toString().take(8)
      val safeName = resolvedTitle.replace(Regex("""[^a-zA-Z0-9_-]"""), "_").take(32)
      val file = File(documentsDir, "${safePrefix}_${safeName}.txt")

      try {
        file.writeText(cleanedContent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to write text document to file", e)
      }

      val resolvedType =
        if (documentType == "OTHER") {
          detectDocumentType(resolvedTitle, file, cleanedContent)
        } else {
          documentType
        }
      val resolvedNotes = notes.ifBlank { cleanedContent.take(MAX_AUTO_NOTES_CHARS).trim() }

      val doc =
        TravelDocument(
          documentId = "doc_${UUID.randomUUID().toString().take(8)}",
          documentType = resolvedType,
          issuingCountry = issuingCountry,
          expiryDate = expiryDate,
          documentNumberMasked = documentNumberMasked,
          notes = resolvedNotes,
          title = resolvedTitle,
          filePath = file.absolutePath,
          mimeType = "text/plain",
          tripId = tripId,
          eventId = eventId,
        )

      travelDocumentDao.insertDocument(doc)
      doc
    }

  /**
   * Saves a document bitmap (e.g. from camera capture) into private app storage and inserts Room
   * metadata.
   */
  open suspend fun saveDocumentBitmap(
    bitmap: Bitmap,
    title: String,
    documentType: String = "OTHER",
    issuingCountry: String = "",
    expiryDate: Long = 0L,
    documentNumberMasked: String = "",
    notes: String = "",
    tripId: String? = null,
    eventId: String? = null,
  ): TravelDocument =
    withContext(Dispatchers.IO) {
      val safePrefix = UUID.randomUUID().toString().take(8)
      val safeName = title.replace(Regex("""[^a-zA-Z0-9_-]"""), "_").take(32).ifBlank { "scan" }
      val file = File(documentsDir, "${safePrefix}_${safeName}.jpg")

      try {
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to write bitmap document to file", e)
      }

      val doc =
        TravelDocument(
          documentId = "doc_${UUID.randomUUID().toString().take(8)}",
          documentType = documentType,
          issuingCountry = issuingCountry,
          expiryDate = expiryDate,
          documentNumberMasked = documentNumberMasked,
          notes = notes,
          title = title,
          filePath = file.absolutePath,
          mimeType = "image/jpeg",
          tripId = tripId,
          eventId = eventId,
        )

      travelDocumentDao.insertDocument(doc)
      doc
    }

  /**
   * Decodes a high-resolution bitmap from an image [Uri], scaling down large images to
   * [maxDimension] to prevent OOM errors while preserving fine print for vision models, and
   * correcting EXIF rotation.
   */
  open suspend fun loadBitmapFromUri(uri: Uri, maxDimension: Int = 1920): Bitmap? =
    withContext(Dispatchers.IO) {
      try {
        Log.d(TAG, "Loading bitmap from uri: $uri")
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val streamOpened =
          context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
            true
          } ?: false

        if (!streamOpened || boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
          Log.e(
            TAG,
            "Failed to read image bounds for $uri: streamOpened=$streamOpened, w=${boundsOptions.outWidth}, h=${boundsOptions.outHeight}",
          )
          return@withContext null
        }

        Log.d(
          TAG,
          "Original image dimensions: ${boundsOptions.outWidth}x${boundsOptions.outHeight}",
        )

        var sampleSize = 1
        val maxSide = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
        if (maxSide > maxDimension) {
          while ((maxSide / (sampleSize * 2)) >= maxDimension) {
            sampleSize *= 2
          }
        }

        val decodeOptions =
          BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
          }
        val decoded =
          context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
          }

        if (decoded == null) {
          Log.e(TAG, "BitmapFactory failed to decode stream for $uri with sampleSize=$sampleSize")
          return@withContext null
        }

        Log.d(
          TAG,
          "Decoded bitmap size: ${decoded.width}x${decoded.height}, sampleSize=$sampleSize",
        )

        val orientation =
          try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
              val exif = ExifInterface(stream)
              exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
          } catch (e: Exception) {
            Log.w(TAG, "Could not read EXIF orientation from $uri", e)
            ExifInterface.ORIENTATION_NORMAL
          }

        val rotationDegrees =
          when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
          }

        if (rotationDegrees != 0f) {
          Log.d(TAG, "Rotating bitmap by $rotationDegrees degrees based on EXIF")
          val matrix = Matrix().apply { postRotate(rotationDegrees) }
          val rotated =
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
          if (rotated != decoded) {
            decoded.recycle()
          }
          rotated
        } else {
          decoded
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to load bitmap from uri: $uri", e)
        null
      }
    }

  open fun getDocumentsForTrip(tripId: String): Flow<List<TravelDocument>> =
    travelDocumentDao.getDocumentsForTrip(tripId)

  open fun getDocumentsForEvent(eventId: String): Flow<List<TravelDocument>> =
    travelDocumentDao.getDocumentsForEvent(eventId)

  open suspend fun linkDocumentToEvent(
    documentId: String,
    eventId: String?,
    tripId: String? = null,
  ) =
    withContext(Dispatchers.IO) {
      travelDocumentDao.linkDocumentToEvent(documentId, eventId, tripId)
    }

  open suspend fun linkDocumentToTrip(documentId: String, tripId: String?) =
    withContext(Dispatchers.IO) { travelDocumentDao.linkDocumentToTrip(documentId, tripId) }

  open suspend fun updateDocument(document: TravelDocument) =
    withContext(Dispatchers.IO) { travelDocumentDao.insertDocument(document) }

  /** Returns true when [document] is backed by a raster image file. */
  open fun isImageDocument(document: TravelDocument): Boolean {
    val ext = document.filePath.substringAfterLast('.', "").lowercase()
    return document.mimeType.startsWith("image/", ignoreCase = true) ||
      ext in setOf("png", "jpg", "jpeg", "webp")
  }

  /** Returns true when [document] is backed by a PDF file. */
  open fun isPdfDocument(document: TravelDocument): Boolean {
    val ext = document.filePath.substringAfterLast('.', "").lowercase()
    return document.mimeType.equals("application/pdf", ignoreCase = true) || ext == "pdf"
  }

  /** Returns true when [document] is an email file (.eml / message/rfc822). */
  open fun isEmailDocument(document: TravelDocument): Boolean {
    val ext = document.filePath.substringAfterLast('.', "").lowercase()
    return isEmailMimeOrExt(document.mimeType, ext)
  }

  /**
   * Returns true when [document] can be rendered into a [Bitmap] for multimodal embedding or Gemini
   * Nano vision extraction (images and PDFs).
   */
  open fun isVisualDocument(document: TravelDocument): Boolean =
    document.filePath.isNotBlank() && (isImageDocument(document) || isPdfDocument(document))

  /**
   * Loads a [Bitmap] representation of [document] for multimodal embedding (`ImagePart`) or Gemini
   * Nano vision scanning.
   * - For image documents, decodes and downsamples the file to [maxDimension].
   * - For PDF documents, renders page 0 via Android's [PdfRenderer] onto a white canvas.
   */
  open fun loadDocumentBitmap(document: TravelDocument, maxDimension: Int = 1024): Bitmap? {
    if (document.filePath.isBlank()) return null
    val file = File(document.filePath)
    if (!file.exists() || !file.isFile) return null
    return when {
      isPdfDocument(document) ->
        renderPdfPageToBitmap(file, pageIndex = 0, maxDimension = maxDimension)
      isImageDocument(document) -> decodeFileBitmap(file, maxDimension = maxDimension)
      else -> null
    }
  }

  /**
   * Renders a single page of a PDF [file] into an [Bitmap.Config.ARGB_8888] [Bitmap] with a white
   * background using Android's built-in [PdfRenderer].
   */
  open fun renderPdfPageToBitmap(
    file: File,
    pageIndex: Int = 0,
    maxDimension: Int = 1024,
  ): Bitmap? {
    if (!file.exists() || !file.isFile) return null
    return try {
      ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
          if (renderer.pageCount <= pageIndex) return null
          renderer.openPage(pageIndex).use { page ->
            val rawW = page.width.coerceAtLeast(1)
            val rawH = page.height.coerceAtLeast(1)
            val maxSide = maxOf(rawW, rawH)
            val scale =
              if (maxSide > maxDimension) {
                maxDimension.toFloat() / maxSide.toFloat()
              } else {
                // Upscale standard 72dpi PDF pages slightly for crisp OCR/embedding, capped at
                // maxDimension.
                minOf(2f, maxDimension.toFloat() / maxSide.toFloat())
              }
            val targetW = (rawW * scale).toInt().coerceAtLeast(1)
            val targetH = (rawH * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to render PDF page to bitmap: ${file.name}", e)
      null
    }
  }

  private fun decodeFileBitmap(file: File, maxDimension: Int): Bitmap? {
    return try {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(file.absolutePath, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return BitmapFactory.decodeFile(file.absolutePath)
      }
      var sampleSize = 1
      val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
      if (maxSide > maxDimension) {
        while ((maxSide / (sampleSize * 2)) >= maxDimension) {
          sampleSize *= 2
        }
      }
      val opts =
        BitmapFactory.Options().apply {
          inSampleSize = sampleSize
          inPreferredConfig = Bitmap.Config.ARGB_8888
        }
      BitmapFactory.decodeFile(file.absolutePath, opts)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to decode image file: ${file.name}", e)
      null
    }
  }

  /**
   * Builds a concise text representation of [document] suitable for on-device embedding models
   * (~256 token window) and AppSearch full-text indexing.
   *
   * Combines metadata (`title`, `documentType`, `issuingCountry`, `documentNumberMasked`, `notes`)
   * with extracted text from backing `.txt`, `.eml`, `.html`, or `.pdf` files when available.
   */
  open fun readDocumentEmbeddingText(document: TravelDocument, maxChars: Int = 1200): String {
    val fileText =
      if (document.filePath.isNotBlank()) {
        val file = File(document.filePath)
        if (file.exists() && file.isFile) {
          extractTextFromFile(file, document.mimeType)
        } else {
          null
        }
      } else {
        null
      }

    val combinedBody =
      when {
        fileText.isNullOrBlank() -> document.notes
        document.notes.isBlank() -> fileText
        fileText.startsWith(document.notes.take(80)) -> fileText
        document.notes.contains(fileText.take(80)) -> document.notes
        else -> "${document.notes} $fileText"
      }

    return buildString {
        append("${document.title} (${document.documentType}) ")
        if (document.issuingCountry.isNotBlank()) {
          append("Country: ${document.issuingCountry} ")
        }
        if (document.documentNumberMasked.isNotBlank()) {
          append("ID: ${document.documentNumberMasked} ")
        }
        if (combinedBody.isNotBlank()) {
          append(combinedBody)
        }
      }
      .replace(Regex("""\s+"""), " ")
      .trim()
      .take(maxChars)
  }

  /** Reads the content of a document file for inspection or model consumption. */
  open suspend fun readDocumentContent(document: TravelDocument): String =
    withContext(Dispatchers.IO) {
      val fileContent: String? =
        if (document.filePath.isNotBlank()) {
          val file = File(document.filePath)
          if (file.exists() && file.isFile) {
            extractTextFromFile(file, document.mimeType)
          } else {
            null
          }
        } else {
          null
        }

      buildString {
          append("Document: ${document.title} (${document.documentType})\n")
          if (document.issuingCountry.isNotBlank())
            append("Issuing Country: ${document.issuingCountry}\n")
          if (document.documentNumberMasked.isNotBlank())
            append("Document Number: ${document.documentNumberMasked}\n")
          if (document.expiryDate > 0L) {
            try {
              val expiry = Instant.ofEpochMilli(document.expiryDate).toString().substringBefore('T')
              append("Expiry Date: $expiry\n")
            } catch (_: Exception) {}
          }
          if (document.notes.isNotBlank()) append("Details/Notes:\n${document.notes}\n")
          if (!fileContent.isNullOrBlank()) {
            append("File Content:\n$fileContent\n")
          } else if (document.filePath.isNotBlank()) {
            val f = File(document.filePath)
            if (f.exists()) {
              append("Attached File: ${f.name} (${f.length()} bytes, MIME: ${document.mimeType})\n")
            }
          }
        }
        .trim()
    }

  /** Deletes a document from both private storage and the Room database. */
  open suspend fun deleteDocument(document: TravelDocument) =
    withContext(Dispatchers.IO) {
      if (document.filePath.isNotBlank()) {
        try {
          val file = File(document.filePath)
          if (file.exists()) file.delete()
        } catch (e: Exception) {
          Log.w(TAG, "Failed to delete file ${document.filePath}", e)
        }
      }
      travelDocumentDao.deleteDocument(document)
    }

  /**
   * Pre-populates a diverse set of mock travel documents across all supported modalities (`.txt`,
   * `.pdf`, `.eml`, and `.jpg`) on initial launch, and automatically upgrades legacy 3-file
   * plain-text seed states.
   */
  open suspend fun seedDefaultDocumentsIfEmpty() =
    withContext(Dispatchers.IO) {
      val existing = travelDocumentDao.getAllDocuments().first()
      if (existing.isNotEmpty()) {
        val hasMultimodalSeed =
          existing.any { it.mimeType == "application/pdf" } &&
            existing.any { it.mimeType == "message/rfc822" }
        val hasLegacySeed = existing.any {
          it.title == "Passport (Netherlands)" ||
            it.title == "US ESTA Travel Authorization" ||
            it.title == "Flight Voucher - United Airlines UA 991"
        }
        if (!hasMultimodalSeed && hasLegacySeed) {
          val legacyTitles = setOf(
            "Passport (Netherlands)",
            "US ESTA Travel Authorization",
            "Flight Voucher - United Airlines UA 991",
          )
          existing.filter { it.title in legacyTitles }.forEach { deleteDocument(it) }
          seedDiverseMockDocuments()
          runCatching { semanticSearchProvider?.syncIndex() }
        }
        return@withContext
      }
      seedDiverseMockDocuments()
    }

  /**
   * Clears all existing travel documents from Room, deletes their private storage files,
   * and clears the semantic search index.
   */
  open suspend fun clearAllDocuments() =
    withContext(Dispatchers.IO) {
      val existing = travelDocumentDao.getAllDocuments().first()
      for (doc in existing) {
        if (doc.filePath.isNotBlank()) {
          runCatching { File(doc.filePath).takeIf { it.exists() }?.delete() }
        }
      }
      runCatching {
        documentsDir.listFiles()?.forEach { file ->
          if (file.isFile) file.delete()
        }
      }
      travelDocumentDao.deleteAllDocuments()
      try {
        semanticSearchProvider?.clearIndex()
      } catch (e: Exception) {
        Log.w(TAG, "Failed to clear semantic search index", e)
      }
    }

  /**
   * Clears all existing travel documents and private files, then seeds the fresh 8-document
   * multimodal mock set covering `.txt`, `.pdf`, `.eml`, and `.jpg` across demo trips.
   */
  open suspend fun resetDefaultDocuments() =
    withContext(Dispatchers.IO) {
      clearAllDocuments()
      seedDiverseMockDocuments()
      try {
        semanticSearchProvider?.syncIndex()
      } catch (e: Exception) {
        Log.w(TAG, "Failed to re-index documents after reset", e)
      }
    }

  private suspend fun seedDiverseMockDocuments() {
    // 1. Global Text Document: Dutch Biometric Passport (.txt)
    val passportContent =
      """
      PASSPORT / PASSEPORT
      Type: P
      Issuing State: NLD (Kingdom of the Netherlands)
      Passport No: ••••••4821
      Surname: CHEN
      Given Names: ALEX
      Nationality: NEDERLANDSE / DUTCH
      Date of Birth: 12 AUG 1992
      Sex: F
      Date of Issue: 15 NOV 2019
      Date of Expiry: 15 NOV 2029
      Authority: BURGEMEESTER VAN AMSTERDAM
      Type: Biometric with electronic RFID chip.
      Emergency Contact: Netherlands Ministry of Foreign Affairs +31 247 247 247
      """
        .trimIndent()

    createTextDocument(
      title = "Passport (Netherlands)",
      content = passportContent,
      documentType = "PASSPORT",
      issuingCountry = "Netherlands",
      expiryDate = Instant.parse("2029-11-15T00:00:00Z").toEpochMilli(),
      documentNumberMasked = "••••••4821",
      notes = "Valid EU biometric travel passport",
    )

    // 2. Visa / Travel Authorization Text Document: US ESTA (.txt)
    val estaContent =
      """
      U.S. CUSTOMS AND BORDER PROTECTION
      ELECTRONIC SYSTEM FOR TRAVEL AUTHORIZATION (ESTA)
      Application Number: ESTA-77492109
      Status: AUTHORIZATION APPROVED
      Passport Number: ••••••4821
      Issuing Country: Netherlands
      Expiration Date: April 10, 2027
      Valid for multiple entries to the United States for business or pleasure for up to 90 days per visit.
      Port of Entry: San Francisco International Airport (SFO) - Global Entry Lane Eligible
      """
        .trimIndent()

    createTextDocument(
      title = "US ESTA Travel Authorization",
      content = estaContent,
      documentType = "VISA",
      issuingCountry = "United States",
      expiryDate = Instant.parse("2027-04-10T00:00:00Z").toEpochMilli(),
      documentNumberMasked = "ESTA-••••9012",
      notes = "Approved ESTA waiver for US travel (Global Entry eligible)",
      tripId = "2026-1",
      eventId = null,
    )

    // 3. PDF Boarding Pass & E-Ticket: United Airlines UA 991 (.pdf)
    createSeedPdfDocument(
      filename = "UA991_Boarding_Pass_SFO.pdf",
      title = "Flight Voucher - United Airlines UA 991",
      lines =
        listOf(
          "UNITED AIRLINES - ELECTRONIC TICKET & BOARDING PASS",
          "Confirmation Reference (PNR): KL8821UA",
          "Passenger: ALEX CHEN (Premier Gold / Star Alliance Gold)",
          "Flight: UA 991 - Boeing 777-300ER",
          "Depart: CDG Paris Charles de Gaulle - Oct 27, 2026 @ 09:00 AM (Terminal 2E, Gate K42)",
          "Arrive: SFO San Francisco Intl - Oct 27, 2026 @ 12:15 PM (Intl Terminal G, Gate G94)",
          "Seat: 32B (Economy Plus - Extra Legroom Window)",
          "Boarding Group: Group 1 (Premier Access)",
          "Baggage Allowance: 2 checked bags (23kg each) + 1 carry-on + 1 personal item",
          "Wi-Fi Voucher Code: UA-WIFI-883920",
        ),
      documentType = "TICKET",
      issuingCountry = "United States",
      expiryDate = Instant.parse("2026-11-05T00:00:00Z").toEpochMilli(),
      documentNumberMasked = "UA991-KL8821UA",
      notes =
        "PDF E-Ticket & Boarding Pass • Flight UA 991 CDG->SFO • Gate K42 • Seat 32B • PNR KL8821UA • Baggage 2x23kg",
      tripId = "2026-1",
      eventId = "io_1",
    )

    // 4. Email Reservation (.eml with HTML): The Redwood Grove Hotel
    val redwoodEmailHtml =
      """
      From: "The Redwood Grove Hotel Reservations" <reservations@redwoodgrovehotel.com>
      To: "Alex Chen" <alex.chen@example.com>
      Subject: Confirmation #RG-90421: The Redwood Grove Hotel (Oct 27 - Nov 1)
      Date: Mon, 12 Oct 2026 14:22:00 -0700
      MIME-Version: 1.0
      Content-Type: text/html; charset="UTF-8"

      <html>
        <body>
          <h2>The Redwood Grove Hotel — Mountain View</h2>
          <p>Dear Alex Chen, your reservation for <strong>Android Dev Summit '26</strong> is confirmed.</p>
          <table>
            <tr><td><strong>Confirmation Number:</strong></td><td>RG-90421-MV</td></tr>
            <tr><td><strong>Room Type:</strong></td><td>Room 412 — Deluxe King Suite (Redwood Courtyard View)</td></tr>
            <tr><td><strong>Check-in:</strong></td><td>Tuesday, October 27, 2026 after 3:00 PM (Digital Key enabled)</td></tr>
            <tr><td><strong>Check-out:</strong></td><td>Sunday, November 1, 2026 at 11:00 AM (Late check-out 1:00 PM approved)</td></tr>
            <tr><td><strong>Guest Wi-Fi:</strong></td><td>Network: <code>RedwoodGuest</code> / Passcode: <code>SEQUOIA2026</code></td></tr>
            <tr><td><strong>Breakfast &amp; Amenities:</strong></td><td>Complimentary breakfast 6:30 AM - 10:00 AM at Canopy Bistro; EV Level-2 chargers in Garage B2.</td></tr>
            <tr><td><strong>Conference Shuttle:</strong></td><td>Direct shuttle to Shoreline Amphitheatre departs every 20 minutes from Main Lobby.</td></tr>
          </table>
        </body>
      </html>
      """
        .trimIndent()

    createSeedEmailDocument(
      filename = "redwood_grove_hotel_confirmation.eml",
      title = "The Redwood Grove Hotel — Booking Email",
      rawEml = redwoodEmailHtml,
      documentType = "HOTEL",
      issuingCountry = "United States",
      expiryDate = Instant.parse("2026-11-02T00:00:00Z").toEpochMilli(),
      documentNumberMasked = "RG-90421-MV",
      tripId = "2026-1",
      eventId = "io_3",
    )

    // 5. PDF Rental Voucher: Zephyr Wheels EV Rental @ SFO (.pdf)
    createSeedPdfDocument(
      filename = "Zephyr_EV_Rental_SFO.pdf",
      title = "Zephyr Wheels Confirmation",
      lines =
        listOf(
          "ZEPHYR WHEELS ELECTRIC VEHICLE RENTAL - SFO AIRPORT",
          "Reservation Code: ZW-55820-SFO",
          "Primary Driver: Alex Chen",
          "Vehicle Class: Polestar 2 Long Range Dual Motor (Full Electric EV)",
          "Pick-up Location: SFO Rental Car Center, Level 4, Express Stall #18",
          "Pick-up Time: Tuesday, October 27, 2026 @ 1:30 PM",
          "Return Time: Sunday, November 1, 2026 @ 4:00 PM (Return with at least 20% battery)",
          "Toll Pass Included: Bay Area FasTrak Transponder #FT-883920 (All bridge & express lanes)",
          "Charging Perk: Unlimited complimentary fast charging at Electrify America stations (PIN: 2026)",
          "Insurance Coverage: Full Loss Damage Waiver (LDW) + Zero Deductible Roadside Assistance",
        ),
      documentType = "OTHER",
      issuingCountry = "United States",
      expiryDate = Instant.parse("2026-11-02T00:00:00Z").toEpochMilli(),
      documentNumberMasked = "ZW-55820-SFO",
      notes =
        "PDF EV Car Rental Voucher • Polestar 2 at SFO Level 4 Stall 18 • FasTrak #FT-883920 • Charging PIN 2026",
      tripId = "2026-1",
      eventId = "io_2",
    )

    // 6. Scanned Image Card (.jpg): SFMOMA VIP Press & Museum Pass
    createSeedImageDocument(
      title = "SFMOMA VIP Museum & Exhibition Pass",
      headerBanner = "SFMOMA • SAN FRANCISCO MUSEUM OF MODERN ART",
      accentColor = Color.rgb(183, 28, 28),
      cardLines =
        listOf(
          "Pass ID: SFM-2026-8841 (VIP Press & Member All-Access)",
          "Holder: Alex Chen",
          "Timed Entry: Thursday, Oct 29, 2026 @ 2:00 PM",
          "Special Exhibition: Digital Horizons & AI Art (Floor 5)",
          "Audio Guide Code: SFMOMA-AI-77 • Coat Check Locker: #C-42",
        ),
      documentType = "TICKET",
      issuingCountry = "United States",
      expiryDate = Instant.parse("2026-10-30T00:00:00Z").toEpochMilli(),
      documentNumberMasked = "SFM-2026-8841",
      notes =
        "Scanned VIP Museum Pass Image • SFMOMA Floor 5 Digital Horizons exhibition • Audio Guide SFMOMA-AI-77 • Locker #C-42",
      tripId = "2026-1",
      eventId = "io_5",
    )

    // 7. Email E-Ticket (.eml with HTML): Eiffel Tower Summit & Seine River Cruise (Paris Trip)
    val parisEmailHtml =
      """
      From: "Societe d'Exploitation de la Tour Eiffel" <billetterie@toureiffel.paris>
      To: "Alex Chen" <alex.chen@example.com>
      Subject: Vos billets Tour Eiffel Sommet #EF-44210 & Croisiere Seine
      Date: Thu, 01 Oct 2026 09:15:00 +0200
      MIME-Version: 1.0
      Content-Type: text/html; charset="UTF-8"

      <html>
        <body>
          <h2>Eiffel Tower Summit Access &amp; Champagne River Cruise</h2>
          <p>Booking Reference: <strong>EF-44210-PAR</strong> — 2 Adult Summit Elevator Tickets</p>
          <ul>
            <li><strong>Entry Gate:</strong> Pilier Sud (South Security Entrance, Green Fast-Track Lane)</li>
            <li><strong>Summit Elevator Time Slot:</strong> 21:00 (Arrive 20 minutes early for security screening)</li>
            <li><strong>Included Access:</strong> 1st Floor Glass Walk, 2nd Floor Observation Deck, and Top Summit Champagne Bar</li>
            <li><strong>Seine Illuminations Cruise:</strong> Bateaux Parisiens Pier 3 (Port de la Bourdonnais) — QR Code <code>SEINE-9921</code></li>
          </ul>
        </body>
      </html>
      """
        .trimIndent()

    createSeedEmailDocument(
      filename = "eiffel_tower_summit_tickets.eml",
      title = "Eiffel Tower Summit & Seine Cruise — Email",
      rawEml = parisEmailHtml,
      documentType = "TICKET",
      issuingCountry = "France",
      expiryDate = Instant.parse("2026-12-31T00:00:00Z").toEpochMilli(),
      documentNumberMasked = "EF-44210-PAR",
      tripId = "2026-3",
      eventId = "par_5",
    )

    // 8. Scanned Image Card (.jpg): JR East Welcome Suica & Shinkansen IC Card (Tokyo Trip)
    createSeedImageDocument(
      title = "JR East Welcome Suica & Shinkansen IC Card",
      headerBanner = "JR EAST • WELCOME SUICA & SHINKANSEN EX-IC",
      accentColor = Color.rgb(27, 94, 32),
      cardLines =
        listOf(
          "IC Card Number: JE4092-8812-3301 (No deposit required)",
          "Preloaded Transit Balance: 10,000 JPY (Subway, JR Lines & Konbini)",
          "Linked Bullet Train: Tokaido Shinkansen Nozomi 24 (Tokyo -> Kyoto)",
          "Reserved Seat: Car 8, Seat 12E (Mt. Fuji Window Side + Oversized Baggage)",
          "Coin Locker PIN at Tokyo Station Yaesu North Exit: 8419",
        ),
      documentType = "TICKET",
      issuingCountry = "Japan",
      expiryDate = Instant.parse("2026-12-15T00:00:00Z").toEpochMilli(),
      documentNumberMasked = "JE4092-3301",
      notes =
        "Scanned IC Transit Card Image • Welcome Suica 10,000 JPY • Linked Shinkansen Nozomi 24 Car 8 Seat 12E • Tokyo Station Locker PIN 8419",
      tripId = "2026-4",
      eventId = null,
    )
  }

  private suspend fun createSeedPdfDocument(
    filename: String,
    title: String,
    lines: List<String>,
    documentType: String,
    issuingCountry: String,
    expiryDate: Long,
    documentNumberMasked: String,
    notes: String,
    tripId: String? = null,
    eventId: String? = null,
  ): TravelDocument {
    val safePrefix = UUID.randomUUID().toString().take(8)
    val destFile = File(documentsDir, "${safePrefix}_$filename")
    destFile.writeBytes(buildPdfDocumentBytes(filename, title, lines))
    val docId = "${documentType}_${UUID.randomUUID().toString().take(8)}"
    val doc =
      TravelDocument(
        id = UUID.randomUUID().toString(),
        documentId = docId,
        documentType = documentType.uppercase(),
        title = title,
        issuingCountry = issuingCountry,
        expiryDate = expiryDate,
        documentNumberMasked = documentNumberMasked,
        filePath = destFile.absolutePath,
        mimeType = "application/pdf",
        notes = notes,
        tripId = tripId,
        eventId = eventId,
      )
    travelDocumentDao.insertDocument(doc)
    return doc
  }

  private suspend fun createSeedEmailDocument(
    filename: String,
    title: String,
    rawEml: String,
    documentType: String,
    issuingCountry: String,
    expiryDate: Long,
    documentNumberMasked: String,
    tripId: String? = null,
    eventId: String? = null,
  ): TravelDocument {
    val safePrefix = UUID.randomUUID().toString().take(8)
    val destFile = File(documentsDir, "${safePrefix}_$filename")
    destFile.writeText(rawEml)
    val parsedSummary = parseEmailContent(rawEml).formattedSummary().take(MAX_AUTO_NOTES_CHARS)
    val docId = "${documentType}_${UUID.randomUUID().toString().take(8)}"
    val doc =
      TravelDocument(
        id = UUID.randomUUID().toString(),
        documentId = docId,
        documentType = documentType.uppercase(),
        title = title,
        issuingCountry = issuingCountry,
        expiryDate = expiryDate,
        documentNumberMasked = documentNumberMasked,
        filePath = destFile.absolutePath,
        mimeType = "message/rfc822",
        notes = parsedSummary,
        tripId = tripId,
        eventId = eventId,
      )
    travelDocumentDao.insertDocument(doc)
    return doc
  }

  private suspend fun createSeedImageDocument(
    title: String,
    headerBanner: String,
    accentColor: Int,
    cardLines: List<String>,
    documentType: String,
    issuingCountry: String,
    expiryDate: Long,
    documentNumberMasked: String,
    notes: String,
    tripId: String? = null,
    eventId: String? = null,
  ): TravelDocument {
    val width = 640
    val height = 380
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Card background
    canvas.drawColor(Color.rgb(250, 250, 252))

    // Colored header banner
    paint.color = accentColor
    canvas.drawRect(0f, 0f, width.toFloat(), 64f, paint)

    // Header text
    paint.color = Color.WHITE
    paint.textSize = 20f
    paint.isFakeBoldText = true
    canvas.drawText(headerBanner, 24f, 40f, paint)

    // Document title
    paint.color = Color.rgb(25, 28, 32)
    paint.textSize = 22f
    canvas.drawText(title, 24f, 104f, paint)

    // Body lines
    paint.isFakeBoldText = false
    paint.textSize = 16f
    paint.color = Color.rgb(55, 60, 68)
    var y = 144f
    for (line in cardLines) {
      canvas.drawText(line, 24f, y, paint)
      y += 34f
    }

    // Simulated high-contrast QR code block in the bottom-right corner for visual realism
    paint.color = Color.rgb(30, 30, 30)
    val qrOriginX = width - 116f
    val qrOriginY = height - 116f
    val cell = 10f
    for (row in 0 until 9) {
      for (col in 0 until 9) {
        val isFinder =
          (row in 0..2 && col in 0..2) ||
            (row in 0..2 && col in 6..8) ||
            (row in 6..8 && col in 0..2)
        val isPattern = (row * 7 + col * 13 + title.length) % 3 == 0
        if (isFinder || isPattern) {
          canvas.drawRect(
            qrOriginX + col * cell,
            qrOriginY + row * cell,
            qrOriginX + (col + 1) * cell - 1f,
            qrOriginY + (row + 1) * cell - 1f,
            paint,
          )
        }
      }
    }

    val saved =
      saveDocumentBitmap(
        bitmap = bitmap,
        title = title,
        documentType = documentType,
        issuingCountry = issuingCountry,
        expiryDate = expiryDate,
        documentNumberMasked = documentNumberMasked,
        notes = notes,
        tripId = tripId,
        eventId = eventId,
      )
    return saved
  }

  /**
   * Generates a rich, realistic PDF document binary using Android's native [PdfDocument] canvas API.
   * Renders high-fidelity branded travel vouchers (e.g. Zephyr Wheels EV Rental, United Airlines Boarding Pass)
   * that display sharply in [PdfRenderer] and keynote presentation slides.
   */
  private fun buildPdfDocumentBytes(
    filename: String,
    title: String,
    lines: List<String>,
  ): ByteArray {
    return try {
      when {
        filename.contains("Zephyr", ignoreCase = true) -> buildZephyrRentalPdf()
        filename.contains("UA991", ignoreCase = true) -> buildFlightTicketPdf()
        else -> buildGenericPdf(title, lines)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to render visual PDF via PdfDocument; falling back to basic PDF", e)
      buildSinglePagePdfBytes(lines)
    }
  }

  private fun buildZephyrRentalPdf(): ByteArray {
    val pdfDoc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
    val page = pdfDoc.startPage(pageInfo)
    val canvas = page.canvas

    // Clean background
    canvas.drawColor(Color.rgb(248, 250, 252))

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val left = 24f
    val right = 588f

    // 1. Header Banner (Deep emerald #0A2E26)
    paint.color = Color.rgb(10, 46, 38)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(RectF(left, 24f, right, 120f), 14f, 14f, paint)

    // Brand logo & name
    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 22f
    canvas.drawText("ZEPHYR WHEELS", left + 20f, 62f, paint)

    // Electric vehicle mobility tagline
    paint.color = Color.rgb(167, 243, 208)
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 10f
    canvas.drawText("ELECTRIC MOBILITY • SFO PREMIER FLEET", left + 20f, 82f, paint)

    // Status Pill on top right
    val pillRight = right - 20f
    val pillLeft = pillRight - 116f
    paint.color = Color.rgb(16, 185, 129)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(RectF(pillLeft, 42f, pillRight, 70f), 14f, 14f, paint)

    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 11f
    canvas.drawText("● CONFIRMED", pillLeft + 14f, 60f, paint)

    // Reservation Ref below status pill
    paint.color = Color.rgb(226, 232, 240)
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    paint.textSize = 11f
    canvas.drawText("RES: ZW-55820-SFO", pillLeft - 22f, 96f, paint)

    // 2. Reserved Vehicle Showcase Card
    val vehTop = 132f
    val vehBottom = 260f
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, vehTop, right, vehBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, vehTop, right, vehBottom), 14f, 14f, paint)
    paint.style = Paint.Style.FILL

    // Section caption
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(100, 116, 139)
    canvas.drawText("RESERVED VEHICLE SPECIFICATION", left + 18f, vehTop + 24f, paint)

    // Vehicle Name
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 18f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("Polestar 2 Long Range Dual Motor", left + 18f, vehTop + 50f, paint)

    // Vehicle category description
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 11f
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText(
      "Class: Premium Full-Electric Sedan • All-Wheel Drive (AWD) • Void Black",
      left + 18f,
      vehTop + 70f,
      paint,
    )

    // Badges Row
    fun drawBadge(x: Float, y: Float, text: String, bgColor: Int, textColor: Int, borderColor: Int): Float {
      paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      paint.textSize = 10f
      val textW = paint.measureText(text)
      val padH = 10f
      val badgeW = textW + (padH * 2)
      val badgeH = 22f

      paint.style = Paint.Style.FILL
      paint.color = bgColor
      canvas.drawRoundRect(RectF(x, y, x + badgeW, y + badgeH), 6f, 6f, paint)

      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 1f
      paint.color = borderColor
      canvas.drawRoundRect(RectF(x, y, x + badgeW, y + badgeH), 6f, 6f, paint)

      paint.style = Paint.Style.FILL
      paint.color = textColor
      canvas.drawText(text, x + padH, y + 15f, paint)
      return badgeW
    }

    var badgeX = left + 18f
    val badgeY = vehTop + 88f
    badgeX +=
      drawBadge(
        badgeX,
        badgeY,
        "⚡ 100% Electric",
        Color.rgb(236, 253, 245),
        Color.rgb(6, 95, 70),
        Color.rgb(167, 243, 208),
      ) + 8f
    badgeX +=
      drawBadge(
        badgeX,
        badgeY,
        "🔋 300+ mi Range",
        Color.rgb(239, 246, 255),
        Color.rgb(30, 64, 175),
        Color.rgb(191, 219, 254),
      ) + 8f
    badgeX +=
      drawBadge(
        badgeX,
        badgeY,
        "🏎 408 hp AWD",
        Color.rgb(248, 250, 252),
        Color.rgb(51, 65, 85),
        Color.rgb(226, 232, 240),
      ) + 8f
    drawBadge(
      badgeX,
      badgeY,
      "✦ 85%+ Battery Guaranteed",
      Color.rgb(254, 243, 199),
      Color.rgb(146, 64, 14),
      Color.rgb(253, 230, 138),
    )

    // 3. Rental Itinerary Grid (Two Columns: Pick-up & Return)
    val itinTop = 272f
    val itinBottom = 416f
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, itinTop, right, itinBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, itinTop, right, itinBottom), 14f, 14f, paint)

    // Vertical Divider
    val midX = (left + right) / 2f
    paint.color = Color.rgb(241, 245, 249)
    paint.strokeWidth = 1.5f
    canvas.drawLine(midX, itinTop + 14f, midX, itinBottom - 14f, paint)
    paint.style = Paint.Style.FILL

    // Column 1: PICK-UP
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 10f
    paint.color = Color.rgb(5, 150, 105)
    canvas.drawText("▲ PICK-UP (DEPARTURE)", left + 18f, itinTop + 26f, paint)

    paint.textSize = 13.5f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("Tuesday, Oct 27, 2026 @ 1:30 PM", left + 18f, itinTop + 48f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 11.5f
    paint.color = Color.rgb(30, 41, 59)
    canvas.drawText("SFO International Airport", left + 18f, itinTop + 72f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 10.5f
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText("Rental Car Center • Level 4, Express Stall #18", left + 18f, itinTop + 90f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(37, 99, 235)
    canvas.drawText("✓ Direct Stall Access • Digital Key Active in App", left + 18f, itinTop + 116f, paint)

    // Column 2: RETURN
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 10f
    paint.color = Color.rgb(220, 38, 38)
    canvas.drawText("▼ RETURN (DROP-OFF)", midX + 18f, itinTop + 26f, paint)

    paint.textSize = 13.5f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("Sunday, Nov 1, 2026 @ 4:00 PM", midX + 18f, itinTop + 48f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 11.5f
    paint.color = Color.rgb(30, 41, 59)
    canvas.drawText("SFO International Airport", midX + 18f, itinTop + 72f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 10.5f
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText("Rental Car Center • Level 4, Express Key Drop", midX + 18f, itinTop + 90f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(100, 116, 139)
    canvas.drawText("✓ Return with ≥ 20% battery (No recharge fee)", midX + 18f, itinTop + 116f, paint)

    // 4. Inclusions & Driver Details Card
    val incTop = 428f
    val incBottom = 570f
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, incTop, right, incBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, incTop, right, incBottom), 14f, 14f, paint)
    paint.style = Paint.Style.FILL

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(100, 116, 139)
    canvas.drawText("EXCLUSIVE INCLUSIONS & DRIVER CREDENTIALS", left + 18f, incTop + 24f, paint)

    var rowY = incTop + 48f
    fun drawInclusionRow(label: String, value: String, highlight: Boolean = false) {
      paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      paint.textSize = 11f
      paint.color = Color.rgb(15, 23, 42)
      canvas.drawText(label, left + 18f, rowY, paint)

      paint.typeface =
        Typeface.create(Typeface.SANS_SERIF, if (highlight) Typeface.BOLD else Typeface.NORMAL)
      paint.textSize = 11f
      paint.color = if (highlight) Color.rgb(5, 150, 105) else Color.rgb(51, 65, 85)
      canvas.drawText(value, left + 160f, rowY, paint)
      rowY += 24f
    }

    drawInclusionRow("Primary Driver:", "Alex Chen (Verified Global Driving License)")
    drawInclusionRow(
      "Toll Transponder:",
      "Bay Area FasTrak #FT-883920 (All Bridges & Lanes Included)",
      highlight = true,
    )
    drawInclusionRow(
      "EV Charging Perk:",
      "Electrify America Unlimited Fast Charging (PIN: 2026)",
      highlight = true,
    )
    drawInclusionRow(
      "Insurance Cover:",
      "Full Loss Damage Waiver (LDW) + Zero Deductible 24/7 Roadside",
    )

    // 5. Payment & Financial Summary Bar
    val payTop = 582f
    val payBottom = 654f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249)
    canvas.drawRoundRect(RectF(left, payTop, right, payBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    paint.color = Color.rgb(203, 213, 225)
    canvas.drawRoundRect(RectF(left, payTop, right, payBottom), 14f, 14f, paint)
    paint.style = Paint.Style.FILL

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 10f
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText("BILLING & CHARGES (5 DAYS RENTAL)", left + 18f, payTop + 22f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 10f
    paint.color = Color.rgb(100, 116, 139)
    canvas.drawText(
      "Rate: 5 x $64.00/day • Clean Mobility EV Credit • SFO Concession Recovery Fee Included",
      left + 18f,
      payTop + 40f,
      paint,
    )

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 10.5f
    paint.color = Color.rgb(5, 150, 105)
    canvas.drawText("✓ PAID IN FULL (VISA ••••4821)", left + 18f, payTop + 58f, paint)

    // Total Amount on Right
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 9.5f
    paint.color = Color.rgb(100, 116, 139)
    canvas.drawText("TOTAL CHARGED", right - 130f, payTop + 24f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 24f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("$360.50", right - 130f, payTop + 54f, paint)

    // 6. Express Gate Barcode & QR Verification Card
    val barTop = 666f
    val barBottom = 768f
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, barTop, right, barBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, barTop, right, barBottom), 14f, 14f, paint)
    paint.style = Paint.Style.FILL

    // Draw realistic barcode pattern
    val barLeft = left + 20f
    val barWidth = 380f
    val barHeight = 40f
    val barY = barTop + 14f
    paint.color = Color.rgb(15, 23, 42)
    val barPattern =
      intArrayOf(
        2, 1, 3, 1, 1, 2, 3, 2, 1, 1, 3, 1, 2, 2, 1, 3, 1, 1, 2, 1, 3, 2, 1, 2, 2, 1, 1, 3, 2, 1, 1,
        2, 3, 1, 2, 1, 3, 2, 1, 1, 2, 3, 1, 2, 1, 1, 3, 2, 1, 2, 1, 3, 1, 2, 2, 1, 3, 1, 1, 2, 3,
        1, 2, 1, 1, 3, 2, 1, 2, 3, 1, 1, 2, 1, 3, 2, 1, 2, 2, 1, 3, 1, 2, 1, 1, 3, 2, 1, 2, 1,
      )
    var currentBarX = barLeft
    var isBar = true
    for (w in barPattern) {
      if (isBar && currentBarX < barLeft + barWidth) {
        val barW = w.toFloat() * 1.8f
        canvas.drawRect(currentBarX, barY, currentBarX + barW, barY + barHeight, paint)
      }
      currentBarX += w.toFloat() * 1.8f
      isBar = !isBar
    }

    // Barcode number text
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText(
      "* ZW-55820-SFO-20261027-POLESTAR2-AWD *",
      barLeft + 16f,
      barY + barHeight + 16f,
      paint,
    )

    // Footer instruction
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 8.5f
    paint.color = Color.rgb(148, 163, 184)
    canvas.drawText(
      "Scan barcode at Express Gate Stall #18 barrier for automated access • 24/7 Support: 1-800-ZEPHYR-EV",
      barLeft,
      barY + barHeight + 30f,
      paint,
    )

    // QR Code square on right side
    val qrSize = 64f
    val qrX = right - qrSize - 20f
    val qrY = barTop + 14f
    val cellSize = qrSize / 8f
    paint.color = Color.rgb(15, 23, 42)
    for (r in 0 until 8) {
      for (c in 0 until 8) {
        val isFinder =
          (r in 0..2 && c in 0..2) || (r in 0..2 && c in 5..7) || (r in 5..7 && c in 0..2)
        val isPixel = isFinder || ((r * 3 + c * 7 + 5) % 2 == 0)
        if (isPixel) {
          canvas.drawRect(
            qrX + c * cellSize,
            qrY + r * cellSize,
            qrX + (c + 1) * cellSize - 0.5f,
            qrY + (r + 1) * cellSize - 0.5f,
            paint,
          )
        }
      }
    }

    pdfDoc.finishPage(page)
    val out = ByteArrayOutputStream()
    pdfDoc.writeTo(out)
    pdfDoc.close()
    return out.toByteArray()
  }

  private fun buildFlightTicketPdf(): ByteArray {
    val pdfDoc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
    val page = pdfDoc.startPage(pageInfo)
    val canvas = page.canvas

    canvas.drawColor(Color.rgb(248, 250, 252))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val left = 24f
    val right = 588f

    // 1. Header Banner (United Navy #002244)
    paint.color = Color.rgb(0, 34, 68)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(RectF(left, 24f, right, 120f), 14f, 14f, paint)

    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 22f
    canvas.drawText("UNITED AIRLINES", left + 20f, 62f, paint)

    paint.color = Color.rgb(147, 197, 253)
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 10f
    canvas.drawText("ELECTRONIC TICKET & BOARDING PASS • STAR ALLIANCE", left + 20f, 82f, paint)

    val pillRight = right - 20f
    val pillLeft = pillRight - 116f
    paint.color = Color.rgb(16, 185, 129)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(RectF(pillLeft, 42f, pillRight, 70f), 14f, 14f, paint)

    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 11f
    canvas.drawText("● CONFIRMED", pillLeft + 14f, 60f, paint)

    paint.color = Color.rgb(226, 232, 240)
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    paint.textSize = 11f
    canvas.drawText("PNR: KL8821UA", pillLeft - 18f, 96f, paint)

    // 2. Passenger & Status Card
    val passTop = 132f
    val passBottom = 222f
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, passTop, right, passBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, passTop, right, passBottom), 14f, 14f, paint)
    paint.style = Paint.Style.FILL

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(100, 116, 139)
    canvas.drawText("PASSENGER DETAILS & STATUS", left + 18f, passTop + 24f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 18f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("ALEX CHEN", left + 18f, passTop + 50f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 11f
    paint.color = Color.rgb(37, 99, 235)
    canvas.drawText(
      "Premier Gold • Star Alliance Gold • Global Entry Lane Eligible",
      left + 18f,
      passTop + 72f,
      paint,
    )

    // 3. Flight Route Card (Two Columns)
    val routeTop = 234f
    val routeBottom = 384f
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, routeTop, right, routeBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, routeTop, right, routeBottom), 14f, 14f, paint)

    val midX = (left + right) / 2f
    paint.color = Color.rgb(241, 245, 249)
    paint.strokeWidth = 1.5f
    canvas.drawLine(midX, routeTop + 14f, midX, routeBottom - 14f, paint)
    paint.style = Paint.Style.FILL

    // CDG
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 10f
    paint.color = Color.rgb(5, 150, 105)
    canvas.drawText("▲ DEPARTURE (ORIGIN)", left + 18f, routeTop + 26f, paint)

    paint.textSize = 14f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("CDG Paris Charles de Gaulle", left + 18f, routeTop + 50f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 11f
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText("Tue, Oct 27, 2026 @ 09:00 AM", left + 18f, routeTop + 70f, paint)
    canvas.drawText("Terminal 2E • Gate K42", left + 18f, routeTop + 90f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(37, 99, 235)
    canvas.drawText("Boarding Closes 08:45 AM", left + 18f, routeTop + 116f, paint)

    // SFO
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 10f
    paint.color = Color.rgb(30, 64, 175)
    canvas.drawText("▼ ARRIVAL (DESTINATION)", midX + 18f, routeTop + 26f, paint)

    paint.textSize = 14f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("SFO San Francisco Intl", midX + 18f, routeTop + 50f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 11f
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText("Tue, Oct 27, 2026 @ 12:15 PM", midX + 18f, routeTop + 70f, paint)
    canvas.drawText("International Terminal G • Gate G94", midX + 18f, routeTop + 90f, paint)

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(5, 150, 105)
    canvas.drawText("Nonstop • 11h 15m Flight Time", midX + 18f, routeTop + 116f, paint)

    // 4. Seat & Boarding Grid
    val seatTop = 396f
    val seatBottom = 496f
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, seatTop, right, seatBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, seatTop, right, seatBottom), 14f, 14f, paint)
    paint.style = Paint.Style.FILL

    val colW = (right - left) / 4f
    for (i in 0 until 4) {
      val cX = left + (i * colW) + 12f
      when (i) {
        0 -> {
          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          paint.textSize = 9.5f
          paint.color = Color.rgb(100, 116, 139)
          canvas.drawText("SEAT", cX, seatTop + 24f, paint)

          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          paint.textSize = 24f
          paint.color = Color.rgb(15, 23, 42)
          canvas.drawText("32B", cX, seatTop + 58f, paint)

          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
          paint.textSize = 9.5f
          paint.color = Color.rgb(71, 85, 105)
          canvas.drawText("Economy Plus", cX, seatTop + 78f, paint)
        }
        1 -> {
          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          paint.textSize = 9.5f
          paint.color = Color.rgb(100, 116, 139)
          canvas.drawText("GROUP", cX, seatTop + 24f, paint)

          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          paint.textSize = 24f
          paint.color = Color.rgb(15, 23, 42)
          canvas.drawText("1", cX, seatTop + 58f, paint)

          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
          paint.textSize = 9.5f
          paint.color = Color.rgb(71, 85, 105)
          canvas.drawText("Premier Access", cX, seatTop + 78f, paint)
        }
        2 -> {
          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          paint.textSize = 9.5f
          paint.color = Color.rgb(100, 116, 139)
          canvas.drawText("BAGGAGE", cX, seatTop + 24f, paint)

          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          paint.textSize = 20f
          paint.color = Color.rgb(15, 23, 42)
          canvas.drawText("2x 23kg", cX, seatTop + 58f, paint)

          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
          paint.textSize = 9.5f
          paint.color = Color.rgb(71, 85, 105)
          canvas.drawText("Included Free", cX, seatTop + 78f, paint)
        }
        3 -> {
          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          paint.textSize = 9.5f
          paint.color = Color.rgb(100, 116, 139)
          canvas.drawText("IN-FLIGHT WI-FI", cX, seatTop + 24f, paint)

          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          paint.textSize = 14f
          paint.color = Color.rgb(37, 99, 235)
          canvas.drawText("ACTIVE", cX, seatTop + 56f, paint)

          paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
          paint.textSize = 9f
          paint.color = Color.rgb(71, 85, 105)
          canvas.drawText("Code: 883920", cX, seatTop + 78f, paint)
        }
      }
    }

    // 5. Perforated divider & Boarding Barcode Card
    val barTop = 510f
    val barBottom = 768f
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, barTop, right, barBottom), 14f, 14f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, barTop, right, barBottom), 14f, 14f, paint)
    paint.style = Paint.Style.FILL

    // Boarding barcode lines
    val barLeft = left + 20f
    val barWidth = 380f
    val barHeight = 44f
    val barY = barTop + 20f
    paint.color = Color.rgb(15, 23, 42)
    val barPattern =
      intArrayOf(
        3, 1, 2, 1, 3, 2, 1, 1, 2, 3, 1, 2, 1, 3, 1, 1, 2, 3, 2, 1, 1, 3, 1, 2, 2, 1, 3, 1, 1, 2, 1,
        3, 2, 1, 2, 2, 1, 1, 3, 2, 1, 1, 2, 3, 1, 2, 1, 3, 2, 1, 1, 2, 3, 1, 2, 1, 1, 3, 2, 1, 2,
        1, 3, 1, 2, 2, 1, 3, 1, 1, 2, 3, 1, 2, 1, 1, 3, 2, 1, 2, 3, 1, 1, 2, 1, 3, 2, 1, 2, 2, 1,
      )
    var currentBarX = barLeft
    var isBar = true
    for (w in barPattern) {
      if (isBar && currentBarX < barLeft + barWidth) {
        val barW = w.toFloat() * 1.8f
        canvas.drawRect(currentBarX, barY, currentBarX + barW, barY + barHeight, paint)
      }
      currentBarX += w.toFloat() * 1.8f
      isBar = !isBar
    }

    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText(
      "* M1CHEN/ALEX EKL8821UA CDGSFOUA 0991 300Y032B0042 100 *",
      barLeft,
      barY + barHeight + 18f,
      paint,
    )

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 8.5f
    paint.color = Color.rgb(148, 163, 184)
    canvas.drawText(
      "Scan digital boarding pass at Gate K42 automated gate turnstile • Star Alliance Gold Premier Access",
      barLeft,
      barY + barHeight + 32f,
      paint,
    )

    // QR Code square on right side
    val qrSize = 64f
    val qrX = right - qrSize - 20f
    val qrY = barTop + 16f
    val cellSize = qrSize / 8f
    paint.color = Color.rgb(15, 23, 42)
    for (r in 0 until 8) {
      for (c in 0 until 8) {
        val isFinder =
          (r in 0..2 && c in 0..2) || (r in 0..2 && c in 5..7) || (r in 5..7 && c in 0..2)
        val isPixel = isFinder || ((r * 5 + c * 3 + 7) % 2 == 0)
        if (isPixel) {
          canvas.drawRect(
            qrX + c * cellSize,
            qrY + r * cellSize,
            qrX + (c + 1) * cellSize - 0.5f,
            qrY + (r + 1) * cellSize - 0.5f,
            paint,
          )
        }
      }
    }

    pdfDoc.finishPage(page)
    val out = ByteArrayOutputStream()
    pdfDoc.writeTo(out)
    pdfDoc.close()
    return out.toByteArray()
  }

  private fun buildGenericPdf(title: String, lines: List<String>): ByteArray {
    val pdfDoc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
    val page = pdfDoc.startPage(pageInfo)
    val canvas = page.canvas

    canvas.drawColor(Color.rgb(248, 250, 252))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val left = 24f
    val right = 588f

    // Header banner
    paint.color = Color.rgb(30, 41, 59)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(RectF(left, 24f, right, 100f), 12f, 12f, paint)

    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 20f
    canvas.drawText(title.take(36), left + 18f, 64f, paint)

    // Body Card
    val bodyTop = 112f
    val bodyBottom = 768f
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, bodyTop, right, bodyBottom), 12f, 12f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = Color.rgb(226, 232, 240)
    canvas.drawRoundRect(RectF(left, bodyTop, right, bodyBottom), 12f, 12f, paint)
    paint.style = Paint.Style.FILL

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 12f
    paint.color = Color.rgb(51, 65, 85)
    var textY = bodyTop + 36f
    for (line in lines) {
      if (textY > bodyBottom - 30f) break
      canvas.drawText(line.take(80), left + 20f, textY, paint)
      textY += 24f
    }

    pdfDoc.finishPage(page)
    val out = ByteArrayOutputStream()
    pdfDoc.writeTo(out)
    pdfDoc.close()
    return out.toByteArray()
  }

  /**
   * Builds a standards-compliant 1-page PDF 1.4 binary with exact `xref` byte offsets so both
   * Android's native [PdfRenderer] and [extractPdfText] can read it.
   */
  private fun buildSinglePagePdfBytes(lines: List<String>): ByteArray {
    fun escapePdfLiteral(text: String): String =
      text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

    val contentStream = buildString {
      append("BT\n/F1 11 Tf\n18 TL\n42 730 Td\n")
      lines.forEachIndexed { idx, line ->
        if (idx > 0) append("T*\n")
        append("(${escapePdfLiteral(line)}) Tj\n")
      }
      append("ET")
    }
    val streamBytes = contentStream.toByteArray(StandardCharsets.ISO_8859_1)

    val out = ByteArrayOutputStream()
    fun writeAscii(str: String) {
      out.write(str.toByteArray(StandardCharsets.ISO_8859_1))
    }

    writeAscii("%PDF-1.4\n")
    val offsets = IntArray(6)

    offsets[1] = out.size()
    writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

    offsets[2] = out.size()
    writeAscii("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")

    offsets[3] = out.size()
    writeAscii(
      "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>\nendobj\n"
    )

    offsets[4] = out.size()
    writeAscii("4 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n")
    out.write(streamBytes)
    writeAscii("\nendstream\nendobj\n")

    offsets[5] = out.size()
    writeAscii("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

    val xrefOffset = out.size()
    writeAscii("xref\n0 6\n0000000000 65535 f \n")
    for (i in 1..5) {
      writeAscii(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
    }
    writeAscii("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
    return out.toByteArray()
  }

  private fun extractTextFromFile(file: File, mimeType: String?): String? {
    if (!file.exists() || !file.isFile) return null
    val ext = file.extension.lowercase()
    return try {
      when {
        isEmailMimeOrExt(mimeType, ext) -> {
          val raw = file.readText()
          parseEmailContent(raw).formattedSummary().ifBlank { raw }
        }
        mimeType.equals("text/html", ignoreCase = true) || ext in setOf("html", "htm") -> {
          stripHtml(file.readText())
        }
        mimeType.equals("application/pdf", ignoreCase = true) || ext == "pdf" -> {
          extractPdfText(file)
        }
        isTextMime(mimeType) || ext in setOf("txt", "md", "json", "csv", "text") -> {
          val raw = file.readText()
          if (looksLikeEmailOrHtml(raw)) {
            parseEmailContent(raw).formattedSummary().ifBlank { raw }
          } else {
            raw
          }
        }
        else -> null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not extract text from file ${file.name}", e)
      null
    }
  }

  /**
   * Extracts selectable text from a PDF [file] by scanning uncompressed and `/FlateDecode` PDF
   * content streams for `Tj` and `TJ` text operators.
   *
   * For image-only (scanned) PDFs where this returns null, callers fall back to
   * [renderPdfPageToBitmap] for multimodal `ImagePart` embedding and Gemini Nano vision OCR.
   */
  open fun extractPdfText(file: File): String? {
    if (!file.exists() || !file.isFile || file.length() > MAX_PDF_BYTES) return null
    return try {
      val bytes = file.readBytes()
      val rawLatin1 = String(bytes, StandardCharsets.ISO_8859_1)
      val contentStreams = mutableListOf<String>()

      var searchIdx = 0
      while (searchIdx < rawLatin1.length) {
        val streamStartMarker = rawLatin1.indexOf("stream", searchIdx)
        if (streamStartMarker == -1) break
        // Advance past "stream\r\n" or "stream\n"
        var dataStart = streamStartMarker + 6
        if (dataStart < rawLatin1.length && rawLatin1[dataStart] == '\r') dataStart++
        if (dataStart < rawLatin1.length && rawLatin1[dataStart] == '\n') dataStart++

        val streamEndMarker = rawLatin1.indexOf("endstream", dataStart)
        if (streamEndMarker == -1) break

        var dataEnd = streamEndMarker
        while (
          dataEnd > dataStart &&
            (bytes[dataEnd - 1] == '\n'.code.toByte() || bytes[dataEnd - 1] == '\r'.code.toByte())
        ) {
          dataEnd--
        }

        if (dataEnd > dataStart) {
          val streamBytes = bytes.copyOfRange(dataStart, dataEnd)
          val inflated = tryInflateStream(streamBytes)
          if (inflated != null) {
            contentStreams.add(String(inflated, StandardCharsets.ISO_8859_1))
          } else {
            contentStreams.add(String(streamBytes, StandardCharsets.ISO_8859_1))
          }
        }
        searchIdx = streamEndMarker + 9
      }

      if (contentStreams.isEmpty()) {
        contentStreams.add(rawLatin1)
      }

      val extractedPieces = mutableListOf<String>()
      val tjRegex = Regex("""\((?:\\.|[^\\)])*\)\s*Tj""")
      val tjArrayRegex = Regex("""\[(.*?)\]\s*TJ""", RegexOption.DOT_MATCHES_ALL)
      val parenStringRegex = Regex("""\((?:\\.|[^\\)])*\)""")

      for (stream in contentStreams) {
        if (!stream.contains("Tj") && !stream.contains("TJ")) continue
        // Walk in document order by matching either (..) Tj or [..] TJ
        val combinedRegex =
          Regex("""\((?:\\.|[^\\)])*\)\s*Tj|\[.*?\]\s*TJ""", RegexOption.DOT_MATCHES_ALL)
        val lineBuilder = StringBuilder()
        for (match in combinedRegex.findAll(stream)) {
          val token = match.value
          if (token.endsWith("Tj")) {
            val inner = parenStringRegex.find(token)?.value
            if (inner != null) {
              lineBuilder.append(decodePdfLiteralString(inner.substring(1, inner.length - 1)))
            }
          } else {
            val arrayBody = tjArrayRegex.find(token)?.groupValues?.getOrNull(1).orEmpty()
            for (strMatch in parenStringRegex.findAll(arrayBody)) {
              val s = strMatch.value
              lineBuilder.append(decodePdfLiteralString(s.substring(1, s.length - 1)))
            }
          }
          lineBuilder.append(" ")
        }
        val cleaned = lineBuilder.toString().replace(Regex("""\s+"""), " ").trim()
        if (cleaned.isNotBlank()) {
          extractedPieces.add(cleaned)
        }
      }

      extractedPieces.joinToString("\n").trim().takeIf { it.isNotBlank() }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to extract text from PDF ${file.name}", e)
      null
    }
  }

  private fun tryInflateStream(data: ByteArray): ByteArray? {
    for (nowrap in listOf(false, true)) {
      val inflater = Inflater(nowrap)
      try {
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!inflater.finished()) {
          val count = inflater.inflate(buffer)
          if (count == 0) {
            if (inflater.needsInput() || inflater.needsDictionary()) break
          } else {
            out.write(buffer, 0, count)
          }
        }
        val result = out.toByteArray()
        if (result.isNotEmpty()) return result
      } catch (_: Exception) {
        // Not a valid deflate stream with this nowrap setting; try next.
      } finally {
        inflater.end()
      }
    }
    return null
  }

  private fun decodePdfLiteralString(raw: String): String {
    val sb = StringBuilder(raw.length)
    var i = 0
    while (i < raw.length) {
      val c = raw[i]
      if (c == '\\' && i + 1 < raw.length) {
        val next = raw[i + 1]
        when (next) {
          'n' -> {
            sb.append('\n')
            i += 2
          }
          'r' -> {
            sb.append('\r')
            i += 2
          }
          't' -> {
            sb.append('\t')
            i += 2
          }
          '(',
          ')',
          '\\' -> {
            sb.append(next)
            i += 2
          }
          in '0'..'7' -> {
            var oct = next.toString()
            var consumed = 2
            if (i + 2 < raw.length && raw[i + 2] in '0'..'7') {
              oct += raw[i + 2]
              consumed++
              if (i + 3 < raw.length && raw[i + 3] in '0'..'7') {
                oct += raw[i + 3]
                consumed++
              }
            }
            sb.append(oct.toInt(8).toChar())
            i += consumed
          }
          else -> {
            sb.append(next)
            i += 2
          }
        }
      } else {
        sb.append(c)
        i++
      }
    }
    return sb.toString()
  }

  /** Parsed representation of an RFC 822 `.eml` or shared email message. */
  data class ParsedEmail(
    val subject: String?,
    val from: String?,
    val date: String?,
    val body: String,
  ) {
    fun formattedSummary(): String =
      buildString {
          if (!subject.isNullOrBlank()) append("Subject: $subject\n")
          if (!from.isNullOrBlank()) append("From: $from\n")
          if (!date.isNullOrBlank()) append("Date: $date\n")
          if (isNotEmpty() && body.isNotBlank()) append("\n")
          append(body.trim())
        }
        .trim()
  }

  /**
   * Parses an `.eml` (`message/rfc822`), forwarded email text, or HTML email into structured
   * headers (`Subject`, `From`, `Date`) and clean plain-text body.
   */
  open fun parseEmailContent(raw: String): ParsedEmail {
    val normalized = raw.replace("\r\n", "\n")
    if (!looksLikeEmailHeaders(normalized) && looksLikeHtml(normalized)) {
      return ParsedEmail(subject = null, from = null, date = null, body = stripHtml(normalized))
    }

    val headerBlock: String
    val rawBodyBlock: String
    val blankLineIdx = normalized.indexOf("\n\n")
    if (blankLineIdx != -1) {
      headerBlock = normalized.substring(0, blankLineIdx)
      rawBodyBlock = normalized.substring(blankLineIdx + 2)
    } else {
      val lines = normalized.lines()
      val headerLines = mutableListOf<String>()
      var bodyStartIndex = lines.size
      val headerRegex = Regex("^[A-Za-z][A-Za-z0-9-]*:\\s*.*")
      for (i in lines.indices) {
        val line = lines[i]
        if (headerRegex.matches(line)) {
          headerLines.add(line)
        } else if ((line.startsWith(" ") || line.startsWith("\t")) && headerLines.isNotEmpty()) {
          headerLines.add(line)
        } else {
          bodyStartIndex = i
          break
        }
      }
      if (headerLines.isNotEmpty() && bodyStartIndex < lines.size) {
        headerBlock = headerLines.joinToString("\n")
        rawBodyBlock = lines.subList(bodyStartIndex, lines.size).joinToString("\n")
      } else {
        headerBlock = normalized
        rawBodyBlock = ""
      }
    }

    val headers = parseHeaders(headerBlock)
    val subject = headers["subject"]
    val from = headers["from"]
    val date = headers["date"]
    val contentType = headers["content-type"].orEmpty()
    val transferEncoding = headers["content-transfer-encoding"].orEmpty()

    if (subject == null && from == null && !contentType.contains("multipart", ignoreCase = true)) {
      val cleaned = if (looksLikeHtml(normalized)) stripHtml(normalized) else normalized.trim()
      return ParsedEmail(subject = null, from = null, date = null, body = cleaned)
    }

    val extractedBody = extractMimeBody(rawBodyBlock, contentType, transferEncoding)
    return ParsedEmail(subject = subject, from = from, date = date, body = extractedBody)
  }

  private fun parseHeaders(headerBlock: String): Map<String, String> {
    val unfoldedLines = mutableListOf<String>()
    for (line in headerBlock.lines()) {
      if ((line.startsWith(" ") || line.startsWith("\t")) && unfoldedLines.isNotEmpty()) {
        val lastIdx = unfoldedLines.lastIndex
        unfoldedLines[lastIdx] = unfoldedLines[lastIdx] + " " + line.trim()
      } else {
        unfoldedLines.add(line)
      }
    }
    val map = mutableMapOf<String, String>()
    for (line in unfoldedLines) {
      val colonIdx = line.indexOf(':')
      if (colonIdx > 0) {
        val key = line.substring(0, colonIdx).trim().lowercase()
        val value = line.substring(colonIdx + 1).trim()
        if (
          key in setOf("subject", "from", "to", "date", "content-type", "content-transfer-encoding")
        ) {
          map.putIfAbsent(key, value)
        }
      }
    }
    return map
  }

  private fun extractMimeBody(
    rawBody: String,
    contentType: String,
    transferEncoding: String,
  ): String {
    val boundaryMatch =
      Regex("""boundary="?([^";\r\n]+)"?""", RegexOption.IGNORE_CASE).find(contentType)
    if (boundaryMatch != null) {
      val boundary = boundaryMatch.groupValues[1]
      val parts = rawBody.split("--$boundary")
      var plainCandidate: String? = null
      var htmlCandidate: String? = null
      for (part in parts) {
        val trimmedPart = part.trim()
        if (trimmedPart.isEmpty() || trimmedPart == "--") continue
        val splitIdx = trimmedPart.indexOf("\n\n")
        if (splitIdx == -1) continue
        val partHeaders = parseHeaders(trimmedPart.substring(0, splitIdx))
        val partType = partHeaders["content-type"].orEmpty().lowercase()
        val partEncoding = partHeaders["content-transfer-encoding"].orEmpty()
        val partContent = trimmedPart.substring(splitIdx + 2)
        if (partType.contains("multipart/")) {
          val nested = extractMimeBody(partContent, partType, partEncoding)
          if (nested.isNotBlank()) return nested
        } else if (partType.contains("text/plain") || partType.isEmpty()) {
          val decoded = decodeTransferEncoding(partContent, partEncoding).trim()
          if (decoded.isNotBlank() && plainCandidate == null) {
            plainCandidate = decoded
          }
        } else if (partType.contains("text/html")) {
          val decoded = stripHtml(decodeTransferEncoding(partContent, partEncoding)).trim()
          if (decoded.isNotBlank() && htmlCandidate == null) {
            htmlCandidate = decoded
          }
        }
      }
      return plainCandidate ?: htmlCandidate ?: stripHtml(rawBody)
    }

    val decoded = decodeTransferEncoding(rawBody, transferEncoding)
    return if (contentType.contains("text/html", ignoreCase = true) || looksLikeHtml(decoded)) {
      stripHtml(decoded)
    } else {
      decoded.trim()
    }
  }

  private fun decodeTransferEncoding(content: String, encoding: String): String {
    return when (encoding.trim().lowercase()) {
      "quoted-printable" -> decodeQuotedPrintable(content)
      "base64" ->
        runCatching {
            val cleanBase64 = content.replace(Regex("""\s+"""), "")
            String(Base64.getDecoder().decode(cleanBase64), StandardCharsets.UTF_8)
          }
          .getOrDefault(content)
      else -> content
    }
  }

  private fun decodeQuotedPrintable(input: String): String {
    val softJoined = input.replace("=\r\n", "").replace("=\n", "")
    val out = ByteArrayOutputStream(softJoined.length)
    var i = 0
    while (i < softJoined.length) {
      val c = softJoined[i]
      if (c == '=' && i + 2 < softJoined.length) {
        val hex = softJoined.substring(i + 1, i + 3)
        val byteVal = hex.toIntOrNull(16)
        if (byteVal != null) {
          out.write(byteVal)
          i += 3
          continue
        }
      }
      out.write(c.code)
      i++
    }
    return String(out.toByteArray(), StandardCharsets.UTF_8)
  }

  private fun stripHtml(html: String): String {
    return html
      .replace(Regex("""(?is)<(script|style|head)\b[^>]*>.*?</\1>"""), " ")
      .replace(Regex("""(?i)<br\s*/?>"""), "\n")
      .replace(Regex("""(?i)</(p|div|tr|li|h[1-6])>"""), "\n")
      .replace(Regex("""<[^>]+>"""), " ")
      .replace("&nbsp;", " ")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .lines()
      .map { it.replace(Regex("""\s+"""), " ").trim() }
      .filter { it.isNotBlank() }
      .joinToString("\n")
  }

  private fun looksLikeEmailHeaders(text: String): Boolean {
    val head = text.take(1000)
    return Regex("""(?m)^(Subject|From|MIME-Version|Content-Type):\s+.+""").containsMatchIn(head)
  }

  private fun looksLikeHtml(text: String): Boolean {
    val head = text.take(1000).lowercase()
    return "<html" in head || "<body" in head || "<div" in head || "<table" in head || "<p>" in head
  }

  private fun looksLikeEmailOrHtml(text: String): Boolean =
    looksLikeEmailHeaders(text) || looksLikeHtml(text)

  private fun queryFilename(uri: Uri): String? {
    if (uri.scheme == "content") {
      try {
        context.contentResolver
          .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
          ?.use { cursor ->
            if (cursor.moveToFirst()) {
              val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (idx != -1) return File(cursor.getString(idx)).name
            }
          }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to resolve filename for content URI $uri", e)
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }

  private fun getMimeTypeFromFilename(filename: String): String {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
      "pdf" -> "application/pdf"
      "jpg",
      "jpeg" -> "image/jpeg"
      "png" -> "image/png"
      "webp" -> "image/webp"
      "txt" -> "text/plain"
      "eml",
      "msg" -> "message/rfc822"
      "html",
      "htm" -> "text/html"
      "json" -> "application/json"
      "md" -> "text/markdown"
      else -> "application/octet-stream"
    }
  }

  private fun isEmailMimeOrExt(mime: String?, ext: String): Boolean {
    val lowerExt = ext.lowercase()
    return mime.equals("message/rfc822", ignoreCase = true) ||
      lowerExt == "eml" ||
      lowerExt == "msg"
  }

  private fun isTextMime(mime: String?): Boolean {
    if (mime == null) return false
    return mime.startsWith("text/") ||
      mime == "application/json" ||
      mime == "application/xml" ||
      mime.equals("message/rfc822", ignoreCase = true)
  }

  private fun detectDocumentType(
    filename: String,
    file: File? = null,
    extractedText: String? = null,
  ): String {
    val combined = "${filename.lowercase()} ${extractedText?.take(600)?.lowercase().orEmpty()}"
    return when {
      "passport" in combined || "passeport" in combined -> "PASSPORT"
      "visa" in combined || "esta" in combined -> "VISA"
      "flight" in combined ||
        "ticket" in combined ||
        "boarding" in combined ||
        "voucher" in combined ||
        "airlines" in combined ||
        "airways" in combined ||
        "train" in combined ||
        "rail" in combined ||
        "eurostar" in combined ||
        "shinkansen" in combined ||
        "amtrak" in combined ||
        "ferry" in combined ||
        "bus" in combined ||
        "coach" in combined ||
        "transit" in combined -> "TICKET"
      "hotel" in combined ||
        "reservation" in combined ||
        "booking" in combined ||
        "check-in" in combined -> "HOTEL"
      "license" in combined || "national_id" in combined || "driver" in combined -> "NATIONAL_ID"
      else -> "OTHER"
    }
  }

  companion object {
    private const val TAG = "TravelDocumentManager"
    private const val MAX_AUTO_NOTES_CHARS = 1000
    private const val MAX_PDF_BYTES = 8L * 1024L * 1024L
  }
}
