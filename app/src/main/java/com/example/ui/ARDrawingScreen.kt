package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.camera.CameraPreviewView
import com.example.model.ARTransform
import com.example.model.EdgeFilterMode
import com.example.model.SketchItem
import com.example.model.SketchTemplates
import com.example.recorder.ScreenRecorderManager
import java.util.Locale

@Composable
fun ARDrawingScreen(
  recorderManager: ScreenRecorderManager,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isRecording by recorderManager.isRecording.collectAsState()
  val recordingDuration by recorderManager.recordingDurationSeconds.collectAsState()
  val lastSavedVideoUri by recorderManager.lastSavedVideoUri.collectAsState()
  val recordingError by recorderManager.errorMessage.collectAsState()

  // Camera & Permissions state
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
    if (!isGranted) {
      Toast.makeText(context, "Camera permission needed to trace on paper", Toast.LENGTH_SHORT).show()
    }
  }

  // Active Sketch Item (Defaults to starter template or imported photo)
  var selectedSketch by remember {
    mutableStateOf<SketchItem?>(SketchTemplates.defaultTemplates.firstOrNull())
  }

  // Transform State (opacity, position, scale, rotation, filter)
  var transform by remember { mutableStateOf(ARTransform()) }

  // Hardware controls
  var isTorchEnabled by remember { mutableStateOf(false) }
  var hasTorchHardware by remember { mutableStateOf(true) }
  var useFrontCamera by remember { mutableStateOf(false) }

  // Lock State
  var isLocked by remember { mutableStateOf(false) }

  // Modals
  var showSketchSheet by remember { mutableStateOf(false) }
  var showRecordingsDialog by remember { mutableStateOf(false) }

  // Screen recording permission launcher
  val mediaProjectionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
      recorderManager.startRecording(result.resultCode, result.data!!)
      Toast.makeText(context, "Screen recording started!", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Screen recording cancelled", Toast.LENGTH_SHORT).show()
    }
  }

  // Photo Picker Launcher
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedSketch = SketchItem(
        id = "custom_${System.currentTimeMillis()}",
        title = "Imported Photo",
        category = "Gallery",
        uri = uri,
        isCustomPhoto = true
      )
      // Reset position and center for newly imported photo
      transform = transform.copy(scale = 1.0f, offsetX = 0f, offsetY = 0f, rotation = 0f)
      Toast.makeText(context, "Photo loaded! Adjust opacity or tap Lock to trace", Toast.LENGTH_SHORT).show()
    }
  }

  // Feedback when recording finishes
  LaunchedEffect(lastSavedVideoUri) {
    if (lastSavedVideoUri != null) {
      Toast.makeText(context, "Recording saved to Movies/AR_Drawings!", Toast.LENGTH_LONG).show()
    }
  }

  LaunchedEffect(recordingError) {
    if (recordingError != null) {
      Toast.makeText(context, recordingError, Toast.LENGTH_LONG).show()
    }
  }

  Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

    // 1. Live Camera Preview Layer
    if (hasCameraPermission) {
      CameraPreviewView(
        modifier = Modifier.fillMaxSize(),
        isTorchEnabled = isTorchEnabled,
        useFrontCamera = useFrontCamera,
        onTorchAvailabilityChanged = { available ->
          hasTorchHardware = available
        }
      )
    } else {
      // Camera permission request fallback
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF141722)),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Camera Access Required",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "AR Drawing requires the camera to display your real paper and pencil under the tracing overlay.",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = Color.LightGray
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("grant_camera_permission_button")
          ) {
            Text("Enable Camera", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 2. AR Sketch Overlay Layer
    selectedSketch?.let { sketch ->
      AROverlayView(
        sketchItem = sketch,
        transform = transform,
        isLocked = isLocked,
        onTransformChanged = { updatedTransform ->
          transform = updatedTransform
        }
      )
    }

    // Status / Top Bars insets
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 3. LOCK BUTTON IN TOP RIGHT CORNER (Mandatory)
    // "Upon selecting a photo, include a lock button in the top right corner;
    // when tapped, this lock must hide all interface options, including the opacity adjustment,
    // and provide access to a torch toggle and screen recording button."
    if (selectedSketch != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = statusBarPadding + 12.dp, end = 16.dp),
        contentAlignment = Alignment.TopEnd
      ) {
        Surface(
          onClick = {
            isLocked = !isLocked
            if (isLocked) {
              Toast.makeText(context, "Locked! Tracing position frozen.", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, "Unlocked", Toast.LENGTH_SHORT).show()
            }
          },
          shape = RoundedCornerShape(24.dp),
          color = if (isLocked) Color(0xFF00E676) else Color(0xCC1A1D27),
          border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isLocked) Color.White else Color(0xFF00E5FF)
          ),
          shadowElevation = 8.dp,
          modifier = Modifier.testTag("lock_toggle_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
              contentDescription = if (isLocked) "Unlock Canvas" else "Lock Canvas",
              tint = if (isLocked) Color.Black else Color(0xFF00E5FF),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isLocked) "LOCKED" else "LOCK",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (isLocked) Color.Black else Color.White,
                fontSize = 13.sp
              )
            )
          }
        }
      }
    }

    // 4. LOCKED MODE INTERFACE:
    // "when tapped, this lock must hide all interface options, including the opacity adjustment,
    // and provide access to a torch toggle and screen recording button."
    if (isLocked) {
      // Floating streamlined controls for Torch & Screen Recording
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = statusBarPadding + 12.dp, start = 16.dp),
        contentAlignment = Alignment.TopStart
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Torch Toggle Button
          Surface(
            onClick = {
              if (hasTorchHardware) {
                isTorchEnabled = !isTorchEnabled
              } else {
                Toast.makeText(context, "No camera flash unit available on this device", Toast.LENGTH_SHORT).show()
              }
            },
            shape = CircleShape,
            color = if (isTorchEnabled) Color(0xFFFFB703) else Color(0xCC1A1D27),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isTorchEnabled) Color.White else Color(0xFFFFB703).copy(alpha = 0.6f)
            ),
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp).testTag("torch_toggle_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = if (isTorchEnabled) "Turn Torch Off" else "Turn Torch On",
                tint = if (isTorchEnabled) Color.Black else Color(0xFFFFB703),
                modifier = Modifier.size(22.dp)
              )
            }
          }

          // Screen Recording Button
          Surface(
            onClick = {
              if (isRecording) {
                recorderManager.stopRecording()
                Toast.makeText(context, "Stopping recording...", Toast.LENGTH_SHORT).show()
              } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    val activity = context as? Activity
                    activity?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                  }
                }
                mediaProjectionLauncher.launch(recorderManager.createScreenCaptureIntent())
              }
            },
            shape = RoundedCornerShape(24.dp),
            color = if (isRecording) Color(0xFFFF3366) else Color(0xCC1A1D27),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isRecording) Color.White else Color(0xFFFF3366).copy(alpha = 0.6f)
            ),
            shadowElevation = 6.dp,
            modifier = Modifier.height(46.dp).testTag("record_screen_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = if (isRecording) "Stop Recording" else "Record Screen",
                tint = if (isRecording) Color.White else Color(0xFFFF3366),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              val minutes = recordingDuration / 60
              val seconds = recordingDuration % 60
              val timerText = if (isRecording) {
                String.format(Locale.getDefault(), "REC %02d:%02d", minutes, seconds)
              } else {
                "RECORD"
              }
              Text(
                text = timerText,
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  fontSize = 12.sp
                )
              )
            }
          }
        }
      }

      // Minimal hint banner for tracing in locked mode
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .padding(bottom = navBarPadding + 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xAA10121A),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Position Locked • Trace on Paper Below • Tap top-right to unlock",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp
              )
            )
          }
        }
      }
    }

    // 5. UNLOCKED MODE INTERFACE:
    // All interface options are visible only when NOT locked!
    AnimatedVisibility(
      visible = !isLocked,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(modifier = Modifier.fillMaxSize()) {

        // Top Navigation & Action Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = statusBarPadding + 10.dp, start = 14.dp, end = 120.dp), // Right end reserved for Lock button
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Import Photo Button
          Surface(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("import_photo_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "Import Photo",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Import Photo",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              )
            }
          }

          // Templates Button
          Surface(
            onClick = { showSketchSheet = true },
            shape = CircleShape,
            color = Color(0xCC1A1D27),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier.size(40.dp).testTag("templates_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Templates",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          // Recordings Gallery Button
          Surface(
            onClick = { showRecordingsDialog = true },
            shape = CircleShape,
            color = Color(0xCC1A1D27),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier.size(40.dp).testTag("recordings_gallery_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Saved Recordings",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // Bottom Interface Controls: Opacity adjustment + Filters + Transform tools
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp)
            .padding(bottom = navBarPadding + 12.dp)
        ) {
          // Bottom Container Card
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE161822)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF))
          ) {
            Column(modifier = Modifier.padding(16.dp)) {

              // Opacity Adjustment (Explicitly required in prompt to be hidden when locked)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Opacity",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${(transform.opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.SemiBold
                    )
                  )
                }

                // Opacity presets (25%, 50%, 75%)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  listOf(0.25f, 0.50f, 0.75f).forEach { preset ->
                    Surface(
                      onClick = { transform = transform.copy(opacity = preset) },
                      shape = RoundedCornerShape(12.dp),
                      color = if (kotlin.math.abs(transform.opacity - preset) < 0.05f) {
                        MaterialTheme.colorScheme.primary
                      } else {
                        Color(0x33FFFFFF)
                      },
                      modifier = Modifier.height(24.dp)
                    ) {
                      Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = "${(preset * 100).toInt()}%",
                          style = MaterialTheme.typography.labelSmall.copy(
                            color = if (kotlin.math.abs(transform.opacity - preset) < 0.05f) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                          )
                        )
                      }
                    }
                  }
                }
              }

              // Opacity Slider
              Slider(
                value = transform.opacity,
                onValueChange = { newOpacity ->
                  transform = transform.copy(opacity = newOpacity)
                },
                valueRange = 0.10f..1.0f,
                colors = SliderDefaults.colors(
                  thumbColor = MaterialTheme.colorScheme.primary,
                  activeTrackColor = MaterialTheme.colorScheme.primary,
                  inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("opacity_slider")
              )

              Spacer(modifier = Modifier.height(6.dp))

              // Filter Mode Chips (Original, Pencil Sketch Outline, Invert, High Contrast)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                EdgeFilterMode.values().forEach { mode ->
                  val isSelected = transform.edgeFilter == mode
                  Surface(
                    onClick = { transform = transform.copy(edgeFilter = mode) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0x22FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).height(32.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                          color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                          fontSize = 11.sp
                        ),
                        maxLines = 1
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Bottom Utilities: Flip Horizontal, Reset Position, Camera switch
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  // Flip Horizontal
                  Surface(
                    onClick = {
                      transform = transform.copy(isFlippedHorizontally = !transform.isFlippedHorizontally)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (transform.isFlippedHorizontally) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color(0x22FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (transform.isFlippedHorizontally) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    modifier = Modifier.testTag("flip_button")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Flip Horizontal",
                        tint = if (transform.isFlippedHorizontally) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Mirror",
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = if (transform.isFlippedHorizontally) MaterialTheme.colorScheme.primary else Color.White
                        )
                      )
                    }
                  }

                  // Reset Transform
                  Surface(
                    onClick = {
                      transform = transform.copy(
                        scale = 1.0f,
                        offsetX = 0f,
                        offsetY = 0f,
                        rotation = 0f,
                        isFlippedHorizontally = false
                      )
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x22FFFFFF),
                    modifier = Modifier.testTag("reset_transform_button")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Position",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                      )
                    }
                  }
                }

                // Switch Camera (Back / Front)
                IconButton(
                  onClick = { useFrontCamera = !useFrontCamera },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    // Modal Bottom Sheets
    if (showSketchSheet) {
      SketchSelectorSheet(
        selectedItem = selectedSketch,
        onSelectSketch = { template ->
          selectedSketch = template
          transform = transform.copy(scale = 1.0f, offsetX = 0f, offsetY = 0f, rotation = 0f)
        },
        onImportGalleryPhoto = { uri ->
          selectedSketch = SketchItem(
            id = "custom_${System.currentTimeMillis()}",
            title = "Imported Photo",
            category = "Gallery",
            uri = uri,
            isCustomPhoto = true
          )
          transform = transform.copy(scale = 1.0f, offsetX = 0f, offsetY = 0f, rotation = 0f)
          showSketchSheet = false
        },
        onDismiss = { showSketchSheet = false }
      )
    }

    if (showRecordingsDialog) {
      RecordingsDialog(
        onDismiss = { showRecordingsDialog = false }
      )
    }
  }
}
