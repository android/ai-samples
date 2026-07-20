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

package com.example.jetpacker.feature.expenses

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@HiltViewModel
class ManageExpensesViewModel
@Inject
constructor(
  private val savedStateHandle: SavedStateHandle,
  private val receiptParser: ReceiptParser,
  private val expenseDao: ExpenseDao,
) : ViewModel() {

  private val _tripIdFlow = MutableStateFlow(savedStateHandle.get<String>("tripId") ?: "")
  internal val tripId: String get() = _tripIdFlow.value

  fun loadForTrip(id: String) {
    if (id.isNotEmpty() && id != _tripIdFlow.value) {
      _tripIdFlow.value = id
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val expenses: Flow<List<Expense>> = _tripIdFlow.flatMapLatest { id -> expenseDao.getExpensesForTrip(id) }

  suspend fun isSupported(): Boolean {
    return receiptParser.isSupported()
  }

  suspend fun parseReceipt(bitmap: Bitmap): String {
    return receiptParser.parseReceipt(bitmap)
  }

  fun addExpense(expense: Expense) {
    viewModelScope.launch {
      val expenseWithTripId =
        if (expense.tripId.isEmpty()) expense.copy(tripId = tripId) else expense
      expenseDao.insertExpense(expenseWithTripId)
    }
  }

  fun deleteExpense(expense: Expense) {
    viewModelScope.launch { expenseDao.deleteExpense(expense) }
  }
}
