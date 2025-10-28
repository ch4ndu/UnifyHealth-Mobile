@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.mobile.unifyhealth

import androidx.compose.runtime.MutableState
import com.mobile.unifyhealth.model.HealthData
import com.mobile.unifyhealth.model.HealthDataType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

expect class HealthService {
    val isPermissionSheetVisible: StateFlow<Boolean>
    val permissionsToRequest: Set<String>
    fun showPermissionSheet()
    fun hidePermissionSheet()
    suspend fun isAvailable(): Boolean
    val permissionsAvailable: MutableStateFlow<Boolean>
    suspend fun hasPermissions(readTypes: Set<HealthDataType>): Boolean
    suspend fun requestAuthorization(readTypes: Set<HealthDataType>)
    suspend fun readData(
        startTime: Instant,
        endTime: Instant,
        type: HealthDataType
    ): List<HealthData>
}