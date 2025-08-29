package com.android.ai.samples.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

internal fun loadBitmapWithCorrectOrientation(context: Context, uri: Uri): Bitmap? {
    var bitmap: Bitmap? = null
    try {
        bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exifInterface = ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1.0f, 1.0f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(-90f)
                    matrix.preScale(-1.0f, 1.0f)
                }
            }
            bitmap = bitmap?.let {
                Bitmap.createBitmap(
                    it,
                    0,
                    0,
                    it.width,
                    it.height,
                    matrix,
                    true
                )
            }
        }
    } catch (e: IOException) {
        // Handle exceptions if any
        e.printStackTrace()
    }
    return bitmap
}
