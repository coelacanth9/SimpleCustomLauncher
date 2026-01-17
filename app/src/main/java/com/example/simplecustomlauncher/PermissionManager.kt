package com.example.simplecustomlauncher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat

object PermissionManager {
    val CALENDAR_PERMISSIONS = arrayOf(android.Manifest.permission.READ_CALENDAR)

    fun checkPermissions(context: android.content.Context, permissions: Array<String>): Boolean {
        return permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun RequestPermissions(
    context: android.content.Context,
    permissions: Array<String>,
    onResult: (Boolean) -> Unit
) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onResult(result.values.all { it })
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!PermissionManager.checkPermissions(context, permissions)) {
            launcher.launch(permissions)
        } else {
            onResult(true)
        }
    }
}