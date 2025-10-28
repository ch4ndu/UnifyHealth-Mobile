package com.mobile.unifyhealth

import android.content.Context
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
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_AWAKE
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_AWAKE_IN_BED
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_DEEP
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_LIGHT
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_OUT_OF_BED
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_REM
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_SLEEPING
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_UNKNOWN
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.mobile.unifyhealth.model.HealthData
import com.mobile.unifyhealth.model.HealthDataType
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
        if (type == HealthDataType.MOVE_MINUTES) {
            val request = ReadRecordsRequest(
                ExerciseSessionRecord::class,
                TimeRangeFilter.between(startTime.toJavaInstant(), endTime.toJavaInstant())
            )
            return try {
                val response = healthConnectClient.readRecords(request)
                val totalMinutes = response.records.sumOf {
                    java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                }
                if (totalMinutes > 0) listOf(
                    HealthData.MoveMinutes(
                        totalMinutes,
                        startTime,
                        endTime
                    )
                ) else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        val recordClass = type.toRecordClass() ?: return emptyList()
        val request = ReadRecordsRequest(
            recordClass,
            TimeRangeFilter.between(startTime.toJavaInstant(), endTime.toJavaInstant())
        )
        return try {
            val response = healthConnectClient.readRecords(request)
            response.records.mapNotNull { it.toSharedModel() }
        } catch (e: Exception) {
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
            HealthDataType.EXERCISE -> ExerciseSessionRecord::class
            HealthDataType.WEIGHT -> WeightRecord::class
            HealthDataType.HEIGHT -> HeightRecord::class
            HealthDataType.BODY_FAT_PERCENTAGE -> BodyFatRecord::class
            HealthDataType.LEAN_BODY_MASS -> LeanBodyMassRecord::class
            HealthDataType.BODY_TEMPERATURE -> BodyTemperatureRecord::class
            HealthDataType.BASAL_ENERGY_BURNED -> BasalMetabolicRateRecord::class
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
            HealthDataType.MOVE_MINUTES -> ExerciseSessionRecord::class
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

            is ExerciseSessionRecord -> {
                val duration = java.time.Duration.between(startTime, endTime).toMinutes()
                val segments = segments.map {
                    HealthData.ExerciseSegment(
                        segmentType = it.segmentType.toString(),
                        repetitions = it.repetitions.toLong(),
                        startTime = it.startTime.toKotlinInstant(),
                        endTime = it.endTime.toKotlinInstant()
                    )
                }
                HealthData.ExerciseSession(
                    exerciseType.toString(),
                    duration,
                    notes,
                    segments,
                    startTime.toKotlinInstant(),
                    endTime.toKotlinInstant()
                )
            }
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

            is BasalMetabolicRateRecord -> HealthData.BasalMetabolicRate(
                basalMetabolicRate.inKilocaloriesPerDay,
                time.toKotlinInstant()
            )

            is HeartRateRecord -> {
                val samples = samples.map {
                    HealthData.HeartRateSample(
                        it.beatsPerMinute,
                        it.time.toKotlinInstant()
                    )
                }
                HealthData.HeartRate(
                    samples,
                    startTime = startTime.toKotlinInstant(),
                    endTime = endTime.toKotlinInstant()
                )
            }

            is HeartRateVariabilityRmssdRecord -> HealthData.HeartRateVariability(
                heartRateVariabilityMillis,
                time.toKotlinInstant()
            )

            is BloodPressureRecord -> HealthData.BloodPressure(
                systolic.inMillimetersOfMercury,
                diastolic.inMillimetersOfMercury,
                bodyPosition.toString(),
                time.toKotlinInstant()
            )

            is BloodGlucoseRecord -> HealthData.BloodGlucose(
                level.inMilligramsPerDeciliter,
                relationToMeal.toString(),
                time.toKotlinInstant()
            )

            is OxygenSaturationRecord -> HealthData.OxygenSaturation(
                percentage.value,
                time.toKotlinInstant()
            )
            is RespiratoryRateRecord -> HealthData.RespiratoryRate(
                rate, // breaths per minute
                time.toKotlinInstant()
            )

            is Vo2MaxRecord -> HealthData.Vo2Max(
                vo2MillilitersPerMinuteKilogram,
                time.toKotlinInstant()
            )

            is SleepSessionRecord -> {
                val duration = java.time.Duration.between(startTime, endTime).toMinutes()
                val stages = stages.map {
                    val stageDuration =
                        java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                    HealthData.SleepStage(
                        it.stage.toSleepStateString(),
                        stageDuration,
                        it.startTime.toKotlinInstant(),
                        it.endTime.toKotlinInstant()
                    )
                }
                HealthData.SleepSession(
                    duration,
                    stages,
                    startTime.toKotlinInstant(),
                    endTime.toKotlinInstant()
                )
            }

            is HydrationRecord -> HealthData.Water(
                volume.inLiters,
                startTime.toKotlinInstant(),
                endTime.toKotlinInstant()
            )
            is NutritionRecord -> HealthData.Nutrition(
                mealType = mealType.toString(),
                calories = energy?.inKilocalories,
                proteinGrams = protein?.inGrams,
                fatGrams = totalFat?.inGrams,
                carbsGrams = totalCarbohydrate?.inGrams,
                startTime = startTime.toKotlinInstant(),
                endTime = endTime.toKotlinInstant()
            )

            is MenstruationFlowRecord -> HealthData.Menstruation(
                flow.toString(),
                time = time.toKotlinInstant(),
            )

            is OvulationTestRecord -> HealthData.OvulationTest(
                result.toString(),
                time.toKotlinInstant()
            )

            is CervicalMucusRecord -> HealthData.CervicalMucus(
                appearance.toString(),
                sensation.toString(),
                time.toKotlinInstant()
            )

            is SexualActivityRecord -> HealthData.SexualActivity(
                protectionUsed.toString(),
                time.toKotlinInstant()
            )
            is IntermenstrualBleedingRecord -> HealthData.IntermenstrualBleeding(time.toKotlinInstant())
            else -> null
        }
    }

    fun Int.toSleepStateString(): String {
        return when (this) {
            STAGE_TYPE_UNKNOWN -> "Unknown"
            STAGE_TYPE_AWAKE -> "Awake"
            STAGE_TYPE_SLEEPING -> "Sleeping"
            STAGE_TYPE_OUT_OF_BED -> "Out of bed"
            STAGE_TYPE_LIGHT -> "Light"
            STAGE_TYPE_DEEP -> "Deep"
            STAGE_TYPE_REM -> "Rem"
            STAGE_TYPE_AWAKE_IN_BED -> "Awake in bed"
            else -> "Unknown"
        }
    }

    private fun Instant.toJavaInstant(): java.time.Instant =
        java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())

    private fun java.time.Instant.toKotlinInstant(): Instant =
        Instant.fromEpochMilliseconds(this.toEpochMilli())
}