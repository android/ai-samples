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
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sun.misc.Unsafe

@OptIn(ExperimentalCoroutinesApi::class)
class ManageExpensesViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var fakeExpenseDao: FakeExpenseDao
  private lateinit var fakeReceiptParser: FakeReceiptParser

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeExpenseDao = FakeExpenseDao()
    fakeReceiptParser = FakeReceiptParser()
  }

  @Test
  fun initialTripId_fromSavedStateHandle_loadsMatchingExpenses() = runTest {
    val initialExpense =
      Expense(
        id = "exp-1",
        title = "Dinner",
        amount = 54.0,
        currency = "USD",
        category = "dining",
        tripId = "trip-initial",
      )
    fakeExpenseDao.insertExpense(initialExpense)

    val savedStateHandle = SavedStateHandle(mapOf("tripId" to "trip-initial"))
    val viewModel = ManageExpensesViewModel(savedStateHandle, fakeReceiptParser, fakeExpenseDao)

    advanceUntilIdle()

    assertEquals("trip-initial", viewModel.tripId)
    val expenses = viewModel.expenses.first()
    assertEquals(1, expenses.size)
    assertEquals("exp-1", expenses.first().id)
  }

  @Test
  fun loadForTrip_updatesTripIdAndFiltersExpenses() = runTest {
    val expenseTripA =
      Expense(
        id = "exp-a",
        title = "Taxi",
        amount = 25.0,
        currency = "USD",
        category = "transportation",
        tripId = "trip-a",
      )
    val expenseTripB =
      Expense(
        id = "exp-b",
        title = "Museum",
        amount = 18.0,
        currency = "EUR",
        category = "activity",
        tripId = "trip-b",
      )
    fakeExpenseDao.insertExpense(expenseTripA)
    fakeExpenseDao.insertExpense(expenseTripB)

    val savedStateHandle = SavedStateHandle(mapOf("tripId" to "trip-a"))
    val viewModel = ManageExpensesViewModel(savedStateHandle, fakeReceiptParser, fakeExpenseDao)

    advanceUntilIdle()
    assertEquals("trip-a", viewModel.tripId)
    assertEquals(listOf("exp-a"), viewModel.expenses.first().map { it.id })

    viewModel.loadForTrip("trip-b")
    advanceUntilIdle()

    assertEquals("trip-b", viewModel.tripId)
    assertEquals(listOf("exp-b"), viewModel.expenses.first().map { it.id })
  }

  @Test
  fun addExpense_withEmptyTripId_assignsCurrentTripId() = runTest {
    val savedStateHandle = SavedStateHandle(mapOf("tripId" to "trip-active"))
    val viewModel = ManageExpensesViewModel(savedStateHandle, fakeReceiptParser, fakeExpenseDao)

    advanceUntilIdle()

    val newExpense =
      Expense(
        id = "exp-new",
        title = "Lunch",
        amount = 12.5,
        currency = "USD",
        category = "dining",
        tripId = "",
      )
    viewModel.addExpense(newExpense)
    advanceUntilIdle()

    assertEquals(1, fakeExpenseDao.expenses.size)
    val saved = fakeExpenseDao.expenses.first()
    assertEquals("trip-active", saved.tripId)
    assertEquals("exp-new", saved.id)
  }

  @Test
  fun addExpense_withExistingTripId_preservesTripId() = runTest {
    val savedStateHandle = SavedStateHandle(mapOf("tripId" to "trip-active"))
    val viewModel = ManageExpensesViewModel(savedStateHandle, fakeReceiptParser, fakeExpenseDao)

    advanceUntilIdle()

    val customExpense =
      Expense(
        id = "exp-custom",
        title = "Flight",
        amount = 350.0,
        currency = "USD",
        category = "transportation",
        tripId = "trip-custom",
      )
    viewModel.addExpense(customExpense)
    advanceUntilIdle()

    assertEquals(1, fakeExpenseDao.expenses.size)
    assertEquals("trip-custom", fakeExpenseDao.expenses.first().tripId)
  }

  @Test
  fun deleteExpense_removesExpenseFromDao() = runTest {
    val expense =
      Expense(
        id = "exp-delete",
        title = "Souvenir",
        amount = 30.0,
        currency = "USD",
        category = "shopping",
        tripId = "trip-1",
      )
    fakeExpenseDao.insertExpense(expense)

    val savedStateHandle = SavedStateHandle(mapOf("tripId" to "trip-1"))
    val viewModel = ManageExpensesViewModel(savedStateHandle, fakeReceiptParser, fakeExpenseDao)

    advanceUntilIdle()
    assertEquals(1, fakeExpenseDao.expenses.size)

    viewModel.deleteExpense(expense)
    advanceUntilIdle()

    assertTrue(fakeExpenseDao.expenses.isEmpty())
  }

  @Test
  fun isSupported_delegatesToReceiptParser() = runTest {
    val savedStateHandle = SavedStateHandle(mapOf("tripId" to "trip-1"))
    val viewModel = ManageExpensesViewModel(savedStateHandle, fakeReceiptParser, fakeExpenseDao)

    fakeReceiptParser.supported = true
    assertTrue(viewModel.isSupported())

    fakeReceiptParser.supported = false
    assertFalse(viewModel.isSupported())
  }

  @Test
  fun parseReceipt_delegatesToReceiptParser() = runTest {
    val savedStateHandle = SavedStateHandle(mapOf("tripId" to "trip-1"))
    val viewModel = ManageExpensesViewModel(savedStateHandle, fakeReceiptParser, fakeExpenseDao)

    val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
    unsafeField.isAccessible = true
    val unsafe = unsafeField.get(null) as Unsafe
    val dummyBitmap = unsafe.allocateInstance(Bitmap::class.java) as Bitmap

    val expectedJson = """{"title":"Coffee","amount":4.50,"currency":"USD","category":"dining"}"""
    fakeReceiptParser.parsedResponse = expectedJson

    val response = viewModel.parseReceipt(dummyBitmap)
    assertEquals(expectedJson, response)
  }

  private class FakeExpenseDao : ExpenseDao {
    val expenses = mutableListOf<Expense>()
    private val _expensesFlow = MutableStateFlow<List<Expense>>(emptyList())

    private fun emit() {
      _expensesFlow.value = expenses.toList()
    }

    override fun getAllExpenses(): Flow<List<Expense>> = _expensesFlow

    override suspend fun insertExpense(expense: Expense) {
      expenses.removeAll { it.id == expense.id }
      expenses.add(expense)
      emit()
    }

    override suspend fun deleteExpense(expense: Expense) {
      expenses.removeAll { it.id == expense.id }
      emit()
    }

    override suspend fun deleteAllExpenses() {
      expenses.clear()
      emit()
    }

    override fun getExpensesForTrip(tripId: String): Flow<List<Expense>> =
      _expensesFlow.map { list -> list.filter { it.tripId == tripId } }
  }

  private class FakeReceiptParser(
    var supported: Boolean = true,
    var parsedResponse: String = "",
  ) : ReceiptParser {
    override suspend fun isSupported(): Boolean = supported

    override suspend fun parseReceipt(bitmap: Bitmap): String = parsedResponse
  }
}
