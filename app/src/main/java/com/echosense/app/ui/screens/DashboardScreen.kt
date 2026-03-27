package com.echosense.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echosense.app.ui.DashboardViewModel
import com.echosense.app.ui.components.ParamSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToHistory: () -> Unit
) {
    val preAmp by viewModel.preAmpGain.collectAsState()
    val voiceBoost by viewModel.voiceBoost.collectAsState()
    val masterGain by viewModel.masterGain.collectAsState()
    val noiseGate by viewModel.noiseGate.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("EchoSense Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Audio Processing",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ParamSlider(
                        label = "Pre-Amp Gain",
                        value = preAmp,
                        onValueChange = { viewModel.updatePreAmp(it) },
                        valueRange = 0.5f..4.0f
                    )
                    
                    ParamSlider(
                        label = "Voice Boost",
                        value = voiceBoost,
                        onValueChange = { viewModel.updateVoiceBoost(it) },
                        valueRange = -10.0f..20.0f,
                        displayValue = "${voiceBoost.toString().take(4)} dB"
                    )

                    ParamSlider(
                        label = "Noise Gate",
                        value = noiseGate,
                        onValueChange = { viewModel.updateNoiseGate(it) },
                        valueRange = -100.0f..0.0f,
                        displayValue = "${noiseGate.toString().take(5)} dB"
                    )

                    ParamSlider(
                        label = "Master Gain",
                        value = masterGain,
                        onValueChange = { viewModel.updateMasterGain(it) },
                        valueRange = 0.0f..2.0f
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Placeholder for Visualizer integration
            Text(
                text = "Live Visualization",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                // We will wrap the existing FrequencyVisualizerView here later
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Visualizer Placeholder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
