package com.mobile.sparkyfitness

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.mobile.sparkyfitness.model.HealthData
import com.mobile.sparkyfitness.model.HealthDataType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlin.reflect.KClass

class HealthConnectProvider(context: Context) : HealthDataProvider {

    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(
            context
        )
    }

    private var _isPermissionSheetVisible = MutableStateFlow(false)
    override val isPermissionSheetVisible = _isPermissionSheetVisible

    private var _permissionsToRequest = mutableStateOf<Set<String>>(emptySet())
    override val permissionsToRequest: Set<String> get() = _permissionsToRequest.value

    override fun showPermissionSheet() {
        _isPermissionSheetVisible.value = true
    }

    override fun hidePermissionSheet() {
        _isPermissionSheetVisible.value = false
    }

    override suspend fun connect(): Boolean =
        true // Health Connect doesn't have an explicit connect step

    override suspend fun hasPermissions(readTypes: Set<HealthDataType>): Boolean {
        val permissions = readTypes.mapNotNull { it.toHealthPermission() }.toSet()
        return healthConnectClient.permissionController.getGrantedPermissions()
            .containsAll(permissions)
    }

    override suspend fun requestAuthorization(readTypes: Set<HealthDataType>) {
        val permissions = readTypes.mapNotNull { it.toHealthPermission() }.toSet()
        _permissionsToRequest.value = permissions
        showPermissionSheet() // Signal the UI to launch the contract
    }

    override suspend fun readData(
        startTime: Instant,
        endTime: Instant,
        type: HealthDataType
    ): List<HealthData> {
        // Special handling for MOVE_MINUTES as it's derived from another type
        if (type == HealthDataType.MOVE_MINUTES) {
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime.toJavaInstant(),
                    endTime.toJavaInstant()
                )
            )
            return try {
                val response = healthConnectClient.readRecords(request)
                // Sum up all exercise durations to represent "Move Minutes"
                val totalMinutes = response.records.sumOf {
                    java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                }
                if (totalMinutes > 0) {
                    listOf(HealthData.MoveMinutes(totalMinutes.toInt(), startTime, endTime))
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val recordClass = type.toRecordClass() ?: return emptyList()
        val request = ReadRecordsRequest(
            recordType = recordClass,
            timeRangeFilter = TimeRangeFilter.between(
                startTime.toJavaInstant(),
                endTime.toJavaInstant()
            )
        )
        return try {
            val response = healthConnectClient.readRecords(request)
            response.records.mapNotNull { it.toSharedModel() }
        } catch (e: Exception) {
            // Log error
            emptyList()
        }
    }


    private fun HealthDataType.toHealthPermission(): String? {
        val recordClass = this.toRecordClass() ?: return null
        return HealthPermission.getReadPermission(recordClass)
    }

    private fun HealthDataType.toRecordClass(): KClass<out androidx.health.connect.client.records.Record>? {
        return when (this) {
            HealthDataType.STEPS -> StepsRecord::class
            HealthDataType.DISTANCE -> DistanceRecord::class
            HealthDataType.FLOORS_CLIMBED -> FloorsClimbedRecord::class
            HealthDataType.ACTIVE_ENERGY_BURNED -> ActiveCaloriesBurnedRecord::class
            HealthDataType.BASAL_ENERGY_BURNED -> BasalMetabolicRateRecord::class
            HealthDataType.EXERCISE -> ExerciseSessionRecord::class
            HealthDataType.WEIGHT -> WeightRecord::class
            HealthDataType.HEIGHT -> HeightRecord::class
            HealthDataType.BODY_FAT_PERCENTAGE -> BodyFatRecord::class
            HealthDataType.LEAN_BODY_MASS -> LeanBodyMassRecord::class
            HealthDataType.BODY_TEMPERATURE -> BodyTemperatureRecord::class
            HealthDataType.HEART_RATE -> HeartRateRecord::class
            HealthDataType.HEART_RATE_VARIABILITY -> HeartRateVariabilityRmssdRecord::class
            HealthDataType.BLOOD_PRESSURE -> BloodPressureRecord::class
            HealthDataType.BLOOD_GLUCOSE -> BloodGlucoseRecord::class
            HealthDataType.OXYGEN_SATURATION -> OxygenSaturationRecord::class
            HealthDataType.RESPIRATORY_RATE -> RespiratoryRateRecord::class
            HealthDataType.VO2_MAX -> Vo2MaxRecord::class
            HealthDataType.SLEEP -> SleepSessionRecord::class
            HealthDataType.WATER -> HydrationRecord::class
            HealthDataType.NUTRITION -> NutritionRecord::class
            HealthDataType.MENSTRUATION -> MenstruationFlowRecord::class
            HealthDataType.OVULATION_TEST -> OvulationTestRecord::class
            HealthDataType.CERVICAL_MUCUS -> CervicalMucusRecord::class
            HealthDataType.SEXUAL_ACTIVITY -> SexualActivityRecord::class
            HealthDataType.INTERMENSTRUAL_BLEEDING -> IntermenstrualBleedingRecord::class
            // Corrected: MOVE_MINUTES is derived from Exercise, so it needs that permission.
            HealthDataType.MOVE_MINUTES -> ExerciseSessionRecord::class
            // Corrected: BMI is calculated, not read directly.
            HealthDataType.BODY_MASS_INDEX -> null
        }
    }

    private fun androidx.health.connect.client.records.Record.toSharedModel(): HealthData? {
        return when (this) {
            is StepsRecord -> HealthData.Steps(
                count,
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )

            is DistanceRecord -> HealthData.Distance(
                distance.inMeters,
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )

            is FloorsClimbedRecord -> HealthData.FloorsClimbed(
                floors,
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )

            is ActiveCaloriesBurnedRecord -> HealthData.ActiveEnergyBurned(
                energy.inKilocalories,
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )

            is BasalMetabolicRateRecord -> HealthData.BasalMetabolicRate(
                basalMetabolicRate.inKilocaloriesPerDay,
                time.toKotlinInstant()
            )

            is ExerciseSessionRecord -> HealthData.ExerciseSession(
                exerciseType.toString(),
                java.time.Duration.between(startTime, endTime).toMinutes(),
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )

            is WeightRecord -> HealthData.Weight(weight.inKilograms, time.toKotlinInstant())
            is HeightRecord -> HealthData.Height(height.inMeters, time.toKotlinInstant())
            is BodyFatRecord -> HealthData.BodyFatPercentage(
                percentage.value,
                time.toKotlinInstant()
            )

            is LeanBodyMassRecord -> HealthData.LeanBodyMass(
                mass.inKilograms,
                time.toKotlinInstant()
            )

            is BodyTemperatureRecord -> HealthData.BodyTemperature(
                temperature.inCelsius,
                time.toKotlinInstant()
            )

            is HeartRateRecord -> HealthData.HeartRate(
                samples.firstOrNull()?.beatsPerMinute ?: 0L,
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )

            is HeartRateVariabilityRmssdRecord -> HealthData.HeartRateVariability(
                heartRateVariabilityMillis,
                time.toKotlinInstant()
            )

            is BloodPressureRecord -> HealthData.BloodPressure(
                systolic.inMillimetersOfMercury,
                diastolic.inMillimetersOfMercury,
                time.toKotlinInstant()
            )

            is BloodGlucoseRecord -> HealthData.BloodGlucose(
                level.inMilligramsPerDeciliter,
                time.toKotlinInstant()
            )

            is OxygenSaturationRecord -> HealthData.OxygenSaturation(
                percentage.value,
                time.toKotlinInstant()
            )

            is RespiratoryRateRecord -> HealthData.RespiratoryRate(rate, time.toKotlinInstant())
            is Vo2MaxRecord -> HealthData.Vo2Max(
                vo2MillilitersPerMinuteKilogram,
                time.toKotlinInstant()
            )

            is SleepSessionRecord -> HealthData.SleepSession(
                java.time.Duration.between(
                    startTime,
                    endTime
                ).toMinutes(), startTime.toKotlinInstant(), endTime.toKotlinInstant()
            )

            is HydrationRecord -> HealthData.Water(
                volume.inLiters,
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )

            is NutritionRecord -> HealthData.Nutrition(
                energy?.inKilocalories,
                protein?.inGrams,
                totalFat?.inGrams,
                totalCarbohydrate?.inGrams,
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )

            is MenstruationFlowRecord -> HealthData.Menstruation(
                time.toKotlinInstant(),
                flow
            )

            is OvulationTestRecord -> HealthData.OvulationTest(
                result.toString(),
                time.toKotlinInstant()
            )

            is CervicalMucusRecord -> HealthData.CervicalMucus(
                toString(),
                time.toKotlinInstant()
            )

            is SexualActivityRecord -> HealthData.SexualActivity(time.toKotlinInstant())
            is IntermenstrualBleedingRecord -> HealthData.IntermenstrualBleeding(time.toKotlinInstant())
            else -> null
        }
    }

    private fun Instant.toJavaInstant(): java.time.Instant =
        java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())

    private fun java.time.Instant.toKotlinInstant(): Instant =
        Instant.fromEpochMilliseconds(this.toEpochMilli())
}