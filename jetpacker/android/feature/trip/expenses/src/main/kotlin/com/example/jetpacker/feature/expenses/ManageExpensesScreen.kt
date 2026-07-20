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

import com.example.jetpacker.core.ui.components.JetPackerFabConfig
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.ModeOfTravel
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.toPath
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.ui.EventColors
import com.example.jetpacker.core.ui.SekuyaFontFamily
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.trips.DummyData
import kotlinx.coroutines.launch
import org.json.JSONObject

object CurrencyConverter {
  // Hardcoded rates for offline usage (Realistically these would be updated periodically)
  private val rates =
    mapOf(
      "USD" to 1.0,
      "INR" to 0.012, // 1 INR = 0.012 USD
      "EUR" to 1.08, // 1 EUR = 1.08 USD
      "GBP" to 1.27, // 1 GBP = 1.27 USD
      "JPY" to 0.0067, // 1 JPY = 0.0067 USD
      "KRW" to 0.00075, // 1 KRW = 0.00075 USD
      "SGD" to 0.75, // 1 SGD = 0.75 USD
      "CNY" to 0.14, // 1 CNY = 0.14 USD
      "THB" to 0.029, // 1 THB = 0.029 USD
      "VND" to 0.00004, // 1 VND = 0.00004 USD
    )

  fun convertToUsd(amount: Double, currency: String): Double {
    val rate = rates[currency.uppercase()] ?: 1.0
    return amount * rate
  }

  fun getSymbol(currency: String): String {
    return when (currency.uppercase()) {
      "USD" -> "$"
      "INR" -> "₹"
      "EUR" -> "€"
      "GBP" -> "£"
      "JPY" -> "¥"
      "KRW" -> "₩"
      "SGD" -> "S$"
      "CNY" -> "¥"
      "THB" -> "฿"
      "VND" -> "₫"
      else -> "$"
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManageExpensesScreen(
  modifier: Modifier = Modifier,
  tripId: String = "",
  contentPadding: PaddingValues,
  onBack: () -> Unit = {},
  onFabConfigChange: (JetPackerFabConfig?) -> Unit = {},
  viewModel: ManageExpensesViewModel = hiltViewModel(),
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  val expenses by viewModel.expenses.collectAsStateWithLifecycle(initialValue = emptyList())

  LaunchedEffect(tripId) {
    if (tripId.isNotEmpty()) {
      viewModel.loadForTrip(tripId)
    }
  }

  var budget by remember { mutableStateOf(2000.00) }
  var isBudgetSheetVisible by remember { mutableStateOf(false) }

  var selectedExpense by remember { mutableStateOf<Expense?>(null) }
  var isParsingError by remember { mutableStateOf(false) }
  var isDetailSheetVisible by remember { mutableStateOf(false) }

  var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

  val totalSpent = expenses.sumOf { CurrencyConverter.convertToUsd(it.amount, it.currency) }

  val isProcessing = remember { mutableStateOf(false) }

  val cameraLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) {
      bitmap ->
      if (bitmap != null) {
        isProcessing.value = true
        coroutineScope.launch {
          try {
            val fullResponse = viewModel.parseReceipt(bitmap)
            Log.d("ManageExpensesScreen", "Receipt parsed successfully")
            if (fullResponse.isNotEmpty() && !fullResponse.contains("false", ignoreCase = true)) {
              val jsonString = fullResponse.substringAfter("```json").substringBefore("```").trim()
              val json = JSONObject(if (jsonString.startsWith("{")) jsonString else fullResponse)
              val title = json.getString("title")
              val amount = json.getDouble("amount")
              val currency = json.getString("currency").uppercase()
              val category = json.getString("category").lowercase()
              viewModel.addExpense(
                Expense(title = title, amount = amount, currency = currency, category = category)
              )
            } else {
              isParsingError = true
              selectedExpense =
                Expense(title = "", amount = 0.0, currency = "USD", category = "activity")
              isDetailSheetVisible = true
            }
          } catch (e: Exception) {
            Log.e("ManageExpensesScreen", "Error using GenAI Prompt API", e)
            isParsingError = true
            selectedExpense =
              Expense(title = "", amount = 0.0, currency = "USD", category = "activity")
            isDetailSheetVisible = true
          } finally {
            isProcessing.value = false
          }
        }
      }
    }

  LaunchedEffect(Unit) {
    onFabConfigChange(
      JetPackerFabConfig(
        icon = Icons.Rounded.DocumentScanner,
        onClick = {
          coroutineScope.launch {
            if (viewModel.isSupported()) {
              cameraLauncher.launch(null)
            } else {
              Toast.makeText(context, "Feature not available", Toast.LENGTH_SHORT).show()
            }
          }
        },
        contentDescription = "Add Expense",
      )
    )
  }

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Expenses",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SekuyaFontFamily,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        },
        actions = {
          IconButton(onClick = {}) {
            Icon(
              Icons.Rounded.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        },
        colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
          ),
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {

        // Total spent and Budget display
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          val totalText = "$${"%.2f".format(totalSpent)}"

          Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surface) {
            Text(
              text = totalText,
              style =
                MaterialTheme.typography.displayLarge.copy(
                  fontSize = 84.sp,
                  fontWeight = FontWeight.Black,
                ),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 84.sp),
              modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "/",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(30.dp),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.clickable { isBudgetSheetVisible = true },
            ) {
              Text(
                text = "$${budget.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              )
            }
          }
        }

        Text(
          text = "Recent expenses",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(vertical = 16.dp),
        )

        if (isProcessing.value) {
          ShimmerExpenseItem()
          Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
          modifier =
            Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
              .drawWithContent {
                drawContent()
                drawRect(
                  brush =
                    Brush.verticalGradient(
                      0f to Color.Black,
                      0.8f to Color.Black,
                      1f to Color.Transparent,
                    ),
                  blendMode = BlendMode.DstIn,
                )
              },
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 100.dp),
        ) {
          items(expenses, key = { it.id }) { expense ->
            ExpenseItem(
              expense = expense,
              isSwipedOff = expenseToDelete == expense,
              onClick = {
                isParsingError = false
                selectedExpense = expense
                isDetailSheetVisible = true
              },
              onDelete = { expenseToDelete = expense },
            )
          }
        }
      }
    }

    if (expenseToDelete != null) {
      DeleteExpenseSheet(
        expense = expenseToDelete!!,
        onConfirm = {
          viewModel.deleteExpense(expenseToDelete!!)
          expenseToDelete = null
        },
        onDismiss = { expenseToDelete = null },
      )
    }

    if (isBudgetSheetVisible) {
      BudgetEditSheet(
        initialBudget = budget,
        onDismiss = { isBudgetSheetVisible = false },
        onSave = { budget = it },
      )
    }

    if (isDetailSheetVisible && selectedExpense != null) {
      ExpenseDetailSheet(
        expense = selectedExpense!!,
        isParsingError = isParsingError,
        onDismiss = { isDetailSheetVisible = false },
        onSave = { updatedExpense ->
          viewModel.addExpense(updatedExpense)
          isDetailSheetVisible = false
        },
      )
    }
  }
}

@Composable
fun ShimmerExpenseItem() {
  val shimmerColors =
    listOf(
      Color.LightGray.copy(alpha = 0.6f),
      Color.LightGray.copy(alpha = 0.2f),
      Color.LightGray.copy(alpha = 0.6f),
    )

  val transition = rememberInfiniteTransition(label = "shimmer")
  val translateAnim =
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 2000f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "shimmer_translate",
    )

  val brush =
    Brush.linearGradient(
      colors = shimmerColors,
      start = Offset(translateAnim.value - 500f, translateAnim.value - 500f),
      end = Offset(translateAnim.value, translateAnim.value),
    )

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(brush))

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Spacer(
          modifier =
            Modifier.height(16.dp)
              .fillMaxWidth(0.7f)
              .clip(RoundedCornerShape(4.dp))
              .background(brush)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Spacer(
          modifier =
            Modifier.height(12.dp)
              .fillMaxWidth(0.4f)
              .clip(RoundedCornerShape(4.dp))
              .background(brush)
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Spacer(
          modifier =
            Modifier.height(16.dp).width(64.dp).clip(RoundedCornerShape(4.dp)).background(brush)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Spacer(
          modifier =
            Modifier.height(12.dp).width(48.dp).clip(RoundedCornerShape(4.dp)).background(brush)
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpenseItem(
  expense: Expense,
  isSwipedOff: Boolean,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val (icon, color) =
    when (expense.category.lowercase()) {
      "travel" -> Icons.Rounded.ModeOfTravel to EventColors.Flight
      "food" -> Icons.Rounded.Restaurant to EventColors.Food
      "shopping" -> Icons.Rounded.ShoppingCart to EventColors.Shopping
      "entertainment" -> Icons.Rounded.Star to EventColors.Entertainment
      "hotel" -> Icons.Rounded.Hotel to EventColors.Hotel
      else -> Icons.Rounded.ConfirmationNumber to EventColors.Activity
    }

  val dismissState =
    rememberSwipeToDismissBoxState(
      SwipeToDismissBoxValue.Settled,
      SwipeToDismissBoxDefaults.positionalThreshold,
    )

  LaunchedEffect(dismissState.currentValue) {
    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
      onDelete()
    }
  }

  LaunchedEffect(isSwipedOff) {
    if (!isSwipedOff && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
      dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }
  }

  SwipeToDismissBox(
    state = dismissState,
    modifier = modifier,
    enableDismissFromStartToEnd = false,
    backgroundContent = {
      val backgroundColor =
        when (dismissState.dismissDirection) {
          SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
          else -> Color.Transparent
        }

      Box(
        modifier =
          Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(backgroundColor),
        contentAlignment = Alignment.CenterEnd,
      ) {
        Icon(
          imageVector = Icons.Rounded.Delete,
          contentDescription = "Delete",
          tint = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(end = 16.dp),
        )
      }
    },
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      onClick = onClick,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
      Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
          val shape = MaterialShapes.SoftBoom
          Box(
            modifier =
              Modifier.fillMaxSize().drawWithCache {
                val path = shape.toPath().asComposePath()
                val matrix = Matrix()
                matrix.scale(size.width, size.height)
                path.transform(matrix)
                onDrawBehind { drawPath(path, color = color.content) }
              }
          )
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(16.dp),
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = expense.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = expense.category.lowercase(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text =
              "${CurrencyConverter.getSymbol(expense.currency)}${"%.2f".format(expense.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteExpenseSheet(expense: Expense, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  var isConfirmChecked by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 48.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier.size(80.dp)
            .background(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Delete,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(36.dp),
        )
      }

      Text(
        "Delete Expense?",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )

      Text(
        "Are you sure you want to delete '${expense.title}' for ${CurrencyConverter.getSymbol(expense.currency)}${"%.2f".format(expense.amount)}? This action cannot be undone.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier =
            Modifier.clickable { isConfirmChecked = !isConfirmChecked }
              .padding(horizontal = 8.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = isConfirmChecked,

            onCheckedChange = { isConfirmChecked = it }
          )
          Text(
            "I am sure I want to delete this expense.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(
          colors =
            ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
        ) {
          Text("Cancel")
        }
        Button(
          onClick = onConfirm,
          enabled = isConfirmChecked,
          modifier = Modifier.weight(1f),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          shape = MaterialTheme.shapes.medium,
        ) {
          Text("Delete")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailSheet(
  expense: Expense,
  isParsingError: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (Expense) -> Unit,
) {
  var title by remember { mutableStateOf(expense.title) }
  var amountText by remember {
    mutableStateOf(if (expense.amount == 0.0) "" else expense.amount.toString())
  }
  var category by remember { mutableStateOf(expense.category) }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 24.dp)) {
      Text(
        "Expense Details",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = if (isParsingError) 8.dp else 16.dp),
      )
      if (isParsingError) {
        Text(
          text = "Expense processing failed - enter fields manually",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount") },
        prefix = { Text(CurrencyConverter.getSymbol(expense.currency)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = category,
        onValueChange = { category = it },
        label = { Text("Category") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
      )

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = {
          val newAmount = amountText.toDoubleOrNull() ?: expense.amount
          onSave(expense.copy(title = title, amount = newAmount, category = category.lowercase()))
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
      ) {
        Text("Save Changes")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditSheet(initialBudget: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
  var text by remember { mutableStateOf(initialBudget.toInt().toString()) }
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        "Set Trip Budget",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(16.dp))
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Budget Amount") },
        prefix = { Text("$") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
      )
      Spacer(modifier = Modifier.height(24.dp))
      Button(
        onClick = {
          onSave(text.toDoubleOrNull() ?: initialBudget)
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
      ) {
        Text("Save Budget")
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun ShimmerExpenseItemPreview() {
  MaterialTheme { ShimmerExpenseItem() }
}

@Preview(showBackground = true)
@Composable
fun ExpenseItemPreview() {
  MaterialTheme {
    ExpenseItem(
      expense = DummyData.expenses.first(),
      isSwipedOff = false,
      onClick = {},
      onDelete = {},
    )
  }
}
