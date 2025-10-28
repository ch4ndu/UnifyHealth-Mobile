@file:OptIn(ExperimentalTime::class)

package com.mobile.unifyhealth.model

import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime


sealed class HealthData(
    open val time: Instant?,
    open val startTime: Instant?,
    open val endTime: Instant?
) {
    // Activity
    data class Steps(
        val count: Long,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)

    data class Distance(
        val meters: Double,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)

    data class FloorsClimbed(
        val floors: Double,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)

    data class ActiveEnergyBurned(
        val calories: Double,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)

    data class MoveMinutes(
        val minutes: Long,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)

    data class ExerciseSession(
        val name: String,
        val durationMinutes: Long,
        val notes: String?,
        val segments: List<ExerciseSegment>,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)

    // Body Measurements
    data class Weight(
        val kilograms: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class Height(
        val meters: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class BodyFatPercentage(
        val percentage: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class LeanBodyMass(
        val kilograms: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class BodyMassIndex(
        val value: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class BodyTemperature(
        val celsius: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class BasalMetabolicRate(
        val kcalPerDay: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    // Vitals
    data class HeartRate(
        val samples: List<HeartRateSample>,
        override val time: Instant? = null,
        override val endTime: Instant? = null,
        override val startTime: Instant? = null
    ) : HealthData(time, startTime, endTime)

    data class HeartRateVariability(
        val ms: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class BloodPressure(
        val systolicMmhg: Double,
        val diastolicMmhg: Double,
        val bodyPosition: String,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class BloodGlucose(
        val mgdl: Double,
        val relationToMeal: String,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class OxygenSaturation(
        val percentage: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class RespiratoryRate(
        val rpm: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class Vo2Max(
        val mlPerKgPerMin: Double,
        override val time: Instant
    ) : HealthData(time, null, null)

    // Sleep & Nutrition
    data class SleepSession(
        val durationMinutes: Long,
        val stages: List<SleepStage>,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)

    data class Water(
        val liters: Double,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)
    data class Nutrition(
        val mealType: String,
        val calories: Double?,
        val proteinGrams: Double?,
        val fatGrams: Double?,
        val carbsGrams: Double?,
        override val startTime: Instant,
        override val endTime: Instant
    ) : HealthData(null, startTime, endTime)

    // Cycle Tracking
    data class Menstruation(
        val flow: String,
        override val startTime: Instant? = null,
        override val endTime: Instant? = null,
        override val time: Instant? = null
    ) : HealthData(time, startTime, endTime)

    data class OvulationTest(
        val result: String,
        override val time: Instant
    ) : HealthData(time, null, null) // Positive or Negative

    data class CervicalMucus(
        val quality: String,
        val sensation: String,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class SexualActivity(
        val protectionUsed: String,
        override val time: Instant
    ) : HealthData(time, null, null)

    data class IntermenstrualBleeding(override val time: Instant) : HealthData(time, null, null)

    data class SleepStage(
        val stage: String,
        val durationMinutes: Long,
        val startTime: Instant,
        val endTime: Instant
    )

    data class HeartRateSample(
        val bpm: Long,
        val time: Instant
    )

    data class ExerciseSegment(
        val segmentType: String,
        val repetitions: Long,
        val startTime: Instant,
        val endTime: Instant
    )
}
