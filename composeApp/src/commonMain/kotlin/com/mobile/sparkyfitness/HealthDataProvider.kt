@file:OptIn(ExperimentalTime::class)

package com.mobile.sparkyfitness

import com.mobile.sparkyfitness.model.HealthData
import com.mobile.sparkyfitness.model.HealthDataType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

interface HealthDataProvider {
    val isPermissionSheetVisible: StateFlow<Boolean>
    val permissionsToRequest: Set<String>
    fun showPermissionSheet()
    fun hidePermissionSheet()
    suspend fun connect(): Boolean
    suspend fun hasPermissions(readTypes: Set<HealthDataType>): Boolean
    suspend fun requestAuthorization(readTypes: Set<HealthDataType>)
    suspend fun readData(
        startTime: Instant,
        endTime: Instant,
        type: HealthDataType
    ): List<HealthData>
}