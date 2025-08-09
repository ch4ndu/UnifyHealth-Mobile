package com.mobile.sparkyfitness

import android.app.Activity
import android.content.Context
import android.util.Log
import com.mobile.sparkyfitness.model.HealthData
import com.mobile.sparkyfitness.model.HealthDataType
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult
import com.samsung.android.sdk.healthdata.HealthConstants
import com.samsung.android.sdk.healthdata.HealthDataResolver
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.samsung.android.sdk.healthdata.HealthPermissionManager
import com.samsung.android.sdk.healthdata.HealthResultHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SamsungSdkProvider(private val context: Context) : HealthDataProvider {

    private var healthDataStore: HealthDataStore? = null
    private var isConnected = false

    override val isPermissionSheetVisible = MutableStateFlow(false)
    override val permissionsToRequest: Set<String> = emptySet()
    override fun showPermissionSheet() {}
    override fun hidePermissionSheet() {}

    override suspend fun connect(): Boolean {
        if (isConnected) return true

        return suspendCancellableCoroutine { continuation ->
            val connectionListener = object : HealthDataStore.ConnectionListener {
                override fun onConnected() {
                    println("Samsung HealthDataStore connected")
                    isConnected = true
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onConnectionFailed(e: HealthConnectionErrorResult) {
                    println("Samsung HealthDataStore connection failed: ${e.errorCode}")
                    isConnected = false
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }

                override fun onDisconnected() {
                    println("Samsung HealthDataStore disconnected")
                    isConnected = false
                }
            }
            healthDataStore = HealthDataStore(context, connectionListener)
            try {
                healthDataStore?.connectService()
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }
    }

    override suspend fun hasPermissions(readTypes: Set<HealthDataType>): Boolean {
        if (!isConnected) return false
        val pmsManager = HealthPermissionManager(healthDataStore)
        val permissionKeys = readTypes.mapNotNull { it.toSamsungPermissionKey() }.toSet()

        return suspendCancellableCoroutine { continuation ->
            try {
                val result = pmsManager.isPermissionAcquired(permissionKeys)
                continuation.resume(result.values.all { it == true })
            } catch (e: Exception) {
                continuation.resume(false)
            }
        }
    }

    override suspend fun requestAuthorization(readTypes: Set<HealthDataType>) {
        if (!isConnected) return
//        isPermissionSheetVisible.value = true
        val pmsManager = HealthPermissionManager(healthDataStore)
//        val permissionKeys = readTypes.mapNotNull { it.toSamsungPermissionKey() }.toSet()
        val permissionKeys = listOf(HealthDataType.STEPS, HealthDataType.SLEEP).map { it.toSamsungPermissionKey() }.toSet()

        return suspendCancellableCoroutine { continuation ->
            try {
                pmsManager.requestPermissions(permissionKeys, context as? Activity)
                    .setResultListener { result ->
                        if (result.status == HealthResultHolder.BaseResult.STATUS_SUCCESSFUL) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(RuntimeException("Samsung Health permission request failed"))
                        }
                    }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    override suspend fun readData(
        startTime: Instant,
        endTime: Instant,
        type: HealthDataType
    ): List<HealthData> {
        try {
            val dataType = type.toSamsungDataType() ?: return emptyList()
            val resolver = HealthDataResolver(healthDataStore, null)

            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(dataType)
                .setLocalTimeRange(
                    HealthConstants.StepCount.START_TIME,
                    HealthConstants.StepCount.TIME_OFFSET,
                    startTime.toEpochMilliseconds(),
                    endTime.toEpochMilliseconds()
                )
                .build()

            return suspendCancellableCoroutine { continuation ->
                val resultListener =
                    HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> { result ->
                        try {
                            val list = mutableListOf<HealthData>()
                            result.forEach { cursor ->
                                val healthData = cursor.toHealthData(type)
                                if (healthData != null) {
                                    list.add(healthData)
                                }
                            }
                            continuation.resume(list)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        } finally {
                            result.close()
                        }
                    }
                resolver.read(request).setResultListener(resultListener)
            }

        } catch (exception: SecurityException) {
            Log.e("chandu", "failed to readDate", exception)
            return emptyList()
        }
    }

    private fun HealthDataType.toSamsungPermissionKey(): HealthPermissionManager.PermissionKey? {
        val dataType = this.toSamsungDataType() ?: return null
        return HealthPermissionManager.PermissionKey(
            dataType,
            HealthPermissionManager.PermissionType.READ
        )
    }

    private fun HealthDataType.toSamsungDataType(): String? {
        return when (this) {
            HealthDataType.STEPS -> HealthConstants.StepCount.HEALTH_DATA_TYPE
            HealthDataType.DISTANCE, HealthDataType.ACTIVE_ENERGY_BURNED, HealthDataType.MOVE_MINUTES -> HealthConstants.Exercise.HEALTH_DATA_TYPE
            HealthDataType.FLOORS_CLIMBED -> HealthConstants.FloorsClimbed.HEALTH_DATA_TYPE
            HealthDataType.EXERCISE -> HealthConstants.Exercise.HEALTH_DATA_TYPE
            HealthDataType.WEIGHT -> HealthConstants.Weight.HEALTH_DATA_TYPE
            HealthDataType.HEIGHT -> HealthConstants.Height.HEALTH_DATA_TYPE
            HealthDataType.BODY_FAT_PERCENTAGE -> HealthConstants.BodyFat.HEALTH_DATA_TYPE
            HealthDataType.HEART_RATE -> HealthConstants.HeartRate.HEALTH_DATA_TYPE
            HealthDataType.BLOOD_PRESSURE -> HealthConstants.BloodPressure.HEALTH_DATA_TYPE
            HealthDataType.BLOOD_GLUCOSE -> HealthConstants.BloodGlucose.HEALTH_DATA_TYPE
            HealthDataType.OXYGEN_SATURATION -> HealthConstants.OxygenSaturation.HEALTH_DATA_TYPE
            HealthDataType.SLEEP -> HealthConstants.Sleep.HEALTH_DATA_TYPE
            HealthDataType.WATER -> HealthConstants.WaterIntake.HEALTH_DATA_TYPE
            HealthDataType.NUTRITION -> HealthConstants.Nutrition.HEALTH_DATA_TYPE
            else -> null // Other types are not directly supported by Samsung SDK
        }
    }

    private fun com.samsung.android.sdk.healthdata.HealthData.toHealthData(type: HealthDataType): HealthData? {
        Log.d("chandu", toString())
        val startTime = Instant.fromEpochMilliseconds(getLong(HealthConstants.Common.CREATE_TIME))
        val endTime = Instant.fromEpochMilliseconds(getLong(HealthConstants.Common.UPDATE_TIME))

        return when (type) {
            HealthDataType.STEPS -> HealthData.Steps(
                getLong(HealthConstants.StepCount.COUNT),
                startTime,
                endTime
            )

            HealthDataType.DISTANCE -> HealthData.Distance(
                getFloat(HealthConstants.Exercise.DISTANCE).toDouble(),
                startTime,
                endTime
            )

            HealthDataType.FLOORS_CLIMBED -> HealthData.FloorsClimbed(
                getFloat(HealthConstants.FloorsClimbed.FLOOR).toDouble(),
                startTime,
                endTime
            )

            HealthDataType.ACTIVE_ENERGY_BURNED -> HealthData.ActiveEnergyBurned(
                getFloat(
                    HealthConstants.Exercise.CALORIE
                ).toDouble(), startTime, endTime
            )

            HealthDataType.MOVE_MINUTES -> {
                val duration =
                    (endTime.toEpochMilliseconds() - startTime.toEpochMilliseconds()) / 60000
                HealthData.MoveMinutes(duration.toInt(), startTime, endTime)
            }

            HealthDataType.EXERCISE -> {
                val duration =
                    (endTime.toEpochMilliseconds() - startTime.toEpochMilliseconds()) / 60000
                HealthData.ExerciseSession(
                    getString(HealthConstants.Exercise.EXERCISE_TYPE),
                    duration,
                    startTime,
                    endTime
                )
            }

            HealthDataType.WEIGHT -> HealthData.Weight(
                getFloat(HealthConstants.Weight.WEIGHT).toDouble(),
                endTime
            )

            HealthDataType.HEIGHT -> HealthData.Height(
                getFloat(HealthConstants.Height.HEIGHT).toDouble(),
                endTime
            )

            HealthDataType.BODY_FAT_PERCENTAGE -> HealthData.BodyFatPercentage(
                getFloat(
                    HealthConstants.BodyFat.BODY_FAT
                ).toDouble(), endTime
            )

            HealthDataType.HEART_RATE -> HealthData.HeartRate(
                getLong(HealthConstants.HeartRate.HEART_RATE),
                startTime,
                endTime
            )

            HealthDataType.BLOOD_PRESSURE -> HealthData.BloodPressure(
                systolicMmhg = getFloat(HealthConstants.BloodPressure.SYSTOLIC).toDouble(),
                diastolicMmhg = getFloat(HealthConstants.BloodPressure.DIASTOLIC).toDouble(),
                time = endTime
            )

            HealthDataType.BLOOD_GLUCOSE -> HealthData.BloodGlucose(
                getFloat(HealthConstants.BloodGlucose.GLUCOSE).toDouble(),
                endTime
            )

            HealthDataType.OXYGEN_SATURATION -> HealthData.OxygenSaturation(
                getFloat(HealthConstants.OxygenSaturation.SPO2).toDouble(),
                endTime
            )

            HealthDataType.SLEEP -> {
                val duration =
                    (endTime.toEpochMilliseconds() - startTime.toEpochMilliseconds()) / 60000
                HealthData.SleepSession(duration, startTime, endTime)
            }

            HealthDataType.WATER -> HealthData.Water(
                getFloat(HealthConstants.WaterIntake.AMOUNT).toDouble(),
                startTime,
                endTime
            )

            HealthDataType.NUTRITION -> HealthData.Nutrition(
                calories = getFloat(HealthConstants.Nutrition.CALORIE).toDouble(),
                proteinGrams = getFloat(HealthConstants.Nutrition.PROTEIN).toDouble(),
                fatGrams = getFloat(HealthConstants.Nutrition.TOTAL_FAT).toDouble(),
                carbsGrams = getFloat(HealthConstants.Nutrition.CARBOHYDRATE).toDouble(),
                startTime = startTime,
                endTime = endTime
            )

            else -> null
        }
    }
}