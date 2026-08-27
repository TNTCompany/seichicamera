package com.tnt.seichicamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tnt.seichicamera.ui.theme.SeichiCameraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeichiCameraTheme {
                // Navigation will be added in Task 5
                androidx.compose.material3.Text("SeichiCamera v2")
            }
        }
    }
}
