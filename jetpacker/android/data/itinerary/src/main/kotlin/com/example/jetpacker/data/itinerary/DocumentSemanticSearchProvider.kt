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

package com.example.jetpacker.data.itinerary

/**
 * Provider interface for on-device semantic document search using ML Kit Embedding API
 * (Embedding Gemma v2) and Jetpack AppSearch hybrid vector retrieval.
 */
interface DocumentSemanticSearchProvider {
  /**
   * Finds travel documents semantically relevant to [query].
   *
   * @param query The natural language user query.
   * @param tripId Optional trip ID filter.
   * @param maxResults Maximum number of documents to return.
   */
  suspend fun findRelevantDocuments(
    query: String,
    tripId: String? = null,
    maxResults: Int = 10,
  ): List<TravelDocument>

  /** Clears any cached search indexes, vectors, or metadata. */
  suspend fun clearIndex() {}

  /** Synchronizes the search index with current stored documents. */
  suspend fun syncIndex() {}
}
