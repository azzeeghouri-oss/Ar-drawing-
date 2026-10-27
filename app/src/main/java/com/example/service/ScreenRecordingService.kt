package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordingService : Service() {

  companion object {
    const val CHANNEL_ID = "ar_drawing_screen_recorder"
    const val NOTIFICATION_ID = 2026
    const val ACTION_START = "com.example.service.START_RECORDING"
    const val ACTION_STOP = "com.example.service.STOP_RECORDING"
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_DATA = "extra_data"

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds = _recordingDurationSeconds.asStateFlow()

    private val _lastSavedVideoUri = MutableStateFlow<Uri?>(null)
    val lastSavedVideoUri = _lastSavedVideoUri.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
  }

  private var mediaProjection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var mediaRecorder: MediaRecorder? = null
  private var outputFile: File? = null
  private var timerJob: Job? = null
  private val serviceScope = CoroutineScope(Dispatchers.Main)

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableExtra(EXTRA_DATA)
        }
        if (resultCode != 0 && data != null) {
          startRecording(resultCode, data)
        } else {
          stopSelf()
        }
      }
      ACTION_STOP -> {
        stopRecording()
        stopSelf()
      }
    }
    return START_NOT_STICKY
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "AR Drawing Recording",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Active screen recording for AR drawing sessions"
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun createNotification(seconds: Int = 0): Notification {
    val contentIntent = Intent(this, MainActivity::class.java)
    val pendingContentIntent = PendingIntent.getActivity(
      this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val stopIntent = Intent(this, ScreenRecordingService::class.java).apply {
      action = ACTION_STOP
    }
    val pendingStopIntent = PendingIntent.getService(
      this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val minutes = seconds / 60
    val secs = seconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Recording AR Drawing Session")
      .setContentText("Elapsed: $timeFormatted • Drawing in progress")
      .setSmallIcon(android.R.drawable.ic_menu_camera)
      .setContentIntent(pendingContentIntent)
      .addAction(android.R.drawable.ic_media_pause, "Stop Recording", pendingStopIntent)
      .setOngoing(true)
      .build()
  }

  private fun startRecording(resultCode: Int, data: Intent) {
    try {
      // Must promote to foreground first (mandatory in Android 14)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          NOTIFICATION_ID,
          createNotification(),
          ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
      } else {
        startForeground(NOTIFICATION_ID, createNotification())
      }

      val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
      val projection = projectionManager.getMediaProjection(resultCode, data)
      if (projection == null) {
        _errorMessage.value = "Failed to obtain screen projection"
        stopSelf()
        return
      }
      mediaProjection = projection

      // Prepare screen metrics
      val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      windowManager.defaultDisplay.getRealMetrics(metrics)

      // Ensure even dimensions for H264
      var width = metrics.widthPixels
      var height = metrics.heightPixels
      if (width % 2 != 0) width -= 1
      if (height % 2 != 0) height -= 1
      val density = metrics.densityDpi

      // Setup output file in Movies/AR_Drawings
      val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
      val appDrawingsDir = File(moviesDir, "AR_Drawings").apply { if (!exists()) mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val videoFile = File(appDrawingsDir, "AR_Trace_$timeStamp.mp4")
      outputFile = videoFile

      // Initialize MediaRecorder
      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(this)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }
      mediaRecorder = recorder

      recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
      recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
      recorder.setVideoEncodingBitRate(6_000_000)
      recorder.setVideoFrameRate(30)
      recorder.setVideoSize(width, height)
      recorder.setOutputFile(videoFile.absolutePath)
      recorder.prepare()

      virtualDisplay = projection.createVirtualDisplay(
        "ARScreenRecorder",
        width,
        height,
        density,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        recorder.surface,
        null,
        null
      )

      recorder.start()
      _isRecording.value = true
      _recordingDurationSeconds.value = 0
      _errorMessage.value = null

      timerJob = serviceScope.launch {
        while (isActive) {
          delay(1000)
          val elapsed = _recordingDurationSeconds.value + 1
          _recordingDurationSeconds.value = elapsed
          val notification = createNotification(elapsed)
          val manager = getSystemService(NotificationManager::class.java)
          manager?.notify(NOTIFICATION_ID, notification)
        }
      }

      projection.registerCallback(object : MediaProjection.Callback() {
        override fun onStop() {
          stopRecording()
        }
      }, null)

    } catch (e: Exception) {
      e.printStackTrace()
      _errorMessage.value = "Recording error: ${e.message}"
      cleanup()
      stopSelf()
    }
  }

  private fun stopRecording() {
    timerJob?.cancel()
    timerJob = null

    try {
      mediaRecorder?.stop()
    } catch (e: Exception) {
      e.printStackTrace()
    }

    try {
      virtualDisplay?.release()
      virtualDisplay = null
    } catch (e: Exception) {
      e.printStackTrace()
    }

    try {
      mediaProjection?.stop()
      mediaProjection = null
    } catch (e: Exception) {
      e.printStackTrace()
    }

    try {
      mediaRecorder?.reset()
      mediaRecorder?.release()
      mediaRecorder = null
    } catch (e: Exception) {
      e.printStackTrace()
    }

    _isRecording.value = false

    outputFile?.let { file ->
      if (file.exists() && file.length() > 0) {
        MediaScannerConnection.scanFile(
          applicationContext,
          arrayOf(file.absolutePath),
          arrayOf("video/mp4")
        ) { _, uri ->
          _lastSavedVideoUri.value = uri ?: Uri.fromFile(file)
        }
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
  }

  private fun cleanup() {
    try {
      timerJob?.cancel()
      timerJob = null
      mediaRecorder?.release()
      mediaRecorder = null
      virtualDisplay?.release()
      virtualDisplay = null
      mediaProjection?.stop()
      mediaProjection = null
      _isRecording.value = false
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onDestroy() {
    cleanup()
    super.onDestroy()
  }
}
