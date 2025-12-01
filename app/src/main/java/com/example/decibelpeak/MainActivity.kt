package com.example.decibelpeak

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.decibelpeak.ui.components.CircularGaugeView
import com.example.decibelpeak.ui.components.RecordButton
import com.example.decibelpeak.ui.components.SoundLevelIndicator
import com.example.decibelpeak.ui.components.VisualizationCarousel
import com.example.decibelpeak.ui.theme.BackgroundDark
import com.example.decibelpeak.ui.theme.BackgroundDarker
import com.example.decibelpeak.ui.theme.DecibelPeakTheme
import com.example.decibelpeak.ui.theme.SpeakerLoud
import com.example.decibelpeak.ui.theme.SpeakerModerate
import com.example.decibelpeak.ui.theme.SpeakerQuiet
import com.example.decibelpeak.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DecibelPeakTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val isRecording by viewModel.isRecording.collectAsState()
    val decibelLevel by viewModel.decibelLevel.collectAsState()
    val waveformSamples by viewModel.waveformSamples.collectAsState()
    val frequencyBands by viewModel.frequencyBands.collectAsState()
    val waterfallData by viewModel.waterfallData.collectAsState()
    val dbHistory by viewModel.dbHistory.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BackgroundDark, BackgroundDarker)
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Decibel Peak",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sound Level Monitor",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Visualization Carousel
                VisualizationCarousel(
                    viewModel = viewModel,
                    isRecording = isRecording,
                    decibelLevel = decibelLevel,
                    waveformSamples = waveformSamples,
                    frequencyBands = frequencyBands,
                    waterfallData = waterfallData,
                    dbHistory = dbHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Take available space
                )

                if (!isLandscape) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Gauge (Portrait only)
                    CircularGaugeView(
                        value = decibelLevel,
                        isRecording = isRecording,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sound Level Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SoundLevelIndicator(
                        icon = Icons.Default.Speaker,
                        label = "Quiet",
                        range = "20-60 dB",
                        color = SpeakerQuiet,
                        isActive = decibelLevel < 60
                    )
                    SoundLevelIndicator(
                        icon = Icons.Default.VolumeUp,
                        label = "Moderate",
                        range = "60-85 dB",
                        color = SpeakerModerate,
                        isActive = decibelLevel >= 60 && decibelLevel < 85
                    )
                    SoundLevelIndicator(
                        icon = Icons.Default.SpeakerGroup,
                        label = "Loud",
                        range = "85+ dB",
                        color = SpeakerLoud,
                        isActive = decibelLevel >= 85
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Control Button
                RecordButton(
                    isRecording = isRecording,
                    onClick = {
                        if (permissionState.status.isGranted) {
                            viewModel.toggleRecording()
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}