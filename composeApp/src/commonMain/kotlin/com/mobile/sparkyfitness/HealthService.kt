@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.mobile.sparkyfitness

import com.mobile.sparkyfitness.model.HealthData
import com.mobile.sparkyfitness.model.HealthDataType
import kotlinx.datetime.Instant

expect class HealthService {
    val isPermissionSheetVisible: Boolean
    fun showPermissionSheet()
    fun hidePermissionSheet()
    suspend fun isAvailable(): Boolean
    suspend fun hasPermissions(readTypes: Set<HealthDataType>): Boolean
    suspend fun requestAuthorization(readTypes: Set<HealthDataType>)
    suspend fun readData(
        startTime: Instant,
        endTime: Instant,
        type: HealthDataType
    ): List<HealthData>
}