package cachet.plugins.health

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.PermissionController
import io.flutter.plugin.common.MethodChannel

class HCManager(private val context: Context) {
    companion object {
        const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1
    }

    private val healthConnectClient: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val isSupported: Boolean = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

    fun checkAvailability(): HCAvailability {
        val availability: HCAvailability = when {
            HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> HCAvailability.INSTALLED
            isSupported -> HCAvailability.NOT_INSTALLED
            else -> HCAvailability.NOT_SUPPORTED
        }
        return availability
    }

    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    fun requestPermissions(permissions: List<String>, result: MethodChannel.Result) {

    }
}