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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.decibelpeak.ui.components.CircularGaugeView
import com.example.decibelpeak.ui.components.FFTSpectrumView
import com.example.decibelpeak.ui.components.FFTWaterfallView
import com.example.decibelpeak.ui.components.RecordButton
import com.example.decibelpeak.ui.components.SoundLevelIndicator
import com.example.decibelpeak.ui.components.WaveformView
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

                // Visualization Area (Carousel placeholder for now, showing Spectrum)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.White.copy(alpha = 0.05f), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    if (isRecording) {
                        // Simple toggle for demo purposes or just show one
                        // For now, let's overlay them or pick one. 
                        // Let's show Spectrum as default
                        FFTSpectrumView(frequencyBands = frequencyBands)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Gauge
                CircularGaugeView(
                    value = decibelLevel,
                    isRecording = isRecording,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.weight(1f))

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