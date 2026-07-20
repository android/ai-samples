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

package com.example.jetpacker

import android.content.Context
import android.content.pm.ApplicationInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.ui.navigation.JetPackerNavGraph
import com.example.jetpacker.ui.navigation.Navigator
import com.example.jetpacker.ui.navigation.Screen
import com.example.jetpacker.ui.navigation.rememberNavigationState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleFeatureFlags()

    setContent {
      JetPackerTheme {
        val navigationState = rememberNavigationState(startRoute = Screen.MyTrips)
        val navigator = remember(navigationState) { Navigator(navigationState) }

        SetupShakeDetection(navigator)

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          JetPackerNavGraph(navigationState = navigationState, navigator = navigator)
        }
      }
    }
  }

  private fun handleFeatureFlags() {
    val overrideTime = intent?.getLongExtra(FeatureFlags.EXTRA_OVERRIDE_TIME, 0L) ?: 0L
    if (overrideTime > 0L) {
      FeatureFlags.putLongFlag(this, FeatureFlags.KEY_OVERRIDE_CURRENT_TIME_MILLIS, overrideTime)
    }
  }

  @Composable
  private fun SetupShakeDetection(navigator: Navigator) {
    val context = LocalContext.current
    val isDebuggable =
      remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
      }

    DisposableEffect(isDebuggable) {
      if (!isDebuggable) return@DisposableEffect onDispose {}
      val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
      val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      val shakeDetector = ShakeDetector {
        navigator.navigate(Screen.Debug)
      }
      accelerometer?.let {
        sensorManager.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_UI)
      }
      onDispose { sensorManager.unregisterListener(shakeDetector) }
    }
  }
}
