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
package com.android.ai.samples.magicselfie.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File

fun rotateImageIfRequired(imageFile: File, bitmap: Bitmap): Bitmap {
    val ei = ExifInterface(imageFile.absolutePath)
    val orientation = ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL,
    )

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipImage(bitmap, true, false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipImage(bitmap, false, true)
        ExifInterface.ORIENTATION_TRANSPOSE -> flipImage(rotateImage(bitmap, 90f), true, false)
        ExifInterface.ORIENTATION_TRANSVERSE -> flipImage(rotateImage(bitmap, 270f), true, false)
        else -> bitmap
    }
}

fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun flipImage(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
    val matrix = Matrix()
    val scaleX = if (horizontal) -1f else 1f
    val scaleY = if (vertical) -1f else 1f
    matrix.setScale(scaleX, scaleY)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
