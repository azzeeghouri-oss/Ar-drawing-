package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ARTransform
import com.example.model.SketchItem
import com.example.util.ImageProcessing
import kotlin.math.roundToInt

@Composable
fun AROverlayView(
  sketchItem: SketchItem,
  transform: ARTransform,
  isLocked: Boolean,
  onTransformChanged: (ARTransform) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colorFilter = ImageProcessing.getColorFilter(transform.edgeFilter)

  Box(
    modifier = modifier
      .fillMaxSize()
      .pointerInput(isLocked) {
        if (!isLocked) {
          detectTransformGestures { centroid, pan, zoom, rotation ->
            val newScale = (transform.scale * zoom).coerceIn(0.2f, 6.0f)
            val newRotation = (transform.rotation + rotation) % 360f
            val newOffsetX = transform.offsetX + pan.x
            val newOffsetY = transform.offsetY + pan.y
            onTransformChanged(
              transform.copy(
                scale = newScale,
                rotation = newRotation,
                offsetX = newOffsetX,
                offsetY = newOffsetY
              )
            )
          }
        }
      },
    contentAlignment = Alignment.Center
  ) {
    val scaleFactorX = if (transform.isFlippedHorizontally) -transform.scale else transform.scale
    val scaleFactorY = transform.scale

    val imageModifier = Modifier
      .testTag("ar_overlay_image")
      .graphicsLayer {
        translationX = transform.offsetX
        translationY = transform.offsetY
        scaleX = scaleFactorX
        scaleY = scaleFactorY
        rotationZ = transform.rotation
        alpha = transform.opacity.coerceIn(0.05f, 1.0f)
      }

    if (sketchItem.uri != null) {
      AsyncImage(
        model = ImageRequest.Builder(context)
          .data(sketchItem.uri)
          .crossfade(true)
          .build(),
        contentDescription = "AR Overlay Photo",
        colorFilter = colorFilter,
        contentScale = ContentScale.Fit,
        modifier = imageModifier.size(320.dp)
      )
    } else if (sketchItem.drawableResId != null) {
      Image(
        painter = painterResource(id = sketchItem.drawableResId),
        contentDescription = sketchItem.title,
        colorFilter = colorFilter,
        contentScale = ContentScale.Fit,
        modifier = imageModifier.size(320.dp)
      )
    }
  }
}
