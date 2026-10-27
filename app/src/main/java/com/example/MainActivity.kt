package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.recorder.ScreenRecorderManager
import com.example.ui.ARDrawingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val recorderManager = remember { ScreenRecorderManager(applicationContext) }
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color.Black
        ) {
          ARDrawingScreen(
            recorderManager = recorderManager
          )
        }
      }
    }
  }
}
