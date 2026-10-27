package com.example.camera

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

@Composable
fun CameraPreviewView(
  modifier: Modifier = Modifier,
  isTorchEnabled: Boolean = false,
  useFrontCamera: Boolean = false,
  onTorchAvailabilityChanged: (Boolean) -> Unit = {}
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var camera by remember { mutableStateOf<Camera?>(null) }
  val previewView = remember {
    PreviewView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      scaleType = PreviewView.ScaleType.FILL_CENTER
      implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }
  }

  // Camera Binding
  LaunchedEffect(useFrontCamera) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor = ContextCompat.getMainExecutor(context)

    cameraProviderFuture.addListener({
      try {
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()

        val cameraSelector = if (useFrontCamera) {
          CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
          CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder().build().also {
          it.surfaceProvider = previewView.surfaceProvider
        }

        if (cameraProvider.hasCamera(cameraSelector)) {
          val boundCamera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview
          )
          camera = boundCamera
          val hasFlash = boundCamera.cameraInfo.hasFlashUnit()
          onTorchAvailabilityChanged(hasFlash)
        } else {
          Log.w("CameraPreview", "Camera selector not available")
          onTorchAvailabilityChanged(false)
        }
      } catch (e: Exception) {
        Log.e("CameraPreview", "Failed to bind camera: ${e.message}", e)
        onTorchAvailabilityChanged(false)
      }
    }, executor)
  }

  // Handle Torch Toggling
  LaunchedEffect(isTorchEnabled, camera) {
    val cam = camera
    if (cam != null && cam.cameraInfo.hasFlashUnit()) {
      try {
        cam.cameraControl.enableTorch(isTorchEnabled)
      } catch (e: Exception) {
        Log.e("CameraPreview", "Torch toggle failed: ${e.message}")
      }
    }
  }

  // Clean unbind on disposal
  DisposableEffect(lifecycleOwner) {
    onDispose {
      try {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        camera?.cameraControl?.enableTorch(false)
        cameraProvider.unbindAll()
      } catch (e: Exception) {
        // Ignored
      }
    }
  }

  AndroidView(
    factory = { ctx ->
      // Setup tap-to-focus
      previewView.setOnTouchListener { view, event ->
        if (event.action == android.view.MotionEvent.ACTION_UP) {
          val cam = camera
          if (cam != null) {
            val factory = SurfaceOrientedMeteringPointFactory(
              view.width.toFloat(),
              view.height.toFloat()
            )
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            cam.cameraControl.startFocusAndMetering(action)
          }
        }
        true
      }
      previewView
    },
    modifier = modifier.fillMaxSize()
  )
}
