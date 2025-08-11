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
 * Converts a comma-separated string of time values (in hh:mm:ss format)
 * into a list of timestamps in milliseconds.
 *
 * Malformed or invalid time strings in the input are logged as warnings and skipped.
 *
 * @param commaSeparatedTimeString The string to parse, e.g., "00:01:30, 00:05:00, 99:99:99".
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
            parseHhMmSsToTimestamp(timeString)
        }
}

/**
 * Parses a single time string in hh:mm:ss format to milliseconds.
 *
 * @param timeString The time string to parse (e.g., "01:23:45").
 * @return The time in milliseconds, or null if the format is invalid or the
 *         time components are out of range.
 */
private fun parseHhMmSsToTimestamp(timeString: String): Long? {
    val timeRegex = "(\\d{2}):(\\d{2}):(\\d{2})".toRegex()

    val matchResult = timeRegex.matchEntire(timeString)
    if (matchResult == null) {
        Log.w(TAG, "Time string '$timeString' does not match hh:mm:ss format. Skipping.")
        return null
    }

    return try {
        val (hoursStr, minutesStr, secondsStr) = matchResult.destructured
        val hours = hoursStr.toInt()
        val minutes = minutesStr.toInt()
        val seconds = secondsStr.toInt()

        if (hours in 0..23 && minutes in 0..59 && seconds in 0..59) {
            (hours * 3600L + minutes * 60L + seconds) * 1000L
        } else {
            Log.w(TAG, "Time components out of valid range for '$timeString'. Skipping.")
            null
        }
    } catch (e: NumberFormatException) {
        // This case is unlikely with the regex, but good for robustness.
        Log.w(TAG, "Error parsing number components in '$timeString'. Skipping.", e)
        null
    }
}
