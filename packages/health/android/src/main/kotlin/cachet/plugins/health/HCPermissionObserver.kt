package cachet.plugins.health

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.flutter.plugin.common.MethodChannel

class HCPermissionObserver(
    private val registry: ActivityResultRegistry,
    private val permissions: Set<String>,
    private val result: MethodChannel.Result,
): DefaultLifecycleObserver {
    private lateinit var getPermissions : ActivityResultLauncher<Set<String>>

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        val contract = PermissionController.createRequestPermissionResultContract()
        getPermissions = registry.register("RequestPermission", owner, contract) { granted ->
            // 権限取得後の処理
            if (granted.containsAll(permissions)) {
                result.success(true)
            } else {
                result.error("Not granted", null, null)
            }
        }
    }

    fun requestPermissions() {
        getPermissions.launch(permissions)
    }
}