@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.mobile.unifyhealth

import com.mobile.unifyhealth.model.HealthData
import com.mobile.unifyhealth.model.HealthDataType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSPredicate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.HealthKit.HKCategorySample
import platform.HealthKit.HKCategoryTypeIdentifierSleepAnalysis
import platform.HealthKit.HKCorrelation
import platform.HealthKit.HKCorrelationQuery
import platform.HealthKit.HKCorrelationTypeIdentifierBloodPressure
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectQueryNoLimit
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantitySample
import platform.HealthKit.HKQuantityTypeIdentifierHeartRate
import platform.HealthKit.HKQuery
import platform.HealthKit.HKQueryOptionStrictStartDate
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKUnit
import platform.HealthKit.countUnit
import platform.HealthKit.minuteUnit
import platform.HealthKit.predicateForSamplesWithStartDate
import platform.HealthKit.unitDividedByUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
actual class HealthService {
    private val healthStore = HKHealthStore()

    actual val isPermissionSheetVisible: StateFlow<Boolean> = MutableStateFlow(false)
    actual val permissionsToRequest: Set<String> = emptySet()

    actual fun showPermissionSheet() {}
    actual fun hidePermissionSheet() {}

    actual suspend fun isAvailable(): Boolean = HKHealthStore.isHealthDataAvailable()

    actual suspend fun hasPermissions(readTypes: Set<HealthDataType>): Boolean {
        // On iOS, we can't reliably check permissions beforehand.
        // We request authorization, and the system decides whether to show a prompt.
        return true
    }

    actual suspend fun requestAuthorization(readTypes: Set<HealthDataType>) {
        val allTypes = readTypes.flatMap { it.toHkObjectTypes() }.toSet()
        if (allTypes.isEmpty()) return

        return suspendCoroutine { continuation ->
            healthStore.requestAuthorizationToShareTypes(
                typesToShare = null,
                readTypes = allTypes,
                completion = { success, error ->
                    if (error != null) {
                        println("HealthKit Authorization Error: ${error.localizedDescription}")
                    }
                    continuation.resume(Unit)
                }
            )
        }
    }

    actual suspend fun readData(startTime: Instant, endTime: Instant, type: HealthDataType): List<HealthData> {
        val startDate = NSDate.dateWithTimeIntervalSince1970(startTime.epochSeconds.toDouble())
        val endDate = NSDate.dateWithTimeIntervalSince1970(endTime.epochSeconds.toDouble())
        val predicate = HKQuery.predicateForSamplesWithStartDate(startDate, endDate, HKQueryOptionStrictStartDate)

        // Special handling for data types that need aggregation
        if (type == HealthDataType.HEART_RATE) {
            return readHeartRateData(predicate)
        }
        if (type == HealthDataType.SLEEP) {
            return readSleepData(predicate)
        }

        return when (type.getCategory()) {
            HealthDataTypeCategory.QUANTITY -> readQuantityData(type, predicate)
            HealthDataTypeCategory.CATEGORY -> readCategoryData(type, predicate)
            HealthDataTypeCategory.CORRELATION -> readCorrelationData(type, predicate)
        }
    }

    private suspend fun readHeartRateData(predicate: NSPredicate): List<HealthData> {
        val quantityType = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate) ?: return emptyList()

        return suspendCoroutine { continuation ->
            val query = HKSampleQuery(quantityType, predicate, HKObjectQueryNoLimit, null) { _, samples, error ->
                if (error != null) {
                    continuation.resume(emptyList())
                    return@HKSampleQuery
                }
                val hkSamples = samples as? List<HKQuantitySample> ?: emptyList()
                if (hkSamples.isEmpty()) {
                    continuation.resume(emptyList())
                    return@HKSampleQuery
                }

                val heartRateSamples = hkSamples.map {
                    val bpm = it.quantity.doubleValueForUnit(HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit())).toLong()
                    val time = Instant.fromEpochSeconds(it.endDate.timeIntervalSince1970.toLong())
                    HealthData.HeartRateSample(bpm, time)
                }

                // Return a single HealthData.HeartRate object containing all samples
                val result = listOf(HealthData.HeartRate(heartRateSamples, heartRateSamples.last().time))
                continuation.resume(result)
            }
            healthStore.executeQuery(query)
        }
    }

    private suspend fun readSleepData(predicate: NSPredicate): List<HealthData> {
        val categoryType = HKObjectType.categoryTypeForIdentifier(HKCategoryTypeIdentifierSleepAnalysis) ?: return emptyList()

        return suspendCoroutine { continuation ->
            val query = HKSampleQuery(categoryType, predicate, HKObjectQueryNoLimit, null) { _, samples, error ->
                if (error != null || samples == null) {
                    continuation.resume(emptyList())
                    return@HKSampleQuery
                }

                val hkSamples = samples as? List<HKCategorySample> ?: emptyList()
                if (hkSamples.isEmpty()) {
                    continuation.resume(emptyList())
                    return@HKSampleQuery
                }

                // Find the overall start and end times for the entire sleep session
                val sessionStartTime = hkSamples.minOf { it.startDate.timeIntervalSince1970 }
                val sessionEndTime = hkSamples.maxOf { it.endDate.timeIntervalSince1970 }

                val totalDuration = (sessionEndTime - sessionStartTime) / 60

                val stages = hkSamples.mapNotNull { it.toSleepStage() }

                val result = listOf(HealthData.SleepSession(
                    durationMinutes = totalDuration.toLong(),
                    stages = stages,
                    startTime = Instant.fromEpochSeconds(sessionStartTime.toLong()),
                    endTime = Instant.fromEpochSeconds(sessionEndTime.toLong())
                ))

                continuation.resume(result)
            }
            healthStore.executeQuery(query)
        }
    }

    private suspend fun readQuantityData(type: HealthDataType, predicate: NSPredicate): List<HealthData> {
        val quantityTypeIdentifier = type.toHkQuantityTypeIdentifier() ?: return emptyList()
        val quantityType = HKObjectType.quantityTypeForIdentifier(quantityTypeIdentifier) ?: return emptyList()

        return suspendCoroutine { continuation ->
            val query = HKSampleQuery(quantityType, predicate, HKObjectQueryNoLimit, null) { _, samples, error ->
                if (error != null) {
                    continuation.resume(emptyList())
                    return@HKSampleQuery
                }
                val healthDataList = samples?.mapNotNull { (it as? HKQuantitySample)?.toHealthData(type) } ?: emptyList()
                continuation.resume(healthDataList)
            }
            healthStore.executeQuery(query)
        }
    }

    private suspend fun readCategoryData(type: HealthDataType, predicate: NSPredicate): List<HealthData> {
        val categoryTypeIdentifier = type.toHkCategoryTypeIdentifier() ?: return emptyList()
        val categoryType = HKObjectType.categoryTypeForIdentifier(categoryTypeIdentifier) ?: return emptyList()

        return suspendCoroutine { continuation ->
            val query = HKSampleQuery(categoryType, predicate, HKObjectQueryNoLimit, null) { _, samples, error ->
                if (error != null) {
                    continuation.resume(emptyList())
                    return@HKSampleQuery
                }
                val healthDataList = samples?.mapNotNull { (it as? HKCategorySample)?.toHealthData(type) } ?: emptyList()
                continuation.resume(healthDataList)
            }
            healthStore.executeQuery(query)
        }
    }

    private suspend fun readCorrelationData(type: HealthDataType, predicate: NSPredicate): List<HealthData> {
        val correlationTypeIdentifier = when (type) {
            HealthDataType.BLOOD_PRESSURE -> HKCorrelationTypeIdentifierBloodPressure
            else -> return emptyList()
        }
        val correlationType = HKObjectType.correlationTypeForIdentifier(correlationTypeIdentifier) ?: return emptyList()

        return suspendCoroutine { continuation ->
            val query = HKCorrelationQuery(correlationType, predicate, null) { _, correlations, error ->
                if (error != null) {
                    continuation.resume(emptyList())
                    return@HKCorrelationQuery
                }
                val healthDataList = correlations?.mapNotNull { (it as? HKCorrelation)?.toHealthData(type) } ?: emptyList()
                continuation.resume(healthDataList)
            }
            healthStore.executeQuery(query)
        }
    }

    actual val permissionsAvailable: MutableStateFlow<Boolean>
        get() = MutableStateFlow(false)
}