package dev.andrefrelicot.decibelpeak.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andrefrelicot.decibelpeak.model.TimestampedDbValue
import dev.andrefrelicot.decibelpeak.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VisualizationCarousel(
    viewModel: MainViewModel,
    isRecording: Boolean,
    decibelLevel: Double,
    waveformSamples: List<Float>,
    frequencyBands: List<Float>,
    waterfallData: List<List<Float>>,
    dbHistory: List<Double>,
    timestampedDbHistory: List<TimestampedDbValue>,
    modifier: Modifier = Modifier
) {
    val visualizations = listOf(
        "Waveform",
        "Spectrum",
        "FFT Bars",
        "FFT Circle",
        "Waterfall",
        "dB Curve"
    )
    
    val pagerState = rememberPagerState(pageCount = { visualizations.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = visualizations[pagerState.currentPage],
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // Pager Indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(visualizations.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(iteration)
                                }
                            }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Visualization Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            if (isRecording) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> WaveformView(samples = waveformSamples)
                        1 -> SpectrumView(samples = waveformSamples)
                        2 -> FFTSpectrumView(frequencyBands = frequencyBands)
                        3 -> FFTCircularView(frequencyBands = frequencyBands)
                        4 -> FFTWaterfallView(waterfallData = waterfallData)
                        5 -> DbCurveView(
                            timestampedDbHistory = timestampedDbHistory,
                            dbHistory = dbHistory
                        )
                    }
                }
            } else {
                // Placeholder when not recording
                 HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // Show empty grid or placeholder
                    Box(modifier = Modifier.fillMaxSize()) // Or draw empty grid
                }
            }
        }
    }
}
