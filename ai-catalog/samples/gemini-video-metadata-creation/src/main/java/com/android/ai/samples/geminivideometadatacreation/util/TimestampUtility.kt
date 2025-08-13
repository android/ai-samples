/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ai.samples.geminivideometadatacreation.util

import android.util.Log

/**
 * Converts a comma-separated string of time values (in hh:mm:ss or mm:ss format)
 * into a list of timestamps in milliseconds.
 *
 * Malformed or invalid time strings in the input are logged as warnings and skipped.
 *
 * @param String to parse, e.g., "01:30, 00:05:00, 99:99".
 * @return A list of Long values representing each valid time in milliseconds.
 */
private const val TAG = "TimestampUtility"

fun convertCommaSeparatedTimeStringsToTimestamps(commaSeparatedTimeString: String?): List<Long> {
    if (commaSeparatedTimeString.isNullOrBlank()) {
        return emptyList()
    }

    return commaSeparatedTimeString
        .split(',')
        .map { it.trim() }
        .filterNot { it.isBlank() }
        .mapNotNull { timeString ->
            parseTimeStringToTimestamp(timeString)
        }
}

private const val SECONDS_IN_HOUR = 3600L
private const val MINUTES_IN_HOUR = 60L
private const val MILLISECONDS_IN_SECOND = 1000L

/**
 * Parses a single time string in hh:mm:ss or mm:ss format to milliseconds.
 *
 * @param timeString The time string to parse (e.g., "01:23:45" or "23:45").
 * @return The time in milliseconds, or null if the format is invalid or the
 *         time components are out of range.
 */
private fun parseTimeStringToTimestamp(timeString: String): Long? {
    try {
        val parts = timeString.split(':').map { it.toInt() }

        val (hours, minutes, seconds) = when (parts.size) {
            3 -> Triple(parts[0], parts[1], parts[2]) // hh:mm:ss
            2 -> Triple(0, parts[0], parts[1]) // mm:ss
            else -> {
                Log.w(TAG, "Invalid time format for '$timeString'. Expected hh:mm:ss or mm:ss.")
                return null
            }
        }

        if (hours in 0..23 && minutes in 0..59 && seconds in 0..59) {
            return (hours * SECONDS_IN_HOUR + minutes * MINUTES_IN_HOUR + seconds) * MILLISECONDS_IN_SECOND
        } else {
            Log.w(TAG, "Time components out of valid range for '$timeString'. Skipping.")
            return null
        }
    } catch (e: NumberFormatException) {
        Log.w(TAG, "Error parsing number components in '$timeString'. Skipping.", e)
        return null
    }
}
