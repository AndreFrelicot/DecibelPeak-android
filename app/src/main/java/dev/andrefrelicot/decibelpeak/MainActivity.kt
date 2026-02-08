package dev.andrefrelicot.decibelpeak

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.andrefrelicot.decibelpeak.ui.components.AnimatedNumber
import dev.andrefrelicot.decibelpeak.ui.components.CalibrationOverlay
import dev.andrefrelicot.decibelpeak.ui.components.CircularGaugeView
import dev.andrefrelicot.decibelpeak.ui.components.LandscapeControlButton
import dev.andrefrelicot.decibelpeak.ui.components.RecordButton
import dev.andrefrelicot.decibelpeak.ui.components.SoundLevelIndicator
import dev.andrefrelicot.decibelpeak.ui.components.VisualizationCarousel
import dev.andrefrelicot.decibelpeak.ui.components.getDecibelColor
import dev.andrefrelicot.decibelpeak.ui.theme.BackgroundDark
import dev.andrefrelicot.decibelpeak.ui.theme.BackgroundDarker
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelPeakTheme
import dev.andrefrelicot.decibelpeak.ui.theme.SpeakerLoud
import dev.andrefrelicot.decibelpeak.ui.theme.SpeakerModerate
import dev.andrefrelicot.decibelpeak.ui.theme.SpeakerQuiet
import dev.andrefrelicot.decibelpeak.viewmodel.MainViewModel
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.SharedFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
    val timestampedDbHistory by viewModel.timestampedDbHistory.collectAsState()
    val dbPeakData by viewModel.dbPeakData.collectAsState()
    val dbPeakValue by viewModel.dbPeakValue.collectAsState()
    val dbPeakTimeMillis by viewModel.dbPeakTimeMillis.collectAsState()
    val selectedVisualization by viewModel.selectedVisualization.collectAsState()
    val showCalibrationOverlay by viewModel.showCalibrationOverlay.collectAsState()
    val tempCalibrationOffset by viewModel.tempCalibrationOffset.collectAsState()
    val hapticFeedbackEvent = viewModel.hapticFeedbackEvent

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val activity = context as? Activity

    // Request permission on launch
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    // Auto-start recording when permission is granted
    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted && !isRecording) {
            viewModel.toggleRecording()
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
            if (isLandscape) {
                // LANDSCAPE LAYOUT (matching iOS)
                LandscapeLayout(
                    viewModel = viewModel,
                    isRecording = isRecording,
                    decibelLevel = decibelLevel,
                    waveformSamples = waveformSamples,
                    frequencyBands = frequencyBands,
                    waterfallData = waterfallData,
                    dbHistory = dbHistory,
                    timestampedDbHistory = timestampedDbHistory,
                    dbPeakData = dbPeakData,
                    dbPeakValue = dbPeakValue,
                    dbPeakTimeMillis = dbPeakTimeMillis,
                    selectedVisualization = selectedVisualization,
                    showCalibrationOverlay = showCalibrationOverlay,
                    tempCalibrationOffset = tempCalibrationOffset,
                    hapticFeedbackEvent = hapticFeedbackEvent,
                    onToggleRecording = {
                        if (permissionState.status.isGranted) {
                            viewModel.toggleRecording()
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    },
                    onCalibrationClick = { viewModel.showCalibration() },
                    onCalibrationOffsetChange = { viewModel.setTempCalibrationOffset(it) },
                    onCalibrationCancel = { viewModel.cancelCalibration() },
                    onCalibrationSave = { viewModel.saveCalibration() }
                )
            } else {
                // PORTRAIT LAYOUT
                PortraitLayout(
                    viewModel = viewModel,
                    isRecording = isRecording,
                    decibelLevel = decibelLevel,
                    waveformSamples = waveformSamples,
                    frequencyBands = frequencyBands,
                    waterfallData = waterfallData,
                    dbHistory = dbHistory,
                    timestampedDbHistory = timestampedDbHistory,
                    dbPeakData = dbPeakData,
                    dbPeakValue = dbPeakValue,
                    dbPeakTimeMillis = dbPeakTimeMillis,
                    selectedVisualization = selectedVisualization,
                    showCalibrationOverlay = showCalibrationOverlay,
                    tempCalibrationOffset = tempCalibrationOffset,
                    hapticFeedbackEvent = hapticFeedbackEvent,
                    onToggleRecording = {
                        if (permissionState.status.isGranted) {
                            viewModel.toggleRecording()
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    },
                    onCalibrationClick = { viewModel.showCalibration() },
                    onCalibrationOffsetChange = { viewModel.setTempCalibrationOffset(it) },
                    onCalibrationCancel = { viewModel.cancelCalibration() },
                    onCalibrationSave = { viewModel.saveCalibration() }
                )
            }

            // Rotation toggle button (top right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable {
                        activity?.requestedOrientation = if (isLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = stringResource(R.string.toggle_orientation),
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Landscape layout matching iOS horizontal mode
 */
@Composable
private fun LandscapeLayout(
    viewModel: MainViewModel,
    isRecording: Boolean,
    decibelLevel: Double,
    waveformSamples: List<Float>,
    frequencyBands: List<Float>,
    waterfallData: List<List<Float>>,
    dbHistory: List<Double>,
    timestampedDbHistory: List<dev.andrefrelicot.decibelpeak.model.TimestampedDbValue>,
    dbPeakData: List<dev.andrefrelicot.decibelpeak.model.DbPeakDataPoint>,
    dbPeakValue: Double,
    dbPeakTimeMillis: Long,
    selectedVisualization: Int,
    showCalibrationOverlay: Boolean,
    tempCalibrationOffset: Double,
    hapticFeedbackEvent: SharedFlow<Unit>,
    onToggleRecording: () -> Unit,
    onCalibrationClick: () -> Unit,
    onCalibrationOffsetChange: (Double) -> Unit,
    onCalibrationCancel: () -> Unit,
    onCalibrationSave: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content - full screen visualization
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // dB value at top center
            Box(
                modifier = Modifier.padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                LandscapeDecibelDisplay(
                    decibelLevel = decibelLevel,
                    isRecording = isRecording
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full-screen visualization carousel with calibration overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                VisualizationCarousel(
                    viewModel = viewModel,
                    isRecording = isRecording,
                    decibelLevel = decibelLevel,
                    waveformSamples = waveformSamples,
                    frequencyBands = frequencyBands,
                    waterfallData = waterfallData,
                    dbHistory = dbHistory,
                    timestampedDbHistory = timestampedDbHistory,
                    dbPeakData = dbPeakData,
                    dbPeakValue = dbPeakValue,
                    dbPeakTimeMillis = dbPeakTimeMillis,
                    selectedVisualization = selectedVisualization,
                    modifier = Modifier.fillMaxSize()
                )

                // Calibration overlay on top of visualization (landscape)
                if (showCalibrationOverlay) {
                    CalibrationOverlay(
                        currentOffset = tempCalibrationOffset,
                        onOffsetChange = onCalibrationOffsetChange,
                        onCancel = onCalibrationCancel,
                        onValidate = onCalibrationSave,
                        hapticFeedbackEvent = hapticFeedbackEvent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Calibration button at top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .clickable { onCalibrationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = stringResource(R.string.calibration),
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Control button at bottom right (hidden when calibration overlay is shown)
        AnimatedVisibility(
            visible = !showCalibrationOverlay,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 30.dp, bottom = 30.dp)
        ) {
            LandscapeControlButton(
                isRecording = isRecording,
                onClick = onToggleRecording
            )
        }
    }
}

/**
 * Floating dB display for landscape mode (matching iOS)
 * Uses AnimatedNumber component for per-digit animation like portrait mode
 */
@Composable
private fun LandscapeDecibelDisplay(
    decibelLevel: Double,
    isRecording: Boolean
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        // Animated dB number using same component as portrait gauge
        if (isRecording && decibelLevel > 0) {
            AnimatedNumber(
                number = decibelLevel.toInt(),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = getDecibelColor(decibelLevel)
            )
        } else {
            Text(
                text = stringResource(R.string.not_monitoring),
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // dB unit label (aligned to bottom of the number)
        Text(
            text = stringResource(R.string.db_unit),
            color = if (isRecording) Color.Gray else Color.Gray.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

/**
 * Portrait layout (original layout)
 */
@Composable
private fun PortraitLayout(
    viewModel: MainViewModel,
    isRecording: Boolean,
    decibelLevel: Double,
    waveformSamples: List<Float>,
    frequencyBands: List<Float>,
    waterfallData: List<List<Float>>,
    dbHistory: List<Double>,
    timestampedDbHistory: List<dev.andrefrelicot.decibelpeak.model.TimestampedDbValue>,
    dbPeakData: List<dev.andrefrelicot.decibelpeak.model.DbPeakDataPoint>,
    dbPeakValue: Double,
    dbPeakTimeMillis: Long,
    selectedVisualization: Int,
    showCalibrationOverlay: Boolean,
    tempCalibrationOffset: Double,
    hapticFeedbackEvent: SharedFlow<Unit>,
    onToggleRecording: () -> Unit,
    onCalibrationClick: () -> Unit,
    onCalibrationOffsetChange: (Double) -> Unit,
    onCalibrationCancel: () -> Unit,
    onCalibrationSave: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = stringResource(R.string.decibel_peak),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.sound_level_monitor),
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Visualization Carousel with calibration overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                VisualizationCarousel(
                    viewModel = viewModel,
                    isRecording = isRecording,
                    decibelLevel = decibelLevel,
                    waveformSamples = waveformSamples,
                    frequencyBands = frequencyBands,
                    waterfallData = waterfallData,
                    dbHistory = dbHistory,
                    timestampedDbHistory = timestampedDbHistory,
                    dbPeakData = dbPeakData,
                    dbPeakValue = dbPeakValue,
                    dbPeakTimeMillis = dbPeakTimeMillis,
                    selectedVisualization = selectedVisualization,
                    modifier = Modifier.fillMaxSize()
                )

                // Calibration overlay on top of visualization (portrait)
                if (showCalibrationOverlay) {
                    CalibrationOverlay(
                        currentOffset = tempCalibrationOffset,
                        onOffsetChange = onCalibrationOffsetChange,
                        onCancel = onCalibrationCancel,
                        onValidate = onCalibrationSave,
                        hapticFeedbackEvent = hapticFeedbackEvent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gauge (Portrait only)
            CircularGaugeView(
                value = decibelLevel,
                isRecording = isRecording,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Sound Level Indicators (icons matching iOS: speaker, speaker.wave.2, speaker.wave.3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SoundLevelIndicator(
                    icon = Icons.AutoMirrored.Filled.VolumeMute,  // Speaker without waves
                    label = stringResource(R.string.level_quiet),
                    range = "20-60 ${stringResource(R.string.db_unit)}",
                    color = SpeakerQuiet,
                    isActive = decibelLevel < 60
                )
                SoundLevelIndicator(
                    icon = Icons.AutoMirrored.Filled.VolumeDown,  // Speaker with 1 wave
                    label = stringResource(R.string.level_moderate),
                    range = "60-85 ${stringResource(R.string.db_unit)}",
                    color = SpeakerModerate,
                    isActive = decibelLevel >= 60 && decibelLevel < 85
                )
                SoundLevelIndicator(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,    // Speaker with 3 waves
                    label = stringResource(R.string.level_loud),
                    range = "85+ ${stringResource(R.string.db_unit)}",
                    color = SpeakerLoud,
                    isActive = decibelLevel >= 85
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Control Button
            RecordButton(
                isRecording = isRecording,
                onClick = onToggleRecording
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Calibration button at top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .clickable { onCalibrationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = stringResource(R.string.calibration),
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}