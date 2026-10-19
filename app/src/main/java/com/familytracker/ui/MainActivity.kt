package com.familytracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familytracker.location.LocationService
import com.familytracker.ui.home.HomeScreen
import com.familytracker.ui.onboarding.JoinScreen
import com.familytracker.ui.onboarding.PermissionScreen
import com.familytracker.ui.onboarding.WelcomeScreen
import com.familytracker.ui.theme.FamilyTrackerTheme
import com.familytracker.util.Permissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamilyTrackerTheme {
                AppRoot()
            }
        }
    }
}

/** Welcome → Join → one screen per permission → map. */
@Composable
private fun AppRoot(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val session by vm.session.collectAsStateWithLifecycle()
    val welcomeSeen by vm.welcomeSeen.collectAsStateWithLifecycle()
    val joining by vm.joining.collectAsStateWithLifecycle()
    val joinError by vm.joinError.collectAsStateWithLifecycle()

    var showWelcome by rememberSaveable { mutableStateOf(!welcomeSeen) }
    var permissionStep by remember { mutableStateOf(Permissions.nextStep(context)) }
    val recheck = {
        permissionStep = Permissions.nextStep(context)
        vm.onResume()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { recheck() }

    val current = session
    val step = permissionStep
    when {
        current == null && showWelcome -> WelcomeScreen(onGetStarted = {
            vm.markWelcomeSeen()
            showWelcome = false
        })

        current == null -> JoinScreen(
            initialName = vm.savedName,
            joining = joining,
            error = joinError,
            onBack = { showWelcome = true },
            onJoin = vm::join,
        )

        step != null -> PermissionScreen(step = step, onResult = recheck)

        else -> {
            LaunchedEffect(current.code) { LocationService.start(context) }
            HomeScreen(vm, current)
        }
    }
}
