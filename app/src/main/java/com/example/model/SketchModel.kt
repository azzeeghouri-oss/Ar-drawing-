package com.example.model

import android.net.Uri

enum class EdgeFilterMode(val label: String) {
  NONE("Original"),
  SKETCH_OUTLINE("Pencil Sketch"),
  INVERTED("Invert Line"),
  HIGH_CONTRAST("High Contrast")
}

data class ARTransform(
  val scale: Float = 1.0f,
  val offsetX: Float = 0f,
  val offsetY: Float = 0f,
  val rotation: Float = 0f,
  val opacity: Float = 0.50f,
  val isFlippedHorizontally: Boolean = false,
  val edgeFilter: EdgeFilterMode = EdgeFilterMode.SKETCH_OUTLINE
)

data class SketchItem(
  val id: String,
  val title: String,
  val category: String,
  val uri: Uri? = null,
  val drawableResId: Int? = null,
  val isCustomPhoto: Boolean = false
)
