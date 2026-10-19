package com.familytracker.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

enum class PermissionStep { Location, Background, Notifications }

object Permissions {

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasLocation(context: Context) =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBackgroundLocation(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    fun hasNotifications(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            granted(context, Manifest.permission.POST_NOTIFICATIONS)

    /** The steps this Android version needs, in order. */
    fun steps(): List<PermissionStep> = buildList {
        add(PermissionStep.Location)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(PermissionStep.Background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(PermissionStep.Notifications)
    }

    /** First permission still missing, or null when everything is granted. */
    fun nextStep(context: Context): PermissionStep? = when {
        !hasLocation(context) -> PermissionStep.Location
        !hasBackgroundLocation(context) -> PermissionStep.Background
        !hasNotifications(context) -> PermissionStep.Notifications
        else -> null
    }

    fun allGranted(context: Context) = nextStep(context) == null

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) ?: true

    fun appSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // Allowed for sideloaded apps; Play policy restrictions don't apply here.
    @SuppressLint("BatteryLife")
    fun batteryOptimizationIntent(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
}
