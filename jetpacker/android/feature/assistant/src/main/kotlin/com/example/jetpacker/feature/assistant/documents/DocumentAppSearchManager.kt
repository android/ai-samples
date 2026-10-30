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

import android.content.Context
import android.util.Log
import androidx.appsearch.app.AppSearchSchema
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.EmbeddingVector
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.GetByDocumentIdRequest
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.RemoveByDocumentIdRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.localstorage.LocalStorage
import androidx.concurrent.futures.await
import com.example.jetpacker.data.itinerary.TravelDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the on-device Jetpack AppSearch index for travel documents.
 *
 * Provides high-performance, native hybrid search combining:
 * 1. BM25 text prefix matching on title, document type, and notes.
 * 2. Hardware-optimized vector similarity (cosine / dot-product) using [EmbeddingVector] properties
 *    with 8-bit quantization.
 * 3. Model signature validation to cleanly handle on-device embedding model upgrades (e.g. v1 to
 *    v2).
 */
@Singleton
class DocumentAppSearchManager
@Inject
constructor(@ApplicationContext private val context: Context) {
  private val sessionMutex = Mutex()
  @Volatile private var session: AppSearchSession? = null
  @Volatile private var isInitialized = false

  suspend fun getSession(): AppSearchSession? {
    if (isInitialized && session != null) return session
    return sessionMutex.withLock {
      if (isInitialized && session != null) return@withLock session
      try {
        val searchContext = LocalStorage.SearchContext.Builder(context, DATABASE_NAME).build()
        val s = LocalStorage.createSearchSessionAsync(searchContext).await()
        setSchemas(s)
        session = s
        isInitialized = true
        s
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e(TAG, "Failed to initialize AppSearch session", e)
        null
      }
    }
  }

  private suspend fun setSchemas(s: AppSearchSession) {
    val documentSchema =
      AppSearchSchema.Builder(SCHEMA_TYPE_DOCUMENT)
        .addProperty(
          AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_TITLE)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .build()
        )
        .addProperty(
          AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_DOCUMENT_TYPE)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .build()
        )
        .addProperty(
          AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_TEXT_CONTENT)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .build()
        )
        .addProperty(
          AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_TRIP_ID)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
            // Verbatim keeps IDs like "2026-3" as a single token so trip scoping is exact
            // instead of matching the sub-tokens "2026" and "3".
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
            .build()
        )
        .addProperty(
          AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_ISSUING_COUNTRY)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .build()
        )
        .addProperty(
          AppSearchSchema.EmbeddingPropertyConfig.Builder(PROPERTY_EMBEDDING)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_8_BIT)
            .build()
        )
        .build()

    val metadataSchema =
      AppSearchSchema.Builder(SCHEMA_TYPE_METADATA)
        .addProperty(
          AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_MODEL_SIGNATURE)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .build()
        )
        .build()

    val request =
      SetSchemaRequest.Builder()
        .addSchemas(documentSchema, metadataSchema)
        .setForceOverride(true)
        .build()
    s.setSchemaAsync(request).await()
  }

  /**
   * Retrieves the stored model signature from the AppSearch metadata document, or null if no model
   * signature has been recorded.
   */
  suspend fun getStoredModelSignature(): String? {
    val s = getSession() ?: return null
    val req = GetByDocumentIdRequest.Builder(NAMESPACE).addIds(METADATA_ID).build()
    return try {
      val res = s.getByDocumentIdAsync(req).await()
      res.successes[METADATA_ID]?.getPropertyString(PROPERTY_MODEL_SIGNATURE)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      null
    }
  }

  /** Records the active embedding model signature in AppSearch metadata. */
  suspend fun setStoredModelSignature(signature: String) {
    val s = getSession() ?: return
    val doc =
      GenericDocument.Builder<GenericDocument.Builder<*>>(
          NAMESPACE,
          METADATA_ID,
          SCHEMA_TYPE_METADATA,
        )
        .setPropertyString(PROPERTY_MODEL_SIGNATURE, signature)
        .build()
    val req = PutDocumentsRequest.Builder().addGenericDocuments(doc).build()
    try {
      s.putAsync(req).await()
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.e(TAG, "Failed to write metadata signature", e)
    }
  }

  /** Indexes a [TravelDocument] into AppSearch with optional vector embedding. */
  suspend fun indexDocument(
    doc: TravelDocument,
    textContent: String,
    vector: FloatArray? = null,
    modelSignature: String? = null,
  ) {
    val s = getSession() ?: return
    val builder =
      GenericDocument.Builder<GenericDocument.Builder<*>>(NAMESPACE, doc.id, SCHEMA_TYPE_DOCUMENT)
        .setPropertyString(PROPERTY_TITLE, doc.title)
        .setPropertyString(PROPERTY_DOCUMENT_TYPE, doc.documentType)
        .setPropertyString(PROPERTY_TEXT_CONTENT, textContent)
        .setPropertyString(
          PROPERTY_TRIP_ID,
          doc.tripId?.takeIf { it.isNotBlank() } ?: GLOBAL_TRIP_TOKEN,
        )
        .setPropertyString(PROPERTY_ISSUING_COUNTRY, doc.issuingCountry)

    if (vector != null && vector.isNotEmpty() && !modelSignature.isNullOrBlank()) {
      builder.setPropertyEmbedding(PROPERTY_EMBEDDING, EmbeddingVector(vector, modelSignature))
    }

    val req = PutDocumentsRequest.Builder().addGenericDocuments(builder.build()).build()
    try {
      val res = s.putAsync(req).await()
      if (res.failures.isNotEmpty()) {
        Log.w(TAG, "AppSearch put failures: ${res.failures}")
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.e(TAG, "Failed to index document ${doc.id}", e)
    }
  }

  /**
   * Performs hybrid semantic vector and full-text search against indexed travel documents.
   *
   * @param query The natural language user query.
   * @param queryVector The query embedding float array, if available.
   * @param modelSignature The signature of the embedding model that produced [queryVector].
   * @param tripId Optional trip ID to restrict matches to. Documents belonging to the trip and
   *   global documents (those with no trip) are both returned.
   * @param maxResults Maximum number of matched document IDs to return.
   * @return List of matching [TravelDocument.id] strings in descending score order.
   */
  suspend fun search(
    query: String,
    queryVector: FloatArray? = null,
    modelSignature: String? = null,
    tripId: String? = null,
    maxResults: Int = 5,
  ): List<String> {
    val s = getSession() ?: return emptyList()

    val useVectors =
      queryVector != null && queryVector.isNotEmpty() && !modelSignature.isNullOrBlank()
    val sanitizedQuery = query.replace(Regex("""[^\w\s]"""), " ").trim()

    val matchQuery: String =
      if (useVectors) {
        if (sanitizedQuery.isBlank()) {
          "semanticSearch(getEmbeddingParameter(0), $SEMANTIC_SIMILARITY_FLOOR)"
        } else {
          "(($sanitizedQuery) OR semanticSearch(getEmbeddingParameter(0), $SEMANTIC_SIMILARITY_FLOOR))"
        }
      } else {
        sanitizedQuery.ifBlank {
          return emptyList()
        }
      }

    val tripScope = buildTripScopeClause(tripId)

    // Primary attempt: scope the query to the active trip plus global documents.
    if (tripScope != null) {
      val scoped =
        runSearch(
          s,
          "$tripScope AND $matchQuery",
          queryVector,
          modelSignature,
          useVectors,
          maxResults,
        )
      if (scoped != null) return scoped
      Log.w(TAG, "Trip-scoped AppSearch query failed; retrying without the trip restrict")
    }

    return runSearch(s, matchQuery, queryVector, modelSignature, useVectors, maxResults)
      ?: emptyList()
  }

  /**
   * Builds an AppSearch property restrict clause matching the trip plus global documents, or null
   * when no trip scoping is requested or the ID is not safely expressible.
   */
  private fun buildTripScopeClause(tripId: String?): String? {
    if (tripId.isNullOrBlank()) return null
    // Verbatim terms are quoted; reject IDs containing quotes/backslashes rather than risk
    // producing a malformed query that AppSearch would reject at parse time.
    if (tripId.any { it == '"' || it == '\\' }) return null
    return """($PROPERTY_TRIP_ID:"$tripId" OR $PROPERTY_TRIP_ID:"$GLOBAL_TRIP_TOKEN")"""
  }

  /**
   * Executes a single AppSearch query, returning null if the query itself could not be run so the
   * caller can fall back to a simpler form.
   */
  private suspend fun runSearch(
    s: AppSearchSession,
    searchQuery: String,
    queryVector: FloatArray?,
    modelSignature: String?,
    useVectors: Boolean,
    maxResults: Int,
  ): List<String>? {
    val specBuilder = SearchSpec.Builder().addFilterSchemas(SCHEMA_TYPE_DOCUMENT)
    specBuilder.setResultCountPerPage(maxResults)
    specBuilder.setListFilterQueryLanguageEnabled(true)
    // The trip restrict quotes the trip ID as a verbatim term; without this the quoted term is
    // not honoured and the restrict silently matches nothing useful.
    specBuilder.setVerbatimSearchEnabled(true)

    if (useVectors && queryVector != null && !modelSignature.isNullOrBlank()) {
      specBuilder.setDefaultEmbeddingSearchMetricType(
        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_COSINE
      )
      specBuilder.addEmbeddingParameters(EmbeddingVector(queryVector, modelSignature))
      // Hybrid ranking: cosine similarity + text relevance
      specBuilder.setRankingStrategy(
        "sum(this.matchedSemanticScores(getEmbeddingParameter(0))) + 0.3 * this.relevanceScore()"
      )
    }

    val searchResults =
      try {
        s.search(searchQuery, specBuilder.build())
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "AppSearch search failed for query: $searchQuery", e)
        return null
      }

    return try {
      val page = searchResults.nextPageAsync.await()
      page.map { it.genericDocument.id }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.w(TAG, "AppSearch results iteration failed", e)
      null
    } finally {
      searchResults.close()
    }
  }

  /**
   * Returns the IDs of every document currently held in the AppSearch index.
   *
   * Used to prune entries whose backing Room rows have been deleted.
   */
  suspend fun getIndexedDocumentIds(limit: Int = MAX_INDEXED_IDS): Set<String> {
    val s = getSession() ?: return emptySet()
    val spec =
      SearchSpec.Builder()
        .addFilterSchemas(SCHEMA_TYPE_DOCUMENT)
        .setResultCountPerPage(limit)
        .build()
    // An empty query string matches every document of the filtered schema.
    val searchResults =
      try {
        s.search("", spec)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "Failed to enumerate indexed documents", e)
        return emptySet()
      }

    return try {
      searchResults.nextPageAsync.await().map { it.genericDocument.id }.toSet()
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.w(TAG, "Failed to read indexed document page", e)
      emptySet()
    } finally {
      searchResults.close()
    }
  }

  /** Removes a document from the AppSearch index by document ID. */
  suspend fun removeDocument(id: String) {
    val s = getSession() ?: return
    val req = RemoveByDocumentIdRequest.Builder(NAMESPACE).addIds(id).build()
    try {
      s.removeAsync(req).await()
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.e(TAG, "Failed to remove document $id from AppSearch", e)
    }
  }

  /** Clears all indexed documents and metadata from the AppSearch index. */
  suspend fun clearAll() {
    val s = getSession() ?: return
    val spec =
      SearchSpec.Builder().addFilterSchemas(SCHEMA_TYPE_DOCUMENT, SCHEMA_TYPE_METADATA).build()
    try {
      s.removeAsync("", spec).await()
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.e(TAG, "Failed to clear AppSearch index", e)
    }
  }

  companion object {
    private const val TAG = "DocAppSearchManager"
    const val DATABASE_NAME = "jetpacker_documents"
    const val NAMESPACE = "travel_documents"
    const val METADATA_ID = "db_metadata"

    const val SCHEMA_TYPE_DOCUMENT = "TravelDocument"
    const val SCHEMA_TYPE_METADATA = "DbMetadata"

    const val PROPERTY_TITLE = "title"
    const val PROPERTY_DOCUMENT_TYPE = "documentType"
    const val PROPERTY_TEXT_CONTENT = "textContent"
    const val PROPERTY_TRIP_ID = "tripId"
    const val PROPERTY_ISSUING_COUNTRY = "issuingCountry"
    const val PROPERTY_EMBEDDING = "embedding"
    const val PROPERTY_MODEL_SIGNATURE = "modelSignature"

    /**
     * Sentinel trip token written for documents that are not tied to a specific trip, so that
     * trip-scoped queries can include them with a single property restrict.
     */
    const val GLOBAL_TRIP_TOKEN = "global"

    /** Minimum cosine similarity a document must reach to be considered a semantic match. */
    const val SEMANTIC_SIMILARITY_FLOOR = "0.15"

    /**
     * Upper bound on IDs returned by [getIndexedDocumentIds]; the vault is a small local corpus.
     */
    const val MAX_INDEXED_IDS = 500
  }
}
