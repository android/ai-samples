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

import android.util.Log
import com.example.jetpacker.data.itinerary.DocumentSemanticSearchProvider
import com.example.jetpacker.data.itinerary.TravelDocument
import com.example.jetpacker.data.itinerary.TravelDocumentDao
import com.example.jetpacker.data.itinerary.TravelDocumentManager
import com.google.mlkit.genai.embedding.Content
import com.google.mlkit.genai.embedding.EmbeddingGeneration
import com.google.mlkit.genai.embedding.EmbeddingModel
import com.google.mlkit.genai.embedding.EmbeddingRequest
import com.google.mlkit.genai.embedding.ImagePart
import com.google.mlkit.genai.embedding.Modality
import com.google.mlkit.genai.embedding.Part
import com.google.mlkit.genai.embedding.TaskConfig
import com.google.mlkit.genai.embedding.TextPart
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Coordinates on-device semantic document search by bridging the ML Kit Embedding API (Embedding
 * Gemma v2) with Jetpack AppSearch native vector indexing and retrieval.
 *
 * Implements Google's canonical on-device RAG architecture:
 * 1. Room serves as the relational source of truth.
 * 2. Jetpack AppSearch provides hardware-accelerated hybrid search (BM25 + cosine vector
 *    similarity).
 * 3. ML Kit Embedding API generates high-dimensional embeddings on-device.
 */
@Singleton
class DocumentSemanticSearch
@Inject
constructor(
  private val travelDocumentDao: TravelDocumentDao,
  private val travelDocumentManager: TravelDocumentManager,
  private val appSearchManager: DocumentAppSearchManager? = null,
) : DocumentSemanticSearchProvider {

  /**
   * Vector cache keyed by the document's embedding text, so that re-indexing an unchanged document
   * never re-runs the embedding model.
   */
  private val embeddingCache = ConcurrentHashMap<String, FloatArray>()

  /**
   * Content hash of the payload most recently written to AppSearch, keyed by document ID. This
   * replaces a one-shot "synced" latch so that documents added after the first query (for example,
   * a document scanned live on stage) are picked up immediately.
   */
  private val indexedContentHashes = ConcurrentHashMap<String, Int>()

  private val indexMutex = Mutex()

  @Volatile private var cachedModel: EmbeddingModel? = null
  @Volatile private var modelChecked = false
  @Volatile private var signatureVerified = false

  @androidx.annotation.VisibleForTesting
  internal fun setEmbeddingModelForTesting(model: EmbeddingModel?) {
    cachedModel = model
    modelChecked = true
    signatureVerified = false
  }

  @androidx.annotation.VisibleForTesting
  internal fun isDocumentHashIndexed(docId: String): Boolean =
    indexedContentHashes.containsKey(docId)

  init {
    travelDocumentManager.semanticSearchProvider = this
    Log.i(
      TAG,
      if (appSearchManager != null) {
        "Vault retrieval: AppSearch hybrid index enabled"
      } else {
        "Vault retrieval: no AppSearch index, falling back to in-memory scoring"
      },
    )
  }

  private suspend fun getEmbeddingModel(): EmbeddingModel? {
    if (modelChecked) return cachedModel
    return try {
      val models = EmbeddingGeneration.getEmbeddingModels()
      val model = models.firstOrNull()
      cachedModel = model
      modelChecked = true
      model
    } catch (e: Throwable) {
      Log.w(TAG, "Embedding model not available on device", e)
      modelChecked = true
      null
    }
  }

  private suspend fun getModelSignature(model: EmbeddingModel?): String? = model?.let {
    runCatching { it.getModelSignature() }.getOrNull()
  }

  /**
   * Finds the most relevant travel documents for a natural language query.
   *
   * Scopes search to the specified [tripId] plus global documents (where [TravelDocument.tripId] is
   * null). Queries Jetpack AppSearch using native hybrid semantic search when available; falls back
   * to token and metadata similarity scoring if AppSearch or ML models are unavailable.
   *
   * @param query Natural language user query (e.g. "passport", "flight ticket to Tokyo", "health
   *   insurance").
   * @param tripId Optional trip ID to restrict results to.
   * @param maxResults Maximum number of documents to return.
   */
  override suspend fun findRelevantDocuments(
    query: String,
    tripId: String?,
    maxResults: Int,
  ): List<TravelDocument> =
    withContext(Dispatchers.IO) {
      val trimmedQuery = query.trim()
      if (trimmedQuery.isBlank()) return@withContext emptyList()

      val allDocs = travelDocumentDao.getAllDocuments().first()
      if (allDocs.isEmpty()) return@withContext emptyList()

      // Filter to documents in scope: matching tripId OR global documents (tripId == null)
      val scopedDocs =
        if (!tripId.isNullOrBlank()) {
          allDocs.filter { it.tripId == null || it.tripId == tripId }
        } else {
          allDocs
        }

      if (scopedDocs.isEmpty()) return@withContext emptyList()

      val model = getEmbeddingModel()
      val signature = getModelSignature(model)

      // Primary retrieval engine: Jetpack AppSearch hybrid search
      if (appSearchManager != null) {
        try {
          syncIndex(allDocs, model, signature)

          val queryVector =
            if (model != null) {
              val queryReq =
                EmbeddingRequest(
                  content = Content(listOf(TextPart(trimmedQuery))),
                  taskConfig = TaskConfig.RetrievalQuery(),
                )
              model.generateEmbedding(queryReq).embedding.vector
            } else {
              null
            }

          val matchedIds =
            appSearchManager.search(
              query = trimmedQuery,
              queryVector = queryVector,
              modelSignature = signature,
              tripId = tripId,
              maxResults = maxResults,
            )

          if (matchedIds.isNotEmpty()) {
            val scopedMap = scopedDocs.associateBy { it.id }
            val matchedDocs = matchedIds.mapNotNull { scopedMap[it] }
            if (matchedDocs.isNotEmpty()) {
              return@withContext matchedDocs.take(maxResults)
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "AppSearch hybrid query failed; falling back to in-memory/keyword search", e)
        }
      }

      // Secondary fallback: in-memory cosine loop if model is present, otherwise keyword scoring
      if (model != null) {
        try {
          return@withContext searchWithEmbeddings(model, trimmedQuery, scopedDocs, maxResults)
        } catch (e: Exception) {
          Log.w(TAG, "Embedding search failed; falling back to keyword search", e)
        }
      }

      searchWithKeywords(trimmedQuery, scopedDocs, maxResults)
    }

  /**
   * Ensures the AppSearch index reflects the current contents of Room.
   *
   * Safe to call repeatedly: unchanged documents are skipped, and documents that no longer exist in
   * Room are pruned from the index.
   */
  suspend fun ensureIndexUpToDate() =
    withContext(Dispatchers.IO) {
      if (appSearchManager == null) return@withContext
      val docs = travelDocumentDao.getAllDocuments().first()
      val model = getEmbeddingModel()
      syncIndex(docs, model, getModelSignature(model))
    }

  override suspend fun clearIndex() {
    withContext(Dispatchers.IO) {
      indexMutex.withLock {
        appSearchManager?.clearAll()
        indexedContentHashes.clear()
        embeddingCache.clear()
      }
    }
  }

  override suspend fun syncIndex() {
    ensureIndexUpToDate()
  }

  /** Removes a single document from the AppSearch index, for example after it is deleted. */
  suspend fun removeFromIndex(docId: String) =
    withContext(Dispatchers.IO) {
      val manager = appSearchManager ?: return@withContext
      indexMutex.withLock {
        manager.removeDocument(docId)
        indexedContentHashes.remove(docId)
      }
      Unit
    }

  /**
   * Incrementally syncs Room documents into AppSearch, generating embeddings and validating the
   * embedding model signature.
   */
  private suspend fun syncIndex(
    docs: List<TravelDocument>,
    model: EmbeddingModel?,
    signature: String?,
  ) {
    val manager = appSearchManager ?: return

    indexMutex.withLock {
      if (!signatureVerified && !signature.isNullOrBlank()) {
        verifyModelSignature(manager, signature)
        signatureVerified = true
      }

      for (doc in docs) {
        val text = buildDocumentEmbeddingText(doc)
        val contentHash = contentHashOf(doc, text)
        if (indexedContentHashes[doc.id] == contentHash) continue

        val vector =
          if (model != null && !signature.isNullOrBlank()) {
            embedDocument(doc, model, text)
          } else {
            null
          }
        manager.indexDocument(doc, text, vector, signature)
        if (vector != null) {
          indexedContentHashes[doc.id] = contentHash
        }
      }

      pruneDeletedDocuments(manager, docs.mapTo(mutableSetOf()) { it.id })
    }
  }

  /**
   * Drops AppSearch entries whose Room rows are gone, so a deleted document can never resurface as
   * a search hit.
   */
  private suspend fun pruneDeletedDocuments(
    manager: DocumentAppSearchManager,
    liveIds: Set<String>,
  ) {
    val indexedIds = runCatching { manager.getIndexedDocumentIds() }.getOrNull() ?: return
    for (staleId in indexedIds - liveIds) {
      manager.removeDocument(staleId)
      indexedContentHashes.remove(staleId)
    }
  }

  /**
   * Clears the index when the embedding model changed, because vectors produced by different models
   * are not comparable, then records the current signature.
   */
  private suspend fun verifyModelSignature(manager: DocumentAppSearchManager, signature: String?) {
    val storedSignature = manager.getStoredModelSignature()
    if (signature != null && storedSignature != null && storedSignature != signature) {
      Log.i(
        TAG,
        "Embedding model signature changed ($storedSignature -> $signature); clearing stale index",
      )
      manager.clearAll()
      indexedContentHashes.clear()
      embeddingCache.clear()
    }
    if (signature != null && storedSignature != signature) {
      manager.setStoredModelSignature(signature)
    }
  }

  /** Includes file path, MIME type, and trip/event linkage so changes force a re-index. */
  private fun contentHashOf(doc: TravelDocument, text: String): Int =
    "$text|${doc.filePath}|${doc.mimeType}|${doc.tripId.orEmpty()}|${doc.eventId.orEmpty()}"
      .hashCode()

  /** Returns the cached vector for [doc], generating and caching it on first use. */
  private suspend fun embedDocument(
    doc: TravelDocument,
    model: EmbeddingModel,
    text: String,
  ): FloatArray? {
    val cacheKey = "$text|${doc.filePath}"
    embeddingCache[cacheKey]?.let { cached ->
      return cached.takeIf { it.isNotEmpty() }
    }
    val vector = generateDocumentEmbedding(doc, model)
    embeddingCache[cacheKey] = vector ?: FloatArray(0)
    return vector
  }

  private suspend fun supportsImageModality(model: EmbeddingModel): Boolean =
    runCatching { model.getMetadata()?.supportedModalities?.contains(Modality.IMAGE) == true }
      .getOrDefault(false)

  /**
   * Generates a vector embedding for a travel document using the ML Kit GenAI Embedding API.
   *
   * When the document is backed by an image or PDF and the active [EmbeddingModel] supports
   * [Modality.IMAGE] (Embedding Gemma v2), passes both [TextPart] and [ImagePart] (decoded image or
   * rendered PDF page 0) so visual content and layout are embedded directly alongside text. Falls
   * back gracefully to text-only embedding if multimodal inference is unsupported or fails.
   */
  suspend fun generateDocumentEmbedding(doc: TravelDocument, model: EmbeddingModel): FloatArray? {
    val docText = buildDocumentEmbeddingText(doc)
    val taskConfig = TaskConfig.RetrievalDocument(title = doc.title.takeIf { it.isNotBlank() })

    if (travelDocumentManager.isVisualDocument(doc) && supportsImageModality(model)) {
      val bitmap = travelDocumentManager.loadDocumentBitmap(doc, maxDimension = 768)
      if (bitmap != null) {
        try {
          return ImagePart(bitmap).use { imagePart ->
            val multimodalReq =
              EmbeddingRequest(
                content = Content(listOf<Part>(TextPart(docText), imagePart)),
                taskConfig = taskConfig,
              )
            model.generateEmbedding(multimodalReq).embedding.vector
          }
        } catch (e: Exception) {
          Log.w(
            TAG,
            "Multimodal ImagePart embedding failed for ${doc.id}; falling back to text-only",
            e,
          )
        }
      }
    }

    val textReq =
      EmbeddingRequest(content = Content(listOf(TextPart(docText))), taskConfig = taskConfig)
    return try {
      val docRes = model.generateEmbedding(textReq)
      docRes.embedding.vector
    } catch (e: Exception) {
      Log.w(TAG, "Failed to generate embedding for doc ${doc.id}", e)
      null
    }
  }

  /**
   * Indexes or updates a single document in the AppSearch index.
   *
   * Call this immediately after a document is scanned, created, imported or re-linked so that it is
   * retrievable without waiting for the next full sync.
   */
  suspend fun indexDocument(doc: TravelDocument) =
    withContext(Dispatchers.IO) {
      val manager = appSearchManager ?: return@withContext
      val text = buildDocumentEmbeddingText(doc)
      val model = getEmbeddingModel()
      val signature = getModelSignature(model)
      indexMutex.withLock {
        val vector =
          if (model != null && !signature.isNullOrBlank()) embedDocument(doc, model, text) else null
        manager.indexDocument(doc, text, vector, signature)
        if (vector != null) {
          indexedContentHashes[doc.id] = contentHashOf(doc, text)
        }
      }
      Unit
    }

  private suspend fun searchWithEmbeddings(
    model: EmbeddingModel,
    query: String,
    docs: List<TravelDocument>,
    maxResults: Int,
  ): List<TravelDocument> {
    val queryReq =
      EmbeddingRequest(
        content = Content(listOf(TextPart(query))),
        taskConfig = TaskConfig.RetrievalQuery(),
      )
    val queryRes = model.generateEmbedding(queryReq)
    val queryVector = queryRes.embedding.vector

    val scored = mutableListOf<Pair<TravelDocument, Float>>()

    for (doc in docs) {
      val docVector = embedDocument(doc, model, buildDocumentEmbeddingText(doc)) ?: continue
      if (docVector.isNotEmpty()) {
        val similarity = cosineSimilarity(queryVector, docVector)
        scored.add(doc to similarity)
      }
    }

    return scored.sortedByDescending { it.second }.take(maxResults).map { it.first }
  }

  private fun searchWithKeywords(
    query: String,
    docs: List<TravelDocument>,
    maxResults: Int,
  ): List<TravelDocument> {
    val queryTerms = query.lowercase().split(Regex("""\s+""")).filter { it.length >= 2 }
    if (queryTerms.isEmpty()) return docs.take(maxResults)

    val scored = docs.map { doc ->
      val searchable = buildDocumentEmbeddingText(doc).lowercase()
      var score = 0
      for (term in queryTerms) {
        if (doc.documentType.lowercase() == term) score += 5
        if (doc.title.lowercase().contains(term)) score += 3
        if (searchable.contains(term)) score += 1
      }
      doc to score
    }

    return scored
      .filter { it.second > 0 }
      .sortedByDescending { it.second }
      .take(maxResults)
      .map { it.first }
      .ifEmpty { docs.take(maxResults) }
  }

  internal fun buildDocumentEmbeddingText(doc: TravelDocument): String =
    travelDocumentManager.readDocumentEmbeddingText(doc)

  private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
    var dot = 0f
    var normA = 0f
    var normB = 0f
    for (i in a.indices) {
      dot += a[i] * b[i]
      normA += a[i] * a[i]
      normB += b[i] * b[i]
    }
    val denom = Math.sqrt((normA * normB).toDouble()).toFloat()
    return if (denom > 0f) dot / denom else 0f
  }

  companion object {
    private const val TAG = "DocSemanticSearch"
  }
}
