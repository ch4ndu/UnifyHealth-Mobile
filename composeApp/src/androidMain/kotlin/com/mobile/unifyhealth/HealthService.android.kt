@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.mobile.unifyhealth

import android.content.Context
import com.mobile.unifyhealth.model.HealthData
import com.mobile.unifyhealth.model.HealthDataType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

actual class HealthService(private val context: Context) {
    private val activeProvider: HealthDataProvider = HealthConnectProvider(context)

    init {
        GlobalScope.launch(Dispatchers.Default) {
            hasPermissions(HealthDataType.entries.toSet())
        }
//        activeProvider = SamsungSdkProvider(context)
    }

    actual suspend fun isAvailable(): Boolean {
//        return true

        return activeProvider.connect()
//        if (activeProvider != null) return true

//        val samsungProvider = SamsungSdkProvider(context)
//        if (samsungProvider.connect()) {
//            activeProvider = samsungProvider
//            return true
//        }

//        val healthConnectStatus = HealthConnectClient.getSdkStatus(context)
//        Log.d("chandu", "healthConnectStatus:$healthConnectStatus")
//        when (healthConnectStatus) {
//            HealthConnectClient.SDK_AVAILABLE -> {
//
////                val provider = HealthConnectProvider(context)
//                val provider = activeProvider
//                if (provider.connect()) {
////                    activeProvider = provider
//                    return true
//                }
//            }
//
//            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
//                return false
//            }
//
//            else -> {
//                return false
//            }
//        }
//
//        return false
    }

    actual val permissionsToRequest: Set<String>
        get() = activeProvider.permissionsToRequest

    actual val isPermissionSheetVisible: StateFlow<Boolean> =
        activeProvider.isPermissionSheetVisible

    actual fun showPermissionSheet() {
        activeProvider.showPermissionSheet()
    }

    actual fun hidePermissionSheet() {
        activeProvider.hidePermissionSheet()
    }

    actual suspend fun hasPermissions(readTypes: Set<HealthDataType>): Boolean {
        return activeProvider.hasPermissions(readTypes)
    }

    actual suspend fun requestAuthorization(readTypes: Set<HealthDataType>) {
        activeProvider.requestAuthorization(readTypes)
    }

    actual suspend fun readData(
        startTime: Instant,
        endTime: Instant,
        type: HealthDataType
    ): List<HealthData> {
        return activeProvider.readData(startTime, endTime, type)
    }

    actual val permissionsAvailable: MutableStateFlow<Boolean>
        get() = MutableStateFlow(false)
}