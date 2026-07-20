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

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.abs

class ShakeDetector(
  private val shakeThreshold: Float = ShakeDetector.DEFAULT_SHAKE_THRESHOLD,
  private val onShake: () -> Unit,
) : SensorEventListener {

  private data class Acceleration(val x: Float, val y: Float, val z: Float) {
    fun distanceTo(other: Acceleration): Float =
      abs(x - other.x) + abs(y - other.y) + abs(z - other.z)
  }

  private var lastUpdate = System.currentTimeMillis()
  private var lastAcc: Acceleration? = null

  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
      val now = System.currentTimeMillis()
      val currAcc = Acceleration(event.values[0], event.values[1], event.values[2])

      if (lastAcc == null) {
        lastAcc = currAcc
        lastUpdate = now
        return
      }

      val elapsed = now - lastUpdate
      if (elapsed > 100) {
        lastUpdate = now

        val speed = currAcc.distanceTo(lastAcc!!) / elapsed * 10000
        if (speed > shakeThreshold) {
          onShake()
        }

        lastAcc = currAcc
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

  companion object {
    const val DEFAULT_SHAKE_THRESHOLD = 3000f
  }
}
