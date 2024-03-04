package cachet.plugins.health

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.NonNull
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.MealType.MEAL_TYPE_BREAKFAST
import androidx.health.connect.client.records.MealType.MEAL_TYPE_DINNER
import androidx.health.connect.client.records.MealType.MEAL_TYPE_LUNCH
import androidx.health.connect.client.records.MealType.MEAL_TYPE_SNACK
import androidx.health.connect.client.records.MealType.MEAL_TYPE_UNKNOWN
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlinx.coroutines.*
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.*


const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1111
const val HEALTH_CONNECT_RESULT_CODE = 16969
const val CHANNEL_NAME = "flutter_health"
const val MMOLL_2_MGDL = 18.0 // 1 mmoll= 18 mgdl

// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

class HealthPlugin(private var channel: MethodChannel? = null) :
    MethodCallHandler,
    ActivityResultListener,
    Result,
    ActivityAware,
    FlutterPlugin {
    private var mResult: Result? = null
    private var handler: Handler? = null
    private var activity: Activity? = null
    private var context: Context? = null
    private var threadPoolExecutor: ExecutorService? = null
    private var useHealthConnectIfAvailable: Boolean = false
    private var healthConnectRequestPermissionsLauncher:  ActivityResultLauncher<Set<String>>? = null
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var scope: CoroutineScope

    private var BODY_FAT_PERCENTAGE = "BODY_FAT_PERCENTAGE"
    private var HEIGHT = "HEIGHT"
    private var WEIGHT = "WEIGHT"
    private var STEPS = "STEPS"
    private var AGGREGATE_STEP_COUNT = "AGGREGATE_STEP_COUNT"
    private var ACTIVE_ENERGY_BURNED = "ACTIVE_ENERGY_BURNED"
    private var HEART_RATE = "HEART_RATE"
    private var BODY_TEMPERATURE = "BODY_TEMPERATURE"
    private var BLOOD_PRESSURE_SYSTOLIC = "BLOOD_PRESSURE_SYSTOLIC"
    private var BLOOD_PRESSURE_DIASTOLIC = "BLOOD_PRESSURE_DIASTOLIC"
    private var BLOOD_OXYGEN = "BLOOD_OXYGEN"
    private var BLOOD_GLUCOSE = "BLOOD_GLUCOSE"
    private var MOVE_MINUTES = "MOVE_MINUTES"
    private var DISTANCE_DELTA = "DISTANCE_DELTA"
    private var WATER = "WATER"
    private var RESTING_HEART_RATE = "RESTING_HEART_RATE"
    private var BASAL_ENERGY_BURNED = "BASAL_ENERGY_BURNED"
    private var FLIGHTS_CLIMBED = "FLIGHTS_CLIMBED"
    private var RESPIRATORY_RATE = "RESPIRATORY_RATE"

    // TODO support unknown?
    private var SLEEP_ASLEEP = "SLEEP_ASLEEP"
    private var SLEEP_AWAKE = "SLEEP_AWAKE"
    private var SLEEP_IN_BED = "SLEEP_IN_BED"
    private var SLEEP_SESSION = "SLEEP_SESSION"
    private var SLEEP_LIGHT = "SLEEP_LIGHT"
    private var SLEEP_DEEP = "SLEEP_DEEP"
    private var SLEEP_REM = "SLEEP_REM"
    private var SLEEP_OUT_OF_BED = "SLEEP_OUT_OF_BED"
    private var WORKOUT = "WORKOUT"
    private var NUTRITION = "NUTRITION"
    private var BREAKFAST = "BREAKFAST"
    private var LUNCH = "LUNCH"
    private var DINNER = "DINNER"
    private var SNACK = "SNACK"
    private var MEAL_UNKNOWN = "UNKNOWN"


    // TODO: Update with new workout types when Health Connect becomes the standard.
    private val workoutTypeMapHealthConnect = mapOf(
        // "AEROBICS" to ExerciseSessionRecord.EXERCISE_TYPE_AEROBICS,
        "AMERICAN_FOOTBALL" to ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN,
        // "ARCHERY" to ExerciseSessionRecord.EXERCISE_TYPE_ARCHERY,
        "AUSTRALIAN_FOOTBALL" to ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN,
        "BADMINTON" to ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON,
        "BASEBALL" to ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL,
        "BASKETBALL" to ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL,
        // "BIATHLON" to ExerciseSessionRecord.EXERCISE_TYPE_BIATHLON,
        "BIKING" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        // "BIKING_HAND" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING_HAND,
        //"BIKING_MOUNTAIN" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING_MOUNTAIN,
        // "BIKING_ROAD" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING_ROAD,
        // "BIKING_SPINNING" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING_SPINNING,
        // "BIKING_STATIONARY" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
        // "BIKING_UTILITY" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING_UTILITY,
        "BOXING" to ExerciseSessionRecord.EXERCISE_TYPE_BOXING,
        "CALISTHENICS" to ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        // "CIRCUIT_TRAINING" to ExerciseSessionRecord.EXERCISE_TYPE_CIRCUIT_TRAINING,
        "CRICKET" to ExerciseSessionRecord.EXERCISE_TYPE_CRICKET,
        // "CROSS_COUNTRY_SKIING" to ExerciseSessionRecord.EXERCISE_TYPE_SKIING_CROSS_COUNTRY,
        // "CROSS_FIT" to ExerciseSessionRecord.EXERCISE_TYPE_CROSSFIT,
        // "CURLING" to ExerciseSessionRecord.EXERCISE_TYPE_CURLING,
        "DANCING" to ExerciseSessionRecord.EXERCISE_TYPE_DANCING,
        // "DIVING" to ExerciseSessionRecord.EXERCISE_TYPE_DIVING,
        // "DOWNHILL_SKIING" to ExerciseSessionRecord.EXERCISE_TYPE_SKIING_DOWNHILL,
        // "ELEVATOR" to ExerciseSessionRecord.EXERCISE_TYPE_ELEVATOR,
        "ELLIPTICAL" to ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
        // "ERGOMETER" to ExerciseSessionRecord.EXERCISE_TYPE_ERGOMETER,
        // "ESCALATOR" to ExerciseSessionRecord.EXERCISE_TYPE_ESCALATOR,
        "FENCING" to ExerciseSessionRecord.EXERCISE_TYPE_FENCING,
        "FRISBEE_DISC" to ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC,
        // "GARDENING" to ExerciseSessionRecord.EXERCISE_TYPE_GARDENING,
        "GOLF" to ExerciseSessionRecord.EXERCISE_TYPE_GOLF,
        "GUIDED_BREATHING" to ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING,
        "GYMNASTICS" to ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS,
        "HANDBALL" to ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL,
        "HIGH_INTENSITY_INTERVAL_TRAINING" to ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        "HIKING" to ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        // "HOCKEY" to ExerciseSessionRecord.EXERCISE_TYPE_HOCKEY,
        // "HORSEBACK_RIDING" to ExerciseSessionRecord.EXERCISE_TYPE_HORSEBACK_RIDING,
        // "HOUSEWORK" to ExerciseSessionRecord.EXERCISE_TYPE_HOUSEWORK,
        // "IN_VEHICLE" to ExerciseSessionRecord.EXERCISE_TYPE_IN_VEHICLE,
        "ICE_SKATING" to ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING,
        // "INTERVAL_TRAINING" to ExerciseSessionRecord.EXERCISE_TYPE_INTERVAL_TRAINING,
        // "JUMP_ROPE" to ExerciseSessionRecord.EXERCISE_TYPE_JUMP_ROPE,
        // "KAYAKING" to ExerciseSessionRecord.EXERCISE_TYPE_KAYAKING,
        // "KETTLEBELL_TRAINING" to ExerciseSessionRecord.EXERCISE_TYPE_KETTLEBELL_TRAINING,
        // "KICK_SCOOTER" to ExerciseSessionRecord.EXERCISE_TYPE_KICK_SCOOTER,
        // "KICKBOXING" to ExerciseSessionRecord.EXERCISE_TYPE_KICKBOXING,
        // "KITE_SURFING" to ExerciseSessionRecord.EXERCISE_TYPE_KITESURFING,
        "MARTIAL_ARTS" to ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS,
        // "MEDITATION" to ExerciseSessionRecord.EXERCISE_TYPE_MEDITATION,
        // "MIXED_MARTIAL_ARTS" to ExerciseSessionRecord.EXERCISE_TYPE_MIXED_MARTIAL_ARTS,
        // "P90X" to ExerciseSessionRecord.EXERCISE_TYPE_P90X,
        "PARAGLIDING" to ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING,
        "PILATES" to ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
        // "POLO" to ExerciseSessionRecord.EXERCISE_TYPE_POLO,
        "RACQUETBALL" to ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL,
        "ROCK_CLIMBING" to ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING,
        "ROWING" to ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        "ROWING_MACHINE" to ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
        "RUGBY" to ExerciseSessionRecord.EXERCISE_TYPE_RUGBY,
        // "RUNNING_JOGGING" to ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_JOGGING,
        // "RUNNING_SAND" to ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_SAND,
        "RUNNING_TREADMILL" to ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        "RUNNING" to ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        "SAILING" to ExerciseSessionRecord.EXERCISE_TYPE_SAILING,
        "SCUBA_DIVING" to ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING,
        // "SKATING_CROSS" to ExerciseSessionRecord.EXERCISE_TYPE_SKATING_CROSS,
        // "SKATING_INDOOR" to ExerciseSessionRecord.EXERCISE_TYPE_SKATING_INDOOR,
        // "SKATING_INLINE" to ExerciseSessionRecord.EXERCISE_TYPE_SKATING_INLINE,
        "SKATING" to ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
        "SKIING" to ExerciseSessionRecord.EXERCISE_TYPE_SKIING,
        // "SKIING_BACK_COUNTRY" to ExerciseSessionRecord.EXERCISE_TYPE_SKIING_BACK_COUNTRY,
        // "SKIING_KITE" to ExerciseSessionRecord.EXERCISE_TYPE_SKIING_KITE,
        // "SKIING_ROLLER" to ExerciseSessionRecord.EXERCISE_TYPE_SKIING_ROLLER,
        // "SLEDDING" to ExerciseSessionRecord.EXERCISE_TYPE_SLEDDING,
        "SNOWBOARDING" to ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING,
        // "SNOWMOBILE" to ExerciseSessionRecord.EXERCISE_TYPE_SNOWMOBILE,
        "SNOWSHOEING" to ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING,
        // "SOCCER" to ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_SOCCER,
        "SOFTBALL" to ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL,
        "SQUASH" to ExerciseSessionRecord.EXERCISE_TYPE_SQUASH,
        "STAIR_CLIMBING_MACHINE" to ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
        "STAIR_CLIMBING" to ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
        // "STANDUP_PADDLEBOARDING" to ExerciseSessionRecord.EXERCISE_TYPE_STANDUP_PADDLEBOARDING,
        // "STILL" to ExerciseSessionRecord.EXERCISE_TYPE_STILL,
        "STRENGTH_TRAINING" to ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        "SURFING" to ExerciseSessionRecord.EXERCISE_TYPE_SURFING,
        "SWIMMING_OPEN_WATER" to ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
        "SWIMMING_POOL" to ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        // "SWIMMING" to ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING,
        "TABLE_TENNIS" to ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS,
        // "TEAM_SPORTS" to ExerciseSessionRecord.EXERCISE_TYPE_TEAM_SPORTS,
        "TENNIS" to ExerciseSessionRecord.EXERCISE_TYPE_TENNIS,
        // "TILTING" to ExerciseSessionRecord.EXERCISE_TYPE_TILTING,
        // "VOLLEYBALL_BEACH" to ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL_BEACH,
        // "VOLLEYBALL_INDOOR" to ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL_INDOOR,
        "VOLLEYBALL" to ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL,
        // "WAKEBOARDING" to ExerciseSessionRecord.EXERCISE_TYPE_WAKEBOARDING,
        // "WALKING_FITNESS" to ExerciseSessionRecord.EXERCISE_TYPE_WALKING_FITNESS,
        // "WALKING_PACED" to ExerciseSessionRecord.EXERCISE_TYPE_WALKING_PACED,
        // "WALKING_NORDIC" to ExerciseSessionRecord.EXERCISE_TYPE_WALKING_NORDIC,
        // "WALKING_STROLLER" to ExerciseSessionRecord.EXERCISE_TYPE_WALKING_STROLLER,
        // "WALKING_TREADMILL" to ExerciseSessionRecord.EXERCISE_TYPE_WALKING_TREADMILL,
        "WALKING" to ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        "WATER_POLO" to ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO,
        "WEIGHTLIFTING" to ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        "WHEELCHAIR" to ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR,
        // "WINDSURFING" to ExerciseSessionRecord.EXERCISE_TYPE_WINDSURFING,
        "YOGA" to ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
        // "ZUMBA" to ExerciseSessionRecord.EXERCISE_TYPE_ZUMBA,
        // "OTHER" to ExerciseSessionRecord.EXERCISE_TYPE_OTHER,
    )

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
        channel?.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        threadPoolExecutor = Executors.newFixedThreadPool(4)
        checkAvailability()
        if (healthConnectAvailable) {
            healthConnectClient =
                HealthConnectClient.getOrCreate(flutterPluginBinding.applicationContext)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = null
        activity = null
        threadPoolExecutor!!.shutdown()
        threadPoolExecutor = null
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        @Suppress("unused")
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), CHANNEL_NAME)
            val plugin = HealthPlugin(channel)
            registrar.addActivityResultListener(plugin)
            channel.setMethodCallHandler(plugin)
        }
    }

    override fun success(p0: Any?) {
        handler?.post { mResult?.success(p0) }
    }

    override fun notImplemented() {
        handler?.post { mResult?.notImplemented() }
    }

    override fun error(
        errorCode: String,
        errorMessage: String?,
        errorDetails: Any?,
    ) {
        handler?.post { mResult?.error(errorCode, errorMessage, errorDetails) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i("FLUTTER_HEALTH", "Access Granted!")
                mResult?.success(true)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.i("FLUTTER_HEALTH", "Access Denied!")
                mResult?.success(false)
            }
        }
        return false
    }


    private  fun onHealthConnectPermissionCallback(permissionGranted: Set<String>)
    {
        if(permissionGranted.isEmpty()) {
            mResult?.success(false);
            Log.i("FLUTTER_HEALTH", "Access Denied (to Health Connect)!")

        }else {
            mResult?.success(true);
            Log.i("FLUTTER_HEALTH", "Access Granted (to Health Connect)!")
        }

    }
    
    private fun writeMealHC(call: MethodCall, result: Result) {
        val startTime = Instant.ofEpochMilli(call.argument<Long>("startTime")!!)
        val endTime = Instant.ofEpochMilli(call.argument<Long>("endTime")!!)
        val calories = call.argument<Double>("caloriesConsumed")
        val carbs = call.argument<Double>("carbohydrates") as Double?
        val protein = call.argument<Double>("protein") as Double?
        val fat = call.argument<Double>("fatTotal") as Double?
        val name = call.argument<String>("name")
        val mealType = call.argument<String>("mealType")!!

        scope.launch {
            try {
                val list = mutableListOf<Record>()
                list.add(
                    NutritionRecord(
                        name = name,
                        energy = calories?.kilocalories,
                        totalCarbohydrate = carbs?.grams,
                        protein = protein?.grams,
                        totalFat = fat?.grams,
                        startTime = startTime,
                        startZoneOffset = null,
                        endTime = endTime,
                        endZoneOffset = null,
                        mealType = MapMealTypeToTypeHC[mealType] ?: MEAL_TYPE_UNKNOWN,
                    ),
                )
                healthConnectClient.insertRecords(
                    list,
                )
                result.success(true)
                Log.i("FLUTTER_HEALTH::SUCCESS", "[Health Connect] Meal was successfully added!")

            } catch (e: Exception) {
                Log.w(
                    "FLUTTER_HEALTH::ERROR",
                    "[Health Connect] There was an error adding the meal",
                )
                Log.w("FLUTTER_HEALTH::ERROR", e.message ?: "unknown error")
                Log.w("FLUTTER_HEALTH::ERROR", e.stackTrace.toString())
                result.success(false)
            }

        }
    }

    private fun getStepsHC(call: MethodCall, result: Result) = scope.launch {
        val start = call.argument<Long>("startTime")!!
        val end = call.argument<Long>("endTime")!!

        try {
            val startInstant = Instant.ofEpochMilli(start)
            val endInstant = Instant.ofEpochMilli(end)
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                ),
            )
            // The result may be null if no data is available in the time range.
            val stepsInInterval = response[StepsRecord.COUNT_TOTAL] ?: 0L
            Log.i("FLUTTER_HEALTH::SUCCESS", "returning $stepsInInterval steps")
            result.success(stepsInInterval)
        } catch (e: Exception) {
            Log.i("FLUTTER_HEALTH::ERROR", "unable to return steps")
            result.success(null)
        }
    }

    /**
     *  Handle calls from the MethodChannel
     */
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "useHealthConnectIfAvailable" -> useHealthConnectIfAvailable(call, result)
            "hasPermissions" -> hasPermissionsHC(call, result)
            "requestAuthorization" -> requestAuthorizationHC(call, result)
            // "revokePermissions" -> revokePermissions(call, result)
            "getData" -> getHCData(call, result)
            "writeData" -> writeHCData(call, result)
            "delete" -> deleteHCData(call, result)
            "getTotalStepsInInterval" -> getStepsHC(call, result)
            "writeWorkoutData" -> writeWorkoutHCData(call, result)
            "writeBloodPressure" -> writeBloodPressureHC(call, result)
            // "writeBloodOxygen" -> writeBloodOxygen(call, result)
            "writeMeal" -> writeMealHC(call, result)
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        if (channel == null) {
            return
        }
        binding.addActivityResultListener(this)
        activity = binding.activity


        if ( healthConnectAvailable) {
            val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

            healthConnectRequestPermissionsLauncher =(activity as ComponentActivity).registerForActivityResult(requestPermissionActivityContract) { granted ->
                onHealthConnectPermissionCallback(granted);
            }
        }

    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        if (channel == null) {
            return
        }
        activity = null
        healthConnectRequestPermissionsLauncher = null;
    }

    /**
     * HEALTH CONNECT BELOW
     */
    var healthConnectAvailable = false
    var healthConnectStatus = HealthConnectClient.SDK_UNAVAILABLE

    fun checkAvailability() {
        healthConnectStatus = HealthConnectClient.getSdkStatus(context!!)
        healthConnectAvailable = healthConnectStatus == HealthConnectClient.SDK_AVAILABLE
    }

    fun useHealthConnectIfAvailable(call: MethodCall, result: Result) {
        useHealthConnectIfAvailable = true
        result.success(null)
    }

    private fun hasPermissionsHC(call: MethodCall, result: Result) {
        val args = call.arguments as HashMap<*, *>
        val types = (args["types"] as? ArrayList<*>)?.filterIsInstance<String>()!!
        val permissions = (args["permissions"] as? ArrayList<*>)?.filterIsInstance<Int>()!!

        var permList = mutableListOf<String>()
        for ((i, typeKey) in types.withIndex()) {
            if(!MapToHCType.containsKey(typeKey)) {
                Log.w("FLUTTER_HEALTH::ERROR", "Datatype " + typeKey + " not found in HC")
                result.success(false)
                return
            }
            val access = permissions[i]
            val dataType = MapToHCType[typeKey]!!
            if (access == 0) {
                permList.add(
                    HealthPermission.getReadPermission(dataType),
                )
            } else {
                permList.addAll(
                    listOf(
                        HealthPermission.getReadPermission(dataType),
                        HealthPermission.getWritePermission(dataType),
                    ),
                )
            }
            // Workout also needs distance and total energy burned too
            if (typeKey == WORKOUT) {
                if (access == 0) {
                    permList.addAll(
                        listOf(
                            HealthPermission.getReadPermission(DistanceRecord::class),
                            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                        ),
                    )
                } else {
                    permList.addAll(
                        listOf(
                            HealthPermission.getReadPermission(DistanceRecord::class),
                            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                            HealthPermission.getWritePermission(DistanceRecord::class),
                            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
                        ),
                    )
                }
            }
        }
        scope.launch {
            result.success(
                healthConnectClient.permissionController.getGrantedPermissions()
                    .containsAll(permList),
            )
        }
    }

    private fun requestAuthorizationHC(call: MethodCall, result: Result) {
        val args = call.arguments as HashMap<*, *>
        val types = (args["types"] as? ArrayList<*>)?.filterIsInstance<String>()!!
        val permissions = (args["permissions"] as? ArrayList<*>)?.filterIsInstance<Int>()!!

        var permList = mutableListOf<String>()
        for ((i, typeKey) in types.withIndex()) {
            if(!MapToHCType.containsKey(typeKey)) {
                Log.w("FLUTTER_HEALTH::ERROR", "Datatype " + typeKey + " not found in HC")
                result.success(false)
                return
            }
            val access = permissions[i]!!
            val dataType = MapToHCType[typeKey]!!
            if (access == 0) {
                permList.add(
                    HealthPermission.getReadPermission(dataType),
                )
            } else {
                permList.addAll(
                    listOf(
                        HealthPermission.getReadPermission(dataType),
                        HealthPermission.getWritePermission(dataType),
                    ),
                )
            }
            // Workout also needs distance and total energy burned too
            if (typeKey == WORKOUT) {
                if (access == 0) {
                    permList.addAll(
                        listOf(
                            HealthPermission.getReadPermission(DistanceRecord::class),
                            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                        ),
                    )
                } else {
                    permList.addAll(
                        listOf(
                            HealthPermission.getReadPermission(DistanceRecord::class),
                            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                            HealthPermission.getWritePermission(DistanceRecord::class),
                            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
                        ),
                    )
                }
            }
        }
        
        if(healthConnectRequestPermissionsLauncher == null) {
            result.success(false)
            Log.i("FLUTTER_HEALTH", "Permission launcher not found")
            return;
        }


        healthConnectRequestPermissionsLauncher!!.launch(permList.toSet());
    }

    fun getHCData(call: MethodCall, result: Result) {
        val dataType = call.argument<String>("dataTypeKey")!!
        val startTime = Instant.ofEpochMilli(call.argument<Long>("startTime")!!)
        val endTime = Instant.ofEpochMilli(call.argument<Long>("endTime")!!)
        val healthConnectData = mutableListOf<Map<String, Any?>>()
        scope.launch {
            MapToHCType[dataType]?.let { classType ->
                val records = mutableListOf<Record>()

                // Set up the initial request to read health records with specified parameters
                var request = ReadRecordsRequest(
                    recordType = classType,
                    // Define the maximum amount of data that HealthConnect can return in a single request
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                )
                
                var response = healthConnectClient.readRecords(request)
                var pageToken = response.pageToken
                
                // Add the records from the initial response to the records list
                records.addAll(response.records)

                // Continue making requests and fetching records while there is a page token
                while (!pageToken.isNullOrEmpty()) {
                    request = ReadRecordsRequest(
                        recordType = classType,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                        pageToken = pageToken
                    )
                    response = healthConnectClient.readRecords(request)

                    pageToken = response.pageToken
                    records.addAll(response.records)
                }

                // Workout needs distance and total calories burned too
                if (dataType == WORKOUT) {
                    for (rec in records) {
                        val record = rec as ExerciseSessionRecord
                        val distanceRequest = healthConnectClient.readRecords(
                            ReadRecordsRequest(
                                recordType = DistanceRecord::class,
                                timeRangeFilter = TimeRangeFilter.between(
                                    record.startTime,
                                    record.endTime,
                                ),
                            ),
                        )
                        var totalDistance = 0.0
                        for (distanceRec in distanceRequest.records) {
                            totalDistance += distanceRec.distance.inMeters
                        }

                        val energyBurnedRequest = healthConnectClient.readRecords(
                            ReadRecordsRequest(
                                recordType = TotalCaloriesBurnedRecord::class,
                                timeRangeFilter = TimeRangeFilter.between(
                                    record.startTime,
                                    record.endTime,
                                ),
                            ),
                        )
                        var totalEnergyBurned = 0.0
                        for (energyBurnedRec in energyBurnedRequest.records) {
                            totalEnergyBurned += energyBurnedRec.energy.inKilocalories
                        }

                        // val metadata = (rec as Record).metadata
                        // Add final datapoint
                        healthConnectData.add(
                            // mapOf(
                            mapOf<String, Any?>(
                                "workoutActivityType" to (
                                        workoutTypeMapHealthConnect.filterValues { it == record.exerciseType }.keys.firstOrNull()
                                            ?: "OTHER"
                                        ),
                                "totalDistance" to if (totalDistance == 0.0) null else totalDistance,
                                "totalDistanceUnit" to "METER",
                                "totalEnergyBurned" to if (totalEnergyBurned == 0.0) null else totalEnergyBurned,
                                "totalEnergyBurnedUnit" to "KILOCALORIE",
                                "unit" to "MINUTES",
                                "date_from" to rec.startTime.toEpochMilli(),
                                "date_to" to rec.endTime.toEpochMilli(),
                                "source_id" to "",
                                "source_name" to record.metadata.dataOrigin.packageName,
                            ),
                        )
                    }
                // Filter sleep stages for requested stage
                }
                else if (classType == SleepSessionRecord::class) {
                    for (rec in response.records) {
                        if (rec is SleepSessionRecord) {
                            if (dataType == SLEEP_SESSION) {
                                healthConnectData.addAll(convertRecord(rec, dataType))
                            }
                            else {
                                for (recStage in rec.stages) {
                                    if (dataType == MapSleepStageToType[recStage.stage]) {
                                        healthConnectData.addAll(
                                            convertRecordStage(
                                                recStage, dataType,
                                                rec.metadata.dataOrigin.packageName
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    for (rec in records) {
                        healthConnectData.addAll(convertRecord(rec, dataType))
                    }
                }
            }
            Handler(context!!.mainLooper).run { result.success(healthConnectData) }
        }
    }

    fun convertRecordStage(stage: SleepSessionRecord.Stage, dataType: String, sourceName: String):
            List<Map<String, Any>> {
        return listOf(
            mapOf<String, Any>(
                "stage" to stage.stage,
                "value" to ChronoUnit.MINUTES.between(stage.startTime, stage.endTime),
                "date_from" to stage.startTime.toEpochMilli(),
                "date_to" to stage.endTime.toEpochMilli(),
                "source_id" to "",
                "source_name" to sourceName,
            ),
        );
    }

    // TODO: Find alternative to SOURCE_ID or make it nullable?
    fun convertRecord(record: Any, dataType: String): List<Map<String, Any>> {
        val metadata = (record as Record).metadata
        when (record) {
            is WeightRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.weight.inKilograms,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is HeightRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.height.inMeters,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is BodyFatRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.percentage.value,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is StepsRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.count,
                    "date_from" to record.startTime.toEpochMilli(),
                    "date_to" to record.endTime.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is ActiveCaloriesBurnedRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.energy.inKilocalories,
                    "date_from" to record.startTime.toEpochMilli(),
                    "date_to" to record.endTime.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is HeartRateRecord -> return record.samples.map {
                mapOf<String, Any>(
                    "value" to it.beatsPerMinute,
                    "date_from" to it.time.toEpochMilli(),
                    "date_to" to it.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                )
            }

            is BodyTemperatureRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.temperature.inCelsius,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is BloodPressureRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to if (dataType == BLOOD_PRESSURE_DIASTOLIC) record.diastolic.inMillimetersOfMercury else record.systolic.inMillimetersOfMercury,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is OxygenSaturationRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.percentage.value,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is BloodGlucoseRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.level.inMilligramsPerDeciliter,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is DistanceRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.distance.inMeters,
                    "date_from" to record.startTime.toEpochMilli(),
                    "date_to" to record.endTime.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is HydrationRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.volume.inLiters,
                    "date_from" to record.startTime.toEpochMilli(),
                    "date_to" to record.endTime.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )

            is SleepSessionRecord -> return listOf(
                mapOf<String, Any>(
                    "date_from" to record.startTime.toEpochMilli(),
                    "date_to" to record.endTime.toEpochMilli(),
                    "value" to ChronoUnit.MINUTES.between(record.startTime, record.endTime),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                ),
            )
            is RestingHeartRateRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.beatsPerMinute,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                )
            )

            is BasalMetabolicRateRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.basalMetabolicRate.inKilocaloriesPerDay,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                )
            )

            is FloorsClimbedRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.floors,
                    "date_from" to record.startTime.toEpochMilli(),
                    "date_to" to record.endTime.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                )
            )

            is RespiratoryRateRecord -> return listOf(
                mapOf<String, Any>(
                    "value" to record.rate,
                    "date_from" to record.time.toEpochMilli(),
                    "date_to" to record.time.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                )
            )

            is NutritionRecord -> return listOf(
                mapOf<String, Any>(
                    "calories" to record.energy!!.inKilocalories,
                    "protein" to record.protein!!.inGrams,
                    "carbs" to record.totalCarbohydrate!!.inGrams,
                    "fat" to record.totalFat!!.inGrams,
                    "name" to record.name!!,
                    "mealType" to (MapTypeToMealTypeHC[record.mealType] ?: MEAL_TYPE_UNKNOWN),
                    "date_from" to record.startTime.toEpochMilli(),
                    "date_to" to record.endTime.toEpochMilli(),
                    "source_id" to "",
                    "source_name" to metadata.dataOrigin.packageName,
                )
            )
            // is ExerciseSessionRecord -> return listOf(mapOf<String, Any>("value" to ,
            //                                             "date_from" to ,
            //                                             "date_to" to ,
            //                                             "source_id" to "",
            //                                             "source_name" to metadata.dataOrigin.packageName))
            else -> throw IllegalArgumentException("Health data type not supported") // TODO: Exception or error?
        }
    }

    //TODO rewrite sleep to fit new update better --> compare with Apple and see if we should not adopt a single type with attached stages approach
    fun writeHCData(call: MethodCall, result: Result) {
        val type = call.argument<String>("dataTypeKey")!!
        val startTime = call.argument<Long>("startTime")!!
        val endTime = call.argument<Long>("endTime")!!
        val value = call.argument<Double>("value")!!
        val record = when (type) {
            BODY_FAT_PERCENTAGE -> BodyFatRecord(
                time = Instant.ofEpochMilli(startTime),
                percentage = Percentage(value),
                zoneOffset = null,
            )

            HEIGHT -> HeightRecord(
                time = Instant.ofEpochMilli(startTime),
                height = Length.meters(value),
                zoneOffset = null,
            )

            WEIGHT -> WeightRecord(
                time = Instant.ofEpochMilli(startTime),
                weight = Mass.kilograms(value),
                zoneOffset = null,
            )

            STEPS -> StepsRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                count = value.toLong(),
                startZoneOffset = null,
                endZoneOffset = null,
            )

            ACTIVE_ENERGY_BURNED -> ActiveCaloriesBurnedRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                energy = Energy.kilocalories(value),
                startZoneOffset = null,
                endZoneOffset = null,
            )

            HEART_RATE -> HeartRateRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                samples = listOf<HeartRateRecord.Sample>(
                    HeartRateRecord.Sample(
                        time = Instant.ofEpochMilli(startTime),
                        beatsPerMinute = value.toLong(),
                    ),
                ),
                startZoneOffset = null,
                endZoneOffset = null,
            )

            BODY_TEMPERATURE -> BodyTemperatureRecord(
                time = Instant.ofEpochMilli(startTime),
                temperature = Temperature.celsius(value),
                zoneOffset = null,
            )

            BLOOD_OXYGEN -> OxygenSaturationRecord(
                time = Instant.ofEpochMilli(startTime),
                percentage = Percentage(value),
                zoneOffset = null,
            )

            BLOOD_GLUCOSE -> BloodGlucoseRecord(
                time = Instant.ofEpochMilli(startTime),
                level = BloodGlucose.milligramsPerDeciliter(value),
                zoneOffset = null,
            )

            DISTANCE_DELTA -> DistanceRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                distance = Length.meters(value),
                startZoneOffset = null,
                endZoneOffset = null,
            )

            WATER -> HydrationRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                volume = Volume.liters(value),
                startZoneOffset = null,
                endZoneOffset = null,
            )
            SLEEP_ASLEEP -> SleepSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = null,
                endZoneOffset = null,
                stages = listOf(SleepSessionRecord.Stage(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), SleepSessionRecord.STAGE_TYPE_SLEEPING)),
            )
            SLEEP_LIGHT -> SleepSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = null,
                endZoneOffset = null,
                stages = listOf(SleepSessionRecord.Stage(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), SleepSessionRecord.STAGE_TYPE_LIGHT)),
            )
            SLEEP_DEEP -> SleepSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = null,
                endZoneOffset = null,
                stages = listOf(SleepSessionRecord.Stage(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), SleepSessionRecord.STAGE_TYPE_DEEP)),
            )
            SLEEP_REM -> SleepSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = null,
                endZoneOffset = null,
                stages = listOf(SleepSessionRecord.Stage(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), SleepSessionRecord.STAGE_TYPE_REM)),
            )
            SLEEP_OUT_OF_BED -> SleepSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = null,
                endZoneOffset = null,
                stages = listOf(SleepSessionRecord.Stage(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), SleepSessionRecord.STAGE_TYPE_OUT_OF_BED)),
            )
            SLEEP_AWAKE -> SleepSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = null,
                endZoneOffset = null,
                stages = listOf(SleepSessionRecord.Stage(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), SleepSessionRecord.STAGE_TYPE_AWAKE)),
            )
            SLEEP_SESSION -> SleepSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = null,
                endZoneOffset = null,
            )

            RESTING_HEART_RATE -> RestingHeartRateRecord(
                time = Instant.ofEpochMilli(startTime),
                beatsPerMinute = value.toLong(),
                zoneOffset = null,
            )

            BASAL_ENERGY_BURNED -> BasalMetabolicRateRecord(
                time = Instant.ofEpochMilli(startTime),
                basalMetabolicRate = Power.kilocaloriesPerDay(value),
                zoneOffset = null,
            )

            FLIGHTS_CLIMBED -> FloorsClimbedRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                floors = value,
                startZoneOffset = null,
                endZoneOffset = null,
            )

            RESPIRATORY_RATE -> RespiratoryRateRecord(
                time = Instant.ofEpochMilli(startTime),
                rate = value,
                zoneOffset = null,
            )
            // AGGREGATE_STEP_COUNT -> StepsRecord()
            BLOOD_PRESSURE_SYSTOLIC -> throw IllegalArgumentException("You must use the [writeBloodPressure] API ")
            BLOOD_PRESSURE_DIASTOLIC -> throw IllegalArgumentException("You must use the [writeBloodPressure] API ")
            WORKOUT -> throw IllegalArgumentException("You must use the [writeWorkoutData] API ")
            NUTRITION -> throw IllegalArgumentException("You must use the [writeMeal] API ")
            else -> throw IllegalArgumentException("The type $type was not supported by the Health plugin or you must use another API ")
        }
        scope.launch {
            try {
                healthConnectClient.insertRecords(listOf(record))
                result.success(true)
            } catch (e: Exception) {
                result.success(false)
            }
        }
    }

    fun writeWorkoutHCData(call: MethodCall, result: Result) {
        val type = call.argument<String>("activityType")!!
        val startTime = Instant.ofEpochMilli(call.argument<Long>("startTime")!!)
        val endTime = Instant.ofEpochMilli(call.argument<Long>("endTime")!!)
        val totalEnergyBurned = call.argument<Int>("totalEnergyBurned")
        val totalDistance = call.argument<Int>("totalDistance")
        if(workoutTypeMapHealthConnect.containsKey(type) == false) {
            result.success(false)
            Log.w("FLUTTER_HEALTH::ERROR", "[Health Connect] Workout type not supported")
            return
        }
        val workoutType = workoutTypeMapHealthConnect[type]!!

        scope.launch {
            try {
                val list = mutableListOf<Record>()
                list.add(
                    ExerciseSessionRecord(
                        startTime = startTime,
                        startZoneOffset = null,
                        endTime = endTime,
                        endZoneOffset = null,
                        exerciseType = workoutType,
                        title = type,
                    ),
                )
                if (totalDistance != null) {
                    list.add(
                        DistanceRecord(
                            startTime = startTime,
                            startZoneOffset = null,
                            endTime = endTime,
                            endZoneOffset = null,
                            distance = Length.meters(totalDistance.toDouble()),
                        ),
                    )
                }
                if (totalEnergyBurned != null) {
                    list.add(
                        TotalCaloriesBurnedRecord(
                            startTime = startTime,
                            startZoneOffset = null,
                            endTime = endTime,
                            endZoneOffset = null,
                            energy = Energy.kilocalories(totalEnergyBurned.toDouble()),
                        ),
                    )
                }
                healthConnectClient.insertRecords(
                    list,
                )
                result.success(true)
                Log.i("FLUTTER_HEALTH::SUCCESS", "[Health Connect] Workout was successfully added!")
            } catch (e: Exception) {
                Log.w(
                    "FLUTTER_HEALTH::ERROR",
                    "[Health Connect] There was an error adding the workout",
                )
                Log.w("FLUTTER_HEALTH::ERROR", e.message ?: "unknown error")
                Log.w("FLUTTER_HEALTH::ERROR", e.stackTrace.toString())
                result.success(false)
            }
        }
    }

    fun writeBloodPressureHC(call: MethodCall, result: Result) {
        val systolic = call.argument<Double>("systolic")!!
        val diastolic = call.argument<Double>("diastolic")!!
        val startTime = Instant.ofEpochMilli(call.argument<Long>("startTime")!!)
        val endTime = Instant.ofEpochMilli(call.argument<Long>("endTime")!!)

        scope.launch {
            try {
                healthConnectClient.insertRecords(
                    listOf(
                        BloodPressureRecord(
                            time = startTime,
                            systolic = Pressure.millimetersOfMercury(systolic),
                            diastolic = Pressure.millimetersOfMercury(diastolic),
                            zoneOffset = null,
                        ),
                    ),
                )
                result.success(true)
                Log.i(
                    "FLUTTER_HEALTH::SUCCESS",
                    "[Health Connect] Blood pressure was successfully added!",
                )
            } catch (e: Exception) {
                Log.w(
                    "FLUTTER_HEALTH::ERROR",
                    "[Health Connect] There was an error adding the blood pressure",
                )
                Log.w("FLUTTER_HEALTH::ERROR", e.message ?: "unknown error")
                Log.w("FLUTTER_HEALTH::ERROR", e.stackTrace.toString())
                result.success(false)
            }
        }
    }

    fun deleteHCData(call: MethodCall, result: Result) {
        val type = call.argument<String>("dataTypeKey")!!
        val startTime = Instant.ofEpochMilli(call.argument<Long>("startTime")!!)
        val endTime = Instant.ofEpochMilli(call.argument<Long>("endTime")!!)
        if(!MapToHCType.containsKey(type)) {
            Log.w("FLUTTER_HEALTH::ERROR", "Datatype " + type + " not found in HC")
            result.success(false)
            return
        }
        val classType = MapToHCType[type]!!

        scope.launch {
            try {
                healthConnectClient.deleteRecords(
                    recordType = classType,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                )
                result.success(true)
            } catch (e: Exception) {
                result.success(false)
            }
        }
    }

    val MapSleepStageToType = hashMapOf<Int, String>(
        1 to SLEEP_AWAKE,
        2 to SLEEP_ASLEEP,
        3 to SLEEP_OUT_OF_BED,
        4 to SLEEP_LIGHT,
        5 to SLEEP_DEEP,
        6 to SLEEP_REM,
    )

    private val MapMealTypeToTypeHC = hashMapOf<String, Int>(
        BREAKFAST to MEAL_TYPE_BREAKFAST,
        LUNCH to MEAL_TYPE_LUNCH,
        DINNER to MEAL_TYPE_DINNER,
        SNACK to MEAL_TYPE_SNACK,
        MEAL_UNKNOWN to MEAL_TYPE_UNKNOWN,
    )

    private val MapTypeToMealTypeHC = hashMapOf<Int, String>(
        MEAL_TYPE_BREAKFAST to BREAKFAST,
        MEAL_TYPE_LUNCH to LUNCH,
        MEAL_TYPE_DINNER to DINNER,
        MEAL_TYPE_SNACK to SNACK,
        MEAL_TYPE_UNKNOWN to MEAL_UNKNOWN,
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
