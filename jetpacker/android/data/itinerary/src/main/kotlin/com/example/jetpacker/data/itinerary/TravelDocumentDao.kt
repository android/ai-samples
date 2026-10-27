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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Data Access Object (DAO) for managing travel documents such as passports and visas. */
@Dao
interface TravelDocumentDao {
  @Query("SELECT * FROM travel_documents ORDER BY expiryDate ASC")
  fun getAllDocuments(): Flow<List<TravelDocument>>

  @Query(
    "SELECT * FROM travel_documents WHERE UPPER(documentType) = UPPER(:documentType) ORDER BY expiryDate ASC"
  )
  fun getDocumentsByType(documentType: String): Flow<List<TravelDocument>>

  @Query("SELECT * FROM travel_documents WHERE tripId = :tripId ORDER BY expiryDate ASC")
  fun getDocumentsForTrip(tripId: String): Flow<List<TravelDocument>>

  @Query("SELECT * FROM travel_documents WHERE eventId = :eventId ORDER BY expiryDate ASC")
  fun getDocumentsForEvent(eventId: String): Flow<List<TravelDocument>>

  @Query("SELECT * FROM travel_documents WHERE documentId = :documentId OR id = :documentId LIMIT 1")
  suspend fun getDocumentById(documentId: String): TravelDocument?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDocument(document: TravelDocument)

  @Query(
    "UPDATE travel_documents SET eventId = :eventId, tripId = CASE WHEN :tripId IS NOT NULL THEN :tripId ELSE tripId END WHERE documentId = :documentId OR id = :documentId"
  )
  suspend fun linkDocumentToEvent(documentId: String, eventId: String?, tripId: String? = null)

  @Query(
    "UPDATE travel_documents SET tripId = :tripId WHERE documentId = :documentId OR id = :documentId"
  )
  suspend fun linkDocumentToTrip(documentId: String, tripId: String?)

  @Delete suspend fun deleteDocument(document: TravelDocument)

  @Query("DELETE FROM travel_documents WHERE id = :id") suspend fun deleteDocumentById(id: String)

  @Query("DELETE FROM travel_documents") suspend fun deleteAllDocuments()
}
