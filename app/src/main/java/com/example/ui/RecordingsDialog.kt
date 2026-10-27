package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingItem(
  val file: File,
  val name: String,
  val sizeFormatted: String,
  val dateFormatted: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val recordings = remember { mutableStateListOf<RecordingItem>() }

  fun refreshRecordings() {
    recordings.clear()
    val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    val arDir = File(moviesDir, "AR_Drawings")
    if (arDir.exists() && arDir.isDirectory) {
      val files = arDir.listFiles { f -> f.extension.equals("mp4", ignoreCase = true) }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()
      files.forEach { file ->
        val sizeMb = String.format(Locale.getDefault(), "%.1f MB", file.length() / (1024.0 * 1024.0))
        val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
        recordings.add(
          RecordingItem(
            file = file,
            name = file.name,
            sizeFormatted = sizeMb,
            dateFormatted = date
          )
        )
      }
    }
  }

  LaunchedEffect(Unit) {
    refreshRecordings()
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Videocam,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Saved Drawing Recordings",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            )
            Text(
              text = "${recordings.size} videos stored offline in Movies/AR_Drawings",
              style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (recordings.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Videocam,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No recorded sessions yet",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Text(
              text = "Tap Lock mode and use the red Record button to capture your drawing process!",
              style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              ),
              modifier = Modifier.padding(horizontal = 24.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(recordings) { item ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              shape = RoundedCornerShape(12.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .clickable {
                      // Open video with system player
                      try {
                        val uri = FileProvider.getUriForFile(
                          context,
                          "${context.packageName}.fileprovider",
                          item.file
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                          setDataAndType(uri, "video/mp4")
                          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                      } catch (e: Exception) {
                        e.printStackTrace()
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = MaterialTheme.colorScheme.primary
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                  )
                  Text(
                    text = "${item.sizeFormatted} • ${item.dateFormatted}",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  )
                }

                // Share
                IconButton(onClick = {
                  try {
                    val uri = FileProvider.getUriForFile(
                      context,
                      "${context.packageName}.fileprovider",
                      item.file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "video/mp4"
                      putExtra(Intent.EXTRA_STREAM, uri)
                      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Drawing Video"))
                  } catch (e: Exception) {
                    e.printStackTrace()
                  }
                }) {
                  Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                // Delete
                IconButton(onClick = {
                  try {
                    item.file.delete()
                    refreshRecordings()
                  } catch (e: Exception) {
                    e.printStackTrace()
                  }
                }) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
