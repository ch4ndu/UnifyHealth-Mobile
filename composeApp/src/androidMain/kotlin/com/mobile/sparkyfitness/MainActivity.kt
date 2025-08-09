package com.mobile.sparkyfitness

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private lateinit var healthService: HealthService
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        healthService = HealthService(this)

        setContent {
            // Create the launcher using the more stable PermissionController method
            val requestPermission = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) { grantedPermissions ->
                Log.d("chandu", "grantedPermissions:$grantedPermissions")
                // After the user responds, hide the sheet signal
                healthService.hidePermissionSheet()
                // You can now re-check permissions or update the UI
            }
            val sheetVisible =
                healthService.isPermissionSheetVisible.collectAsStateWithLifecycle(false)
            Log.d("chandu", "sheetVisible:${sheetVisible.value}")

            // This effect observes the signal from your HealthService
            LaunchedEffect(sheetVisible.value) {
                if (sheetVisible.value) {
                    Log.d("chandu", "permissionsToRequest: ${healthService.permissionsToRequest}")
                    requestPermission.launch(healthService.permissionsToRequest)
                }
            }
            App(healthService)
        }
    }
}