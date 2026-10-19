package com.familytracker.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.familytracker.R
import com.familytracker.ui.components.PrimaryButton
import com.familytracker.util.PermissionStep
import com.familytracker.util.Permissions

/** One clean screen per permission. [onResult] makes the caller re-check what's still missing. */
@Composable
fun PermissionScreen(step: PermissionStep, onResult: () -> Unit) {
    val context = LocalContext.current
    var denials by rememberSaveable(step) { mutableIntStateOf(0) }

    val multiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.none { it }) denials++
        onResult()
    }
    val singleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) denials++
        onResult()
    }

    val steps = Permissions.steps()
    val index = steps.indexOf(step) + 1
    val (icon, title, body) = when (step) {
        PermissionStep.Location -> Triple(Icons.Filled.LocationOn, R.string.perm_location_title, R.string.perm_location_body)
        PermissionStep.Background -> Triple(Icons.Filled.MyLocation, R.string.perm_background_title, R.string.perm_background_body)
        PermissionStep.Notifications -> Triple(Icons.Filled.NotificationsActive, R.string.perm_notifications_title, R.string.perm_notifications_body)
    }

    val request: () -> Unit = {
        when (step) {
            PermissionStep.Location -> multiLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
            PermissionStep.Background -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                singleLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            PermissionStep.Notifications -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                singleLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Text(
            stringResource(R.string.perm_step, index, steps.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.weight(1f))
        StepIcon(icon)
        Spacer(Modifier.height(32.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (step == PermissionStep.Background) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.perm_background_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1.4f))

        // After repeated denials Android stops showing the dialog; settings is the only way left.
        val settingsOnly = denials >= 2
        PrimaryButton(
            text = stringResource(if (settingsOnly) R.string.open_settings else R.string.allow),
            onClick = { if (settingsOnly) context.startActivity(Permissions.appSettingsIntent(context)) else request() },
        )
        if (!settingsOnly && (step == PermissionStep.Background || denials > 0)) {
            TextButton(
                onClick = { context.startActivity(Permissions.appSettingsIntent(context)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(
                    stringResource(R.string.open_settings),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun StepIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}
