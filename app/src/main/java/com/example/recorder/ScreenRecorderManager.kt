package com.example.recorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import com.example.service.ScreenRecordingService
import kotlinx.coroutines.flow.StateFlow

class ScreenRecorderManager(private val context: Context) {

  val isRecording: StateFlow<Boolean> = ScreenRecordingService.isRecording
  val recordingDurationSeconds: StateFlow<Int> = ScreenRecordingService.recordingDurationSeconds
  val lastSavedVideoUri: StateFlow<Uri?> = ScreenRecordingService.lastSavedVideoUri
  val errorMessage: StateFlow<String?> = ScreenRecordingService.errorMessage

  fun createScreenCaptureIntent(): Intent {
    val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    return manager.createScreenCaptureIntent()
  }

  fun startRecording(resultCode: Int, data: Intent) {
    val serviceIntent = Intent(context, ScreenRecordingService::class.java).apply {
      action = ScreenRecordingService.ACTION_START
      putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
      putExtra(ScreenRecordingService.EXTRA_DATA, data)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(serviceIntent)
    } else {
      context.startService(serviceIntent)
    }
  }

  fun stopRecording() {
    val serviceIntent = Intent(context, ScreenRecordingService::class.java).apply {
      action = ScreenRecordingService.ACTION_STOP
    }
    context.startService(serviceIntent)
  }
}
