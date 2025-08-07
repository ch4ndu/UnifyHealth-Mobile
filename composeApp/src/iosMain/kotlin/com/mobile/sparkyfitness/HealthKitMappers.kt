package com.mobile.sparkyfitness

import com.mobile.sparkyfitness.model.HealthData
import com.mobile.sparkyfitness.model.HealthDataType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Instant
import platform.Foundation.timeIntervalSince1970
import platform.HealthKit.HKCategorySample
import platform.HealthKit.HKCategoryTypeIdentifier
import platform.HealthKit.HKCategoryTypeIdentifierCervicalMucusQuality
import platform.HealthKit.HKCategoryTypeIdentifierIntermenstrualBleeding
import platform.HealthKit.HKCategoryTypeIdentifierMenstrualFlow
import platform.HealthKit.HKCategoryTypeIdentifierOvulationTestResult
import platform.HealthKit.HKCategoryTypeIdentifierSexualActivity
import platform.HealthKit.HKCategoryTypeIdentifierSleepAnalysis
import platform.HealthKit.HKCategoryValueCervicalMucusQualityCreamy
import platform.HealthKit.HKCategoryValueCervicalMucusQualityDry
import platform.HealthKit.HKCategoryValueCervicalMucusQualityEggWhite
import platform.HealthKit.HKCategoryValueCervicalMucusQualitySticky
import platform.HealthKit.HKCategoryValueCervicalMucusQualityWatery
import platform.HealthKit.HKCategoryValueOvulationTestResultIndeterminate
import platform.HealthKit.HKCategoryValueOvulationTestResultNegative
import platform.HealthKit.HKCategoryValueOvulationTestResultPositive
import platform.HealthKit.HKCorrelation
import platform.HealthKit.HKCorrelationTypeIdentifier
import platform.HealthKit.HKCorrelationTypeIdentifierBloodPressure
import platform.HealthKit.HKMetricPrefixDeci
import platform.HealthKit.HKMetricPrefixKilo
import platform.HealthKit.HKMetricPrefixMilli
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantitySample
import platform.HealthKit.HKQuantityTypeIdentifier
import platform.HealthKit.HKQuantityTypeIdentifierActiveEnergyBurned
import platform.HealthKit.HKQuantityTypeIdentifierAppleMoveTime
import platform.HealthKit.HKQuantityTypeIdentifierBasalEnergyBurned
import platform.HealthKit.HKQuantityTypeIdentifierBloodGlucose
import platform.HealthKit.HKQuantityTypeIdentifierBloodPressureDiastolic
import platform.HealthKit.HKQuantityTypeIdentifierBloodPressureSystolic
import platform.HealthKit.HKQuantityTypeIdentifierBodyFatPercentage
import platform.HealthKit.HKQuantityTypeIdentifierBodyMass
import platform.HealthKit.HKQuantityTypeIdentifierBodyMassIndex
import platform.HealthKit.HKQuantityTypeIdentifierBodyTemperature
import platform.HealthKit.HKQuantityTypeIdentifierDietaryWater
import platform.HealthKit.HKQuantityTypeIdentifierDistanceWalkingRunning
import platform.HealthKit.HKQuantityTypeIdentifierFlightsClimbed
import platform.HealthKit.HKQuantityTypeIdentifierHeartRate
import platform.HealthKit.HKQuantityTypeIdentifierHeartRateVariabilitySDNN
import platform.HealthKit.HKQuantityTypeIdentifierHeight
import platform.HealthKit.HKQuantityTypeIdentifierLeanBodyMass
import platform.HealthKit.HKQuantityTypeIdentifierOxygenSaturation
import platform.HealthKit.HKQuantityTypeIdentifierRespiratoryRate
import platform.HealthKit.HKQuantityTypeIdentifierStepCount
import platform.HealthKit.HKQuantityTypeIdentifierVO2Max
import platform.HealthKit.HKUnit
import platform.HealthKit.countUnit
import platform.HealthKit.degreeCelsiusUnit
import platform.HealthKit.gramUnitWithMetricPrefix
import platform.HealthKit.kilocalorieUnit
import platform.HealthKit.literUnit
import platform.HealthKit.literUnitWithMetricPrefix
import platform.HealthKit.meterUnit
import platform.HealthKit.millimeterOfMercuryUnit
import platform.HealthKit.minuteUnit
import platform.HealthKit.percentUnit
import platform.HealthKit.secondUnitWithMetricPrefix
import platform.HealthKit.unitDividedByUnit

@OptIn(ExperimentalForeignApi::class)
internal fun HKQuantitySample.toHealthData(type: HealthDataType): HealthData? {
    val startDate = Instant.fromEpochSeconds(this.startDate.timeIntervalSince1970.toLong())
    val endDate = Instant.fromEpochSeconds(this.endDate.timeIntervalSince1970.toLong())

    return when (type) {
        HealthDataType.STEPS -> HealthData.Steps(
            this.quantity.doubleValueForUnit(HKUnit.countUnit()).toLong(), startDate, endDate
        )

        HealthDataType.DISTANCE -> HealthData.Distance(
            this.quantity.doubleValueForUnit(HKUnit.meterUnit()),
            startDate,
            endDate
        )

        HealthDataType.FLOORS_CLIMBED -> HealthData.FloorsClimbed(
            this.quantity.doubleValueForUnit(
                HKUnit.countUnit()
            ), startDate, endDate
        )

        HealthDataType.ACTIVE_ENERGY_BURNED -> HealthData.ActiveEnergyBurned(
            this.quantity.doubleValueForUnit(
                HKUnit.kilocalorieUnit()
            ), startDate, endDate
        )

        HealthDataType.MOVE_MINUTES -> HealthData.MoveMinutes(
            this.quantity.doubleValueForUnit(
                HKUnit.minuteUnit()
            ).toInt(), startDate, endDate
        )

        HealthDataType.WEIGHT -> HealthData.Weight(
            this.quantity.doubleValueForUnit(
                HKUnit.gramUnitWithMetricPrefix(
                    HKMetricPrefixKilo
                )
            ), endDate
        )

        HealthDataType.HEIGHT -> HealthData.Height(
            this.quantity.doubleValueForUnit(HKUnit.meterUnit()),
            endDate
        )

        HealthDataType.BODY_FAT_PERCENTAGE -> HealthData.BodyFatPercentage(
            this.quantity.doubleValueForUnit(
                HKUnit.percentUnit()
            ), endDate
        )

        HealthDataType.LEAN_BODY_MASS -> HealthData.LeanBodyMass(
            this.quantity.doubleValueForUnit(
                HKUnit.gramUnitWithMetricPrefix(HKMetricPrefixKilo)
            ), endDate
        )

        HealthDataType.BODY_MASS_INDEX -> HealthData.BodyMassIndex(
            this.quantity.doubleValueForUnit(
                HKUnit.countUnit()
            ), endDate
        )

        HealthDataType.BODY_TEMPERATURE -> HealthData.BodyTemperature(
            this.quantity.doubleValueForUnit(
                HKUnit.degreeCelsiusUnit()
            ), endDate
        )

        HealthDataType.BASAL_ENERGY_BURNED -> HealthData.BasalMetabolicRate(
            this.quantity.doubleValueForUnit(
                HKUnit.kilocalorieUnit()
            ), endDate
        )

        HealthDataType.HEART_RATE -> HealthData.HeartRate(
            this.quantity.doubleValueForUnit(
                HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit())
            ).toLong(),
            startDate,
            endDate
        )

        HealthDataType.HEART_RATE_VARIABILITY -> HealthData.HeartRateVariability(
            this.quantity.doubleValueForUnit(
                HKUnit.secondUnitWithMetricPrefix(HKMetricPrefixMilli)
            ), endDate
        )

        HealthDataType.BLOOD_GLUCOSE -> HealthData.BloodGlucose(
            this.quantity.doubleValueForUnit(
                HKUnit.gramUnitWithMetricPrefix(HKMetricPrefixMilli)
                    .unitDividedByUnit(HKUnit.literUnitWithMetricPrefix(HKMetricPrefixDeci))
            ), endDate
        )

        HealthDataType.OXYGEN_SATURATION -> HealthData.OxygenSaturation(
            this.quantity.doubleValueForUnit(
                HKUnit.percentUnit()
            ), endDate
        )

        HealthDataType.RESPIRATORY_RATE -> HealthData.RespiratoryRate(
            this.quantity.doubleValueForUnit(
                HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit())
            ), endDate
        )

        HealthDataType.VO2_MAX -> HealthData.Vo2Max(
            this.quantity.doubleValueForUnit(
                HKUnit.literUnitWithMetricPrefix(
                    HKMetricPrefixMilli
                ).unitDividedByUnit(HKUnit.gramUnitWithMetricPrefix(HKMetricPrefixKilo))
                    .unitDividedByUnit(HKUnit.minuteUnit())
            ), endDate
        )

        HealthDataType.WATER -> HealthData.Water(
            this.quantity.doubleValueForUnit(HKUnit.literUnit()),
            startDate,
            endDate
        )

        else -> null
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun HKCategorySample.toHealthData(type: HealthDataType): HealthData? {
    val startDate = Instant.fromEpochSeconds(this.startDate.timeIntervalSince1970.toLong())
    val endDate = Instant.fromEpochSeconds(this.endDate.timeIntervalSince1970.toLong())

    return when (type) {
        HealthDataType.SLEEP -> {
            val duration = endDate.epochSeconds - startDate.epochSeconds
            HealthData.SleepSession(duration / 60, startDate, endDate)
        }

        HealthDataType.MENSTRUATION -> HealthData.Menstruation(startDate, 0)
        HealthDataType.OVULATION_TEST -> {
            val result = when (this.value) {
                HKCategoryValueOvulationTestResultPositive -> "Positive"
                HKCategoryValueOvulationTestResultNegative -> "Negative"
                HKCategoryValueOvulationTestResultIndeterminate -> "Indeterminate"
                else -> "Unknown"
            }
            HealthData.OvulationTest(result, endDate)
        }

        HealthDataType.CERVICAL_MUCUS -> {
            val quality = when (this.value) {
                HKCategoryValueCervicalMucusQualityDry -> "Dry"
                HKCategoryValueCervicalMucusQualitySticky -> "Sticky"
                HKCategoryValueCervicalMucusQualityCreamy -> "Creamy"
                HKCategoryValueCervicalMucusQualityWatery -> "Watery"
                HKCategoryValueCervicalMucusQualityEggWhite -> "EggWhite"
                else -> "Unknown"
            }
            HealthData.CervicalMucus(quality, endDate)
        }

        HealthDataType.SEXUAL_ACTIVITY -> HealthData.SexualActivity(endDate)
        HealthDataType.INTERMENSTRUAL_BLEEDING -> HealthData.IntermenstrualBleeding(endDate)
        else -> null
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun HKCorrelation.toHealthData(type: HealthDataType): HealthData? {
    val endDate = Instant.fromEpochSeconds(this.endDate.timeIntervalSince1970.toLong())

    return when (type) {
        HealthDataType.BLOOD_PRESSURE -> {
            val systolicType =
                HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBloodPressureSystolic)!!
            val diastolicType = HKObjectType.quantityTypeForIdentifier(
                HKQuantityTypeIdentifierBloodPressureDiastolic
            )!!

            val systolicSample =
                this.objectsForType(systolicType).firstOrNull() as? HKQuantitySample
            val diastolicSample =
                this.objectsForType(diastolicType).firstOrNull() as? HKQuantitySample

            val systolicValue =
                systolicSample?.quantity?.doubleValueForUnit(HKUnit.millimeterOfMercuryUnit())
            val diastolicValue =
                diastolicSample?.quantity?.doubleValueForUnit(HKUnit.millimeterOfMercuryUnit())

            if (systolicValue != null && diastolicValue != null) {
                HealthData.BloodPressure(systolicValue, diastolicValue, endDate)
            } else {
                null
            }
        }

        else -> null
    }
}

internal enum class HealthDataTypeCategory { QUANTITY, CATEGORY, CORRELATION }

internal fun HealthDataType.getCategory(): HealthDataTypeCategory {
    return when (this) {
        HealthDataType.STEPS, HealthDataType.DISTANCE, HealthDataType.FLOORS_CLIMBED,
        HealthDataType.ACTIVE_ENERGY_BURNED, HealthDataType.MOVE_MINUTES, HealthDataType.WEIGHT,
        HealthDataType.HEIGHT, HealthDataType.BODY_FAT_PERCENTAGE, HealthDataType.LEAN_BODY_MASS,
        HealthDataType.BODY_MASS_INDEX, HealthDataType.BODY_TEMPERATURE, HealthDataType.BASAL_ENERGY_BURNED,
        HealthDataType.HEART_RATE, HealthDataType.HEART_RATE_VARIABILITY, HealthDataType.BLOOD_GLUCOSE,
        HealthDataType.OXYGEN_SATURATION, HealthDataType.RESPIRATORY_RATE, HealthDataType.VO2_MAX,
        HealthDataType.WATER -> HealthDataTypeCategory.QUANTITY

        HealthDataType.SLEEP, HealthDataType.MENSTRUATION, HealthDataType.OVULATION_TEST,
        HealthDataType.CERVICAL_MUCUS, HealthDataType.SEXUAL_ACTIVITY,
        HealthDataType.INTERMENSTRUAL_BLEEDING -> HealthDataTypeCategory.CATEGORY

        HealthDataType.BLOOD_PRESSURE -> HealthDataTypeCategory.CORRELATION

        HealthDataType.EXERCISE, HealthDataType.NUTRITION -> HealthDataTypeCategory.CATEGORY // Placeholder
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun HealthDataType.toHkObjectType(): HKObjectType? {
    return when (this.getCategory()) {
        HealthDataTypeCategory.QUANTITY -> this.toHkQuantityTypeIdentifier()
            ?.let { HKObjectType.quantityTypeForIdentifier(it) }

        HealthDataTypeCategory.CATEGORY -> this.toHkCategoryTypeIdentifier()
            ?.let { HKObjectType.categoryTypeForIdentifier(it) }

        HealthDataTypeCategory.CORRELATION -> this.toHkCorrelationTypeIdentifier()
            ?.let { HKObjectType.correlationTypeForIdentifier(it) }
    }
}

internal fun HealthDataType.toHkQuantityTypeIdentifier(): HKQuantityTypeIdentifier? {
    return when (this) {
        HealthDataType.STEPS -> HKQuantityTypeIdentifierStepCount
        HealthDataType.DISTANCE -> HKQuantityTypeIdentifierDistanceWalkingRunning
        HealthDataType.FLOORS_CLIMBED -> HKQuantityTypeIdentifierFlightsClimbed
        HealthDataType.ACTIVE_ENERGY_BURNED -> HKQuantityTypeIdentifierActiveEnergyBurned
        HealthDataType.MOVE_MINUTES -> HKQuantityTypeIdentifierAppleMoveTime
        HealthDataType.WEIGHT -> HKQuantityTypeIdentifierBodyMass
        HealthDataType.HEIGHT -> HKQuantityTypeIdentifierHeight
        HealthDataType.BODY_FAT_PERCENTAGE -> HKQuantityTypeIdentifierBodyFatPercentage
        HealthDataType.LEAN_BODY_MASS -> HKQuantityTypeIdentifierLeanBodyMass
        HealthDataType.BODY_MASS_INDEX -> HKQuantityTypeIdentifierBodyMassIndex
        HealthDataType.BODY_TEMPERATURE -> HKQuantityTypeIdentifierBodyTemperature
        HealthDataType.BASAL_ENERGY_BURNED -> HKQuantityTypeIdentifierBasalEnergyBurned
        HealthDataType.HEART_RATE -> HKQuantityTypeIdentifierHeartRate
        HealthDataType.HEART_RATE_VARIABILITY -> HKQuantityTypeIdentifierHeartRateVariabilitySDNN
        HealthDataType.BLOOD_GLUCOSE -> HKQuantityTypeIdentifierBloodGlucose
        HealthDataType.OXYGEN_SATURATION -> HKQuantityTypeIdentifierOxygenSaturation
        HealthDataType.RESPIRATORY_RATE -> HKQuantityTypeIdentifierRespiratoryRate
        HealthDataType.VO2_MAX -> HKQuantityTypeIdentifierVO2Max
        HealthDataType.WATER -> HKQuantityTypeIdentifierDietaryWater
        else -> null
    }
}

internal fun HealthDataType.toHkCategoryTypeIdentifier(): HKCategoryTypeIdentifier? {
    return when (this) {
        HealthDataType.SLEEP -> HKCategoryTypeIdentifierSleepAnalysis
        HealthDataType.MENSTRUATION -> HKCategoryTypeIdentifierMenstrualFlow
        HealthDataType.OVULATION_TEST -> HKCategoryTypeIdentifierOvulationTestResult
        HealthDataType.CERVICAL_MUCUS -> HKCategoryTypeIdentifierCervicalMucusQuality
        HealthDataType.SEXUAL_ACTIVITY -> HKCategoryTypeIdentifierSexualActivity
        HealthDataType.INTERMENSTRUAL_BLEEDING -> HKCategoryTypeIdentifierIntermenstrualBleeding
        else -> null
    }
}

internal fun HealthDataType.toHkCorrelationTypeIdentifier(): HKCorrelationTypeIdentifier? {
    return when (this) {
        HealthDataType.BLOOD_PRESSURE -> HKCorrelationTypeIdentifierBloodPressure
        else -> null
    }
}