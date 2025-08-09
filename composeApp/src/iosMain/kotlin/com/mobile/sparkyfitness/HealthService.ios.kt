@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.mobile.sparkyfitness

import com.mobile.sparkyfitness.model.HealthData
import com.mobile.sparkyfitness.model.HealthDataType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSPredicate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.HealthKit.HKCategorySample
import platform.HealthKit.HKCorrelation
import platform.HealthKit.HKCorrelationQuery
import platform.HealthKit.HKCorrelationTypeIdentifierBloodPressure
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectQueryNoLimit
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantitySample
import platform.HealthKit.HKQuery
import platform.HealthKit.HKQueryOptionStrictStartDate
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.predicateForSamplesWithStartDate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
actual class HealthService {
    private val healthStore = HKHealthStore()

    actual val isPermissionSheetVisible: StateFlow<Boolean> = MutableStateFlow(false)

    actual fun showPermissionSheet() {}
    actual fun hidePermissionSheet() {}

    actual suspend fun isAvailable(): Boolean = HKHealthStore.isHealthDataAvailable()

    actual suspend fun hasPermissions(readTypes: Set<HealthDataType>): Boolean {
        // On iOS, we can't reliably check permissions beforehand.
        // We request authorization, and the system decides whether to show a prompt.
        return true
    }

    actual suspend fun requestAuthorization(readTypes: Set<HealthDataType>) {
        val allTypes = readTypes.mapNotNull { it.toHkObjectType() }.toSet()
        if (allTypes.isEmpty()) return

        return suspendCoroutine { continuation ->
            healthStore.requestAuthorizationToShareTypes(null, allTypes) { success, error ->
                if (error != null) {
                    println("HealthKit Authorization Error: ${error.localizedDescription}")
                }
                continuation.resume(Unit)
            }
        }
    }

    actual suspend fun readData(
        startTime: Instant,
        endTime: Instant,
        type: HealthDataType
    ): List<HealthData> {
        val startDate = NSDate.dateWithTimeIntervalSince1970(startTime.epochSeconds.toDouble())
        val endDate = NSDate.dateWithTimeIntervalSince1970(endTime.epochSeconds.toDouble())
        val predicate = HKQuery.predicateForSamplesWithStartDate(
            startDate,
            endDate,
            HKQueryOptionStrictStartDate
        )

        return when (type.getCategory()) {
            HealthDataTypeCategory.QUANTITY -> readQuantityData(type, predicate)
            HealthDataTypeCategory.CATEGORY -> readCategoryData(type, predicate)
            HealthDataTypeCategory.CORRELATION -> readCorrelationData(type, predicate)
        }
    }

    private suspend fun readQuantityData(
        type: HealthDataType,
        predicate: NSPredicate
    ): List<HealthData> {
        val quantityTypeIdentifier = type.toHkQuantityTypeIdentifier() ?: return emptyList()
        val quantityType =
            HKObjectType.quantityTypeForIdentifier(quantityTypeIdentifier) ?: return emptyList()

        return suspendCoroutine { continuation ->
            val query = HKSampleQuery(
                quantityType,
                predicate,
                HKObjectQueryNoLimit,
                null
            ) { _, samples, error ->
                if (error != null) {
                    continuation.resume(emptyList())
                    return@HKSampleQuery
                }
                val healthDataList =
                    samples?.mapNotNull { (it as? HKQuantitySample)?.toHealthData(type) }
                        ?: emptyList()
                continuation.resume(healthDataList)
            }
            healthStore.executeQuery(query)
        }
    }

    private suspend fun readCategoryData(
        type: HealthDataType,
        predicate: NSPredicate
    ): List<HealthData> {
        val categoryTypeIdentifier = type.toHkCategoryTypeIdentifier() ?: return emptyList()
        val categoryType =
            HKObjectType.categoryTypeForIdentifier(categoryTypeIdentifier) ?: return emptyList()

        return suspendCoroutine { continuation ->
            val query = HKSampleQuery(
                categoryType,
                predicate,
                HKObjectQueryNoLimit,
                null
            ) { _, samples, error ->
                if (error != null) {
                    continuation.resume(emptyList())
                    return@HKSampleQuery
                }
                val healthDataList =
                    samples?.mapNotNull { (it as? HKCategorySample)?.toHealthData(type) }
                        ?: emptyList()
                continuation.resume(healthDataList)
            }
            healthStore.executeQuery(query)
        }
    }

    private suspend fun readCorrelationData(
        type: HealthDataType,
        predicate: NSPredicate
    ): List<HealthData> {
        val correlationTypeIdentifier = when (type) {
            HealthDataType.BLOOD_PRESSURE -> HKCorrelationTypeIdentifierBloodPressure
            else -> return emptyList()
        }
        val correlationType = HKObjectType.correlationTypeForIdentifier(correlationTypeIdentifier)
            ?: return emptyList()

        return suspendCoroutine { continuation ->
            val query =
                HKCorrelationQuery(correlationType, predicate, null) { _, correlations, error ->
                    if (error != null) {
                        continuation.resume(emptyList())
                        return@HKCorrelationQuery
                    }
                    val healthDataList =
                        correlations?.mapNotNull { (it as? HKCorrelation)?.toHealthData(type) }
                            ?: emptyList()
                    continuation.resume(healthDataList)
                }
            healthStore.executeQuery(query)
        }
    }

    actual val permissionsToRequest: Set<String>
        get() = emptySet()
}