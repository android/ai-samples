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

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConverterTest {

  @Test
  fun convertToUsd_knownCurrencies_calculatesExpectedAmount() {
    assertEquals(100.0, CurrencyConverter.convertToUsd(100.0, "USD"), 0.0001)
    assertEquals(108.0, CurrencyConverter.convertToUsd(100.0, "EUR"), 0.0001)
    assertEquals(127.0, CurrencyConverter.convertToUsd(100.0, "GBP"), 0.0001)
    assertEquals(6.7, CurrencyConverter.convertToUsd(1000.0, "JPY"), 0.0001)
    assertEquals(1.2, CurrencyConverter.convertToUsd(100.0, "INR"), 0.0001)
    assertEquals(0.75, CurrencyConverter.convertToUsd(1000.0, "KRW"), 0.0001)
    assertEquals(75.0, CurrencyConverter.convertToUsd(100.0, "SGD"), 0.0001)
    assertEquals(14.0, CurrencyConverter.convertToUsd(100.0, "CNY"), 0.0001)
    assertEquals(2.9, CurrencyConverter.convertToUsd(100.0, "THB"), 0.0001)
    assertEquals(0.4, CurrencyConverter.convertToUsd(10000.0, "VND"), 0.0001)
  }

  @Test
  fun convertToUsd_isCaseInsensitive() {
    assertEquals(108.0, CurrencyConverter.convertToUsd(100.0, "eur"), 0.0001)
    assertEquals(127.0, CurrencyConverter.convertToUsd(100.0, "Gbp"), 0.0001)
  }

  @Test
  fun convertToUsd_unknownCurrency_defaultsToOneToOneRate() {
    assertEquals(42.0, CurrencyConverter.convertToUsd(42.0, "XYZ"), 0.0001)
  }

  @Test
  fun getSymbol_knownCurrencies_returnsExpectedSymbol() {
    assertEquals("$", CurrencyConverter.getSymbol("USD"))
    assertEquals("€", CurrencyConverter.getSymbol("EUR"))
    assertEquals("£", CurrencyConverter.getSymbol("GBP"))
    assertEquals("¥", CurrencyConverter.getSymbol("JPY"))
    assertEquals("₹", CurrencyConverter.getSymbol("INR"))
    assertEquals("₩", CurrencyConverter.getSymbol("KRW"))
    assertEquals("S$", CurrencyConverter.getSymbol("SGD"))
    assertEquals("¥", CurrencyConverter.getSymbol("CNY"))
    assertEquals("฿", CurrencyConverter.getSymbol("THB"))
    assertEquals("₫", CurrencyConverter.getSymbol("VND"))
  }

  @Test
  fun getSymbol_isCaseInsensitive() {
    assertEquals("€", CurrencyConverter.getSymbol("eur"))
    assertEquals("£", CurrencyConverter.getSymbol("Gbp"))
  }

  @Test
  fun getSymbol_unknownCurrency_defaultsToDollarSign() {
    assertEquals("$", CurrencyConverter.getSymbol("UNKNOWN"))
  }
}
