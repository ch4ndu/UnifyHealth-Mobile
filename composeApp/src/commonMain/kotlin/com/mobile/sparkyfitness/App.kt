@file:OptIn(ExperimentalMaterial3Api::class)

package com.mobile.sparkyfitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.sparkyfitness.model.HealthData
import com.mobile.sparkyfitness.model.HealthDataType
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

@Composable
fun App(healthService: HealthService) {
    val coroutineScope = rememberCoroutineScope()
    var isAvailable by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    var healthData by remember { mutableStateOf<List<HealthData>>(emptyList()) }
    var message by remember { mutableStateOf("Welcome to Health App!") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        isAvailable = healthService.isAvailable()
        if (isAvailable) {
            // On iOS, we can't check permissions, so we just request them.
            // On Android, this check is more meaningful.
            hasPermissions = healthService.hasPermissions(HealthDataType.entries.toSet())
            message = if (hasPermissions) "Ready to fetch data." else "Please grant permissions."
        } else {
            message = "Health service not available on this device."
        }
        isLoading = false
    }

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("KMP Native Health Tracker") }) }) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                if (isLoading) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                Spacer(Modifier.height(16.dp))

                if (isAvailable && !hasPermissions) {
                    Button(onClick = {
                        coroutineScope.launch {
                            healthService.requestAuthorization(HealthDataType.values().toSet())
                            // After requesting, we assume we have them for UI purposes.
                            hasPermissions = true
                            message = "Permissions requested. You can now try fetching data."
                        }
                    }) {
                        Text("Request All Health Permissions")
                    }
                }

                if (hasPermissions) {
                    DataTypeGrid(healthService, onLoading = { isLoading = it }) { data, msg ->
                        healthData = data
                        message = msg
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(healthData) { dataPoint ->
                        HealthDataCard(dataPoint)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DataTypeGrid(
    healthService: HealthService,
    onLoading: (Boolean) -> Unit,
    onResult: (List<HealthData>, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HealthDataType.values()) { type ->
            Button(
                modifier = Modifier.height(60.dp),
                onClick = {
                    coroutineScope.launch {
                        onLoading(true)
                        onResult(emptyList(), "Fetching ${type.name}...")
                        val now = Clock.System.now()
                        val startTime = now.minus(1.days)
                        val data = healthService.readData(startTime, now, type)
                        if (data.isNotEmpty()) {
                            onResult(data, "Fetched ${data.size} ${type.name} records.")
                        } else {
                            onResult(
                                emptyList(),
                                "No ${type.name} data found for the last 24 hours."
                            )
                        }
                        onLoading(false)
                    }
                }
            ) {
                Text(
                    type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HealthDataCard(data: HealthData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = data::class.simpleName ?: "Health Data"
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Display logic for all new types
            when (data) {
                is HealthData.Steps -> Text("Count: ${data.count}")
                is HealthData.Distance -> Text("Meters: ${data.meters}")
                is HealthData.FloorsClimbed -> Text("Floors: ${data.floors}")
                is HealthData.ActiveEnergyBurned -> Text("Active Calories: ${data.calories}")
                is HealthData.MoveMinutes -> Text("Total Exercise Minutes: ${data.minutes}")
                is HealthData.ExerciseSession -> Text("Duration: ${data.durationMinutes} min")
                is HealthData.Weight -> Text("Kilograms: ${data.kilograms}")
                is HealthData.Height -> Text("Meters: ${data.meters}")
                is HealthData.BodyFatPercentage -> Text("Percentage: ${data.percentage}%")
                is HealthData.LeanBodyMass -> Text("Kilograms: ${data.kilograms}")
                is HealthData.BodyMassIndex -> Text("BMI: ${data.value}")
                is HealthData.BodyTemperature -> Text("Celsius: ${data.celsius}")
                is HealthData.BasalMetabolicRate -> Text("Rate: ${data.kcalPerDay} kcal/day")
                is HealthData.HeartRate -> Text("BPM: ${data.bpm}", color = Color.Red)
                is HealthData.HeartRateVariability -> Text("RMSSD (ms): ${data.ms}")
                is HealthData.BloodPressure -> Text("Systolic: ${data.systolicMmhg}, Diastolic: ${data.diastolicMmhg}")
                is HealthData.BloodGlucose -> Text("mg/dL: ${data.mgdl}")
                is HealthData.OxygenSaturation -> Text("Saturation: ${data.percentage}%")
                is HealthData.RespiratoryRate -> Text("Breaths/min: ${data.rpm}")
                is HealthData.Vo2Max -> Text("ml/kg/min: ${data.mlPerKgPerMin}")
                is HealthData.SleepSession -> Text(
                    "Duration: ${data.durationMinutes} min",
                    color = Color.Blue
                )

                is HealthData.Water -> Text("Liters: ${data.liters}")
                is HealthData.Nutrition -> Text("Calories: ${data.calories ?: "N/A"}, Protein: ${data.proteinGrams ?: "N/A"}g")
                is HealthData.Menstruation -> Text("Recorded Menstrual Flow")
                is HealthData.OvulationTest -> Text("Result: ${data.result}")
                is HealthData.CervicalMucus -> Text("Quality: ${data.quality}")
                is HealthData.SexualActivity -> Text("Recorded Sexual Activity")
                is HealthData.IntermenstrualBleeding -> Text("Recorded Intermenstrual Bleeding")
            }

            val displayTime = data.time ?: data.startTime
            if (displayTime != null) {
                Text("Time: $displayTime", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}