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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetpacker.data.itinerary.TravelDocument
import com.example.jetpacker.feature.assistant.documents.DocumentAppSearchManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DocumentAppSearchManagerTest {

  private lateinit var context: Context
  private var _manager: DocumentAppSearchManager? = null
  private val manager: DocumentAppSearchManager get() = checkNotNull(_manager)

  @Before
  fun setUp() {
    try {
      System.loadLibrary("icing")
    } catch (t: Throwable) {
      Assume.assumeNoException(
        "Icing native library requires an Android runtime; skipping on host JVM",
        t,
      )
    }
    context = ApplicationProvider.getApplicationContext()
    _manager = DocumentAppSearchManager(context)
  }

  @After fun tearDown() = runTest { _manager?.clearAll() }

  @Test
  fun testIndexAndSearchDocument_byTextPrefix() = runTest {
    val doc =
      TravelDocument(
        id = "doc-passport-1",
        title = "US Passport",
        documentType = "passport",
        issuingCountry = "United States",
        notes = "Issued in 2022, valid for 10 years",
      )

    manager.indexDocument(doc, textContent = "US Passport passport United States Issued in 2022")

    val results = manager.search(query = "Passport", maxResults = 5)
    assertThat(results).contains("doc-passport-1")
  }

  @Test
  fun testRemoveDocument_removesFromIndex() = runTest {
    val doc =
      TravelDocument(
        id = "doc-license-1",
        title = "Driver's License",
        documentType = "driver_license",
        issuingCountry = "Netherlands",
        notes = "Category B",
      )

    manager.indexDocument(
      doc,
      textContent = "Driver's License driver_license Netherlands Category B",
    )

    val beforeResults = manager.search(query = "License", maxResults = 5)
    assertThat(beforeResults).contains("doc-license-1")

    manager.removeDocument("doc-license-1")

    val afterResults = manager.search(query = "License", maxResults = 5)
    assertThat(afterResults).doesNotContain("doc-license-1")
  }

  @Test
  fun testModelSignature_recordsAndRetrieves() = runTest {
    val signature = "embeddinggemma_v2_cpu_float32"
    manager.setStoredModelSignature(signature)

    val stored = manager.getStoredModelSignature()
    assertThat(stored).isEqualTo(signature)
  }

  @Test
  fun testIndexAndSearchDocument_withEmbeddingVector() = runTest {
    val signature = "gemma_embed_v2_test"
    val doc1 =
      TravelDocument(
        id = "doc-tokyo-hotel",
        title = "Tokyo Grand Hotel Reservation",
        documentType = "hotel_reservation",
        issuingCountry = "Japan",
        notes = "Confirmation code 98765, check in at 3pm",
      )
    val doc2 =
      TravelDocument(
        id = "doc-berlin-museum",
        title = "Berlin Pergamon Pass",
        documentType = "museum_ticket",
        issuingCountry = "Germany",
        notes = "Valid on weekends",
      )

    // Vector 1 points along dimension 0, vector 2 points along dimension 1
    val vec1 = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f)
    val vec2 = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f)

    manager.indexDocument(
      doc1,
      textContent = "Tokyo Hotel Reservation",
      vector = vec1,
      modelSignature = signature,
    )
    manager.indexDocument(
      doc2,
      textContent = "Berlin Museum Pass",
      vector = vec2,
      modelSignature = signature,
    )

    // Query vector matches vec1
    val queryVec = floatArrayOf(0.95f, 0.05f, 0.0f, 0.0f)
    val results =
      manager.search(
        query = "hotel in Tokyo",
        queryVector = queryVec,
        modelSignature = signature,
        maxResults = 1,
      )

    assertThat(results).containsExactly("doc-tokyo-hotel")
  }

  @Test
  fun testSearch_scopedToTrip_includesGlobalDocumentsAndExcludesOtherTrips() = runTest {
    val tripDoc =
      TravelDocument(
        id = "doc-paris-hotel",
        title = "Hotel Lumiere Reservation",
        documentType = "hotel_reservation",
        notes = "Wifi network LUMIERE-GUEST",
        tripId = "2026-3",
      )
    val globalDoc =
      TravelDocument(
        id = "doc-global-passport",
        title = "Dutch Passport",
        documentType = "passport",
        notes = "Valid until 2031",
      )
    val otherTripDoc =
      TravelDocument(
        id = "doc-tokyo-hotel-2",
        title = "Tokyo Capsule Reservation",
        documentType = "hotel_reservation",
        notes = "Wifi network TOKYO-GUEST",
        tripId = "2026-9",
      )

    manager.indexDocument(tripDoc, textContent = "Hotel Lumiere Reservation wifi passport")
    manager.indexDocument(globalDoc, textContent = "Dutch Passport wifi passport")
    manager.indexDocument(otherTripDoc, textContent = "Tokyo Capsule Reservation wifi passport")

    val results = manager.search(query = "passport OR wifi", tripId = "2026-3", maxResults = 10)

    assertThat(results).containsAtLeast("doc-paris-hotel", "doc-global-passport")
    assertThat(results).doesNotContain("doc-tokyo-hotel-2")
  }

  @Test
  fun testGetIndexedDocumentIds_returnsEveryIndexedDocument() = runTest {
    manager.indexDocument(
      TravelDocument(id = "doc-a", title = "Alpha", documentType = "note"),
      textContent = "Alpha",
    )
    manager.indexDocument(
      TravelDocument(id = "doc-b", title = "Beta", documentType = "note"),
      textContent = "Beta",
    )

    assertThat(manager.getIndexedDocumentIds()).containsAtLeast("doc-a", "doc-b")

    manager.removeDocument("doc-a")

    assertThat(manager.getIndexedDocumentIds()).doesNotContain("doc-a")
  }
}
