package cachet.plugins.health

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord

object HCType {
    const val BODY_FAT_PERCENTAGE = "BODY_FAT_PERCENTAGE"
    const val HEIGHT = "HEIGHT"
    const val WEIGHT = "WEIGHT"
    const val STEPS = "STEPS"
    const val AGGREGATE_STEP_COUNT = "AGGREGATE_STEP_COUNT"
    const val ACTIVE_ENERGY_BURNED = "ACTIVE_ENERGY_BURNED"
    const val HEART_RATE = "HEART_RATE"
    const val BODY_TEMPERATURE = "BODY_TEMPERATURE"
    const val BLOOD_PRESSURE_SYSTOLIC = "BLOOD_PRESSURE_SYSTOLIC"
    const val BLOOD_PRESSURE_DIASTOLIC = "BLOOD_PRESSURE_DIASTOLIC"
    const val BLOOD_OXYGEN = "BLOOD_OXYGEN"
    const val BLOOD_GLUCOSE = "BLOOD_GLUCOSE"
    const val MOVE_MINUTES = "MOVE_MINUTES"
    const val DISTANCE_DELTA = "DISTANCE_DELTA"
    const val WATER = "WATER"
    const val RESTING_HEART_RATE = "RESTING_HEART_RATE"
    const val BASAL_ENERGY_BURNED = "BASAL_ENERGY_BURNED"
    const val FLIGHTS_CLIMBED = "FLIGHTS_CLIMBED"
    const val RESPIRATORY_RATE = "RESPIRATORY_RATE"

    // TODO support unknown?
    const val SLEEP_ASLEEP = "SLEEP_ASLEEP"
    const val SLEEP_AWAKE = "SLEEP_AWAKE"
    const val SLEEP_IN_BED = "SLEEP_IN_BED"
    const val SLEEP_SESSION = "SLEEP_SESSION"
    const val SLEEP_LIGHT = "SLEEP_LIGHT"
    const val SLEEP_DEEP = "SLEEP_DEEP"
    const val SLEEP_REM = "SLEEP_REM"
    const val SLEEP_OUT_OF_BED = "SLEEP_OUT_OF_BED"
    const val WORKOUT = "WORKOUT"
    const val NUTRITION = "NUTRITION"
    const val BREAKFAST = "BREAKFAST"
    const val LUNCH = "LUNCH"
    const val DINNER = "DINNER"
    const val SNACK = "SNACK"
    const val MEAL_UNKNOWN = "UNKNOWN"

    val MapSleepStageToType = hashMapOf<Int, String>(
        1 to SLEEP_AWAKE,
        2 to SLEEP_ASLEEP,
        3 to SLEEP_OUT_OF_BED,
        4 to SLEEP_LIGHT,
        5 to SLEEP_DEEP,
        6 to SLEEP_REM,
    )

    val MapMealTypeToTypeHC = hashMapOf<String, Int>(
        BREAKFAST to MealType.MEAL_TYPE_BREAKFAST,
        LUNCH to MealType.MEAL_TYPE_LUNCH,
        DINNER to MealType.MEAL_TYPE_DINNER,
        SNACK to MealType.MEAL_TYPE_SNACK,
        MEAL_UNKNOWN to MealType.MEAL_TYPE_UNKNOWN,
    )

    val MapTypeToMealTypeHC = hashMapOf<Int, String>(
        MealType.MEAL_TYPE_BREAKFAST to BREAKFAST,
        MealType.MEAL_TYPE_LUNCH to LUNCH,
        MealType.MEAL_TYPE_DINNER to DINNER,
        MealType.MEAL_TYPE_SNACK to SNACK,
        MealType.MEAL_TYPE_UNKNOWN to MEAL_UNKNOWN,
    )

    val MapToHCType = hashMapOf(
        BODY_FAT_PERCENTAGE to BodyFatRecord::class,
        HEIGHT to HeightRecord::class,
        WEIGHT to WeightRecord::class,
        STEPS to StepsRecord::class,
        AGGREGATE_STEP_COUNT to StepsRecord::class,
        ACTIVE_ENERGY_BURNED to ActiveCaloriesBurnedRecord::class,
        HEART_RATE to HeartRateRecord::class,
        BODY_TEMPERATURE to BodyTemperatureRecord::class,
        BLOOD_PRESSURE_SYSTOLIC to BloodPressureRecord::class,
        BLOOD_PRESSURE_DIASTOLIC to BloodPressureRecord::class,
        BLOOD_OXYGEN to OxygenSaturationRecord::class,
        BLOOD_GLUCOSE to BloodGlucoseRecord::class,
        DISTANCE_DELTA to DistanceRecord::class,
        WATER to HydrationRecord::class,
        SLEEP_ASLEEP to SleepSessionRecord::class,
        SLEEP_AWAKE to SleepSessionRecord::class,
        SLEEP_LIGHT to SleepSessionRecord::class,
        SLEEP_DEEP to SleepSessionRecord::class,
        SLEEP_REM to SleepSessionRecord::class,
        SLEEP_OUT_OF_BED to SleepSessionRecord::class,
        SLEEP_SESSION to SleepSessionRecord::class,
        WORKOUT to ExerciseSessionRecord::class,
        NUTRITION to NutritionRecord::class,
        RESTING_HEART_RATE to RestingHeartRateRecord::class,
        BASAL_ENERGY_BURNED to BasalMetabolicRateRecord::class,
        FLIGHTS_CLIMBED to FloorsClimbedRecord::class,
        RESPIRATORY_RATE to RespiratoryRateRecord::class,
        // MOVE_MINUTES to TODO: Find alternative?
        // TODO: Implement remaining types
        // "ActiveCaloriesBurned" to ActiveCaloriesBurnedRecord::class,
        // "BasalBodyTemperature" to BasalBodyTemperatureRecord::class,
        // "BasalMetabolicRate" to BasalMetabolicRateRecord::class,
        // "BloodGlucose" to BloodGlucoseRecord::class,
        // "BloodPressure" to BloodPressureRecord::class,
        // "BodyFat" to BodyFatRecord::class,
        // "BodyTemperature" to BodyTemperatureRecord::class,
        // "BoneMass" to BoneMassRecord::class,
        // "CervicalMucus" to CervicalMucusRecord::class,
        // "CyclingPedalingCadence" to CyclingPedalingCadenceRecord::class,
        // "Distance" to DistanceRecord::class,
        // "ElevationGained" to ElevationGainedRecord::class,
        // "ExerciseSession" to ExerciseSessionRecord::class,
        // "FloorsClimbed" to FloorsClimbedRecord::class,
        // "HeartRate" to HeartRateRecord::class,
        // "Height" to HeightRecord::class,
        // "Hydration" to HydrationRecord::class,
        // "LeanBodyMass" to LeanBodyMassRecord::class,
        // "MenstruationFlow" to MenstruationFlowRecord::class,
        // "MenstruationPeriod" to MenstruationPeriodRecord::class,
        // "Nutrition" to NutritionRecord::class,
        // "OvulationTest" to OvulationTestRecord::class,
        // "OxygenSaturation" to OxygenSaturationRecord::class,
        // "Power" to PowerRecord::class,
        // "RespiratoryRate" to RespiratoryRateRecord::class,
        // "RestingHeartRate" to RestingHeartRateRecord::class,
        // "SexualActivity" to SexualActivityRecord::class,
        // "SleepSession" to SleepSessionRecord::class,
        // "SleepStage" to SleepStageRecord::class,
        // "Speed" to SpeedRecord::class,
        // "StepsCadence" to StepsCadenceRecord::class,
        // "Steps" to StepsRecord::class,
        // "TotalCaloriesBurned" to TotalCaloriesBurnedRecord::class,
        // "Vo2Max" to Vo2MaxRecord::class,
        // "Weight" to WeightRecord::class,
        // "WheelchairPushes" to WheelchairPushesRecord::class,
    )
}