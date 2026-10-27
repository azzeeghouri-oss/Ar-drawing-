package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.model.EdgeFilterMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageProcessing {

  fun getColorFilter(mode: EdgeFilterMode): ColorFilter? {
    return when (mode) {
      EdgeFilterMode.NONE -> null
      EdgeFilterMode.SKETCH_OUTLINE -> {
        // High-contrast desaturate and edge threshold
        val cm = ColorMatrix(
          floatArrayOf(
            -1.5f, -1.5f, -1.5f, 0f, 380f,
            -1.5f, -1.5f, -1.5f, 0f, 380f,
            -1.5f, -1.5f, -1.5f, 0f, 380f,
            0f, 0f, 0f, 1f, 0f
          )
        )
        ColorFilter.colorMatrix(cm)
      }
      EdgeFilterMode.INVERTED -> {
        val cm = ColorMatrix(
          floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
          )
        )
        ColorFilter.colorMatrix(cm)
      }
      EdgeFilterMode.HIGH_CONTRAST -> {
        // High contrast grayscale
        val cm = ColorMatrix(
          floatArrayOf(
            2.0f, 0f, 0f, 0f, -130f,
            0f, 2.0f, 0f, 0f, -130f,
            0f, 0f, 2.0f, 0f, -130f,
            0f, 0f, 0f, 1f, 0f
          )
        )
        ColorFilter.colorMatrix(cm)
      }
    }
  }

  suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
