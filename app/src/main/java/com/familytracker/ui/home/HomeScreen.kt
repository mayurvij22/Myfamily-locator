package com.familytracker.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familytracker.R
import com.familytracker.location.StayTracker
import com.familytracker.ui.AppViewModel
import com.familytracker.ui.Session
import com.familytracker.ui.components.Avatar
import com.familytracker.ui.components.CardShape
import com.familytracker.ui.components.SheetShape
import com.familytracker.ui.theme.AlertRed
import com.familytracker.ui.theme.White
import com.familytracker.util.Format
import com.familytracker.util.Permissions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TopCardSpace = 96.dp
private val DetailSheetHeight = 380.dp
private const val WEAK_ACCURACY_M = 150f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, session: Session) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val members by vm.members.collectAsStateWithLifecycle()
    val myLocation by vm.myLocation.collectAsStateWithLifecycle()
    val sharing by vm.sharing.collectAsStateWithLifecycle()
    val areas by vm.areas.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }

    var batteryExempt by remember { mutableStateOf(Permissions.isIgnoringBatteryOptimizations(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        batteryExempt = Permissions.isIgnoringBatteryOptimizations(context)
    }

    val myUid = vm.myUid
    val liveMe = myLocation?.toLatLng()
    val rows = remember(members, liveMe, areas, now, myUid) {
        val meDoc = members?.firstOrNull { it.uid == myUid }
        val myPos = liveMe ?: meDoc?.let { m -> if (m.lat != null && m.lng != null) LatLng(m.lat, m.lng) else null }
        buildMemberUi(members.orEmpty(), myUid, myPos, areas, now)
    }
    val me = rows.firstOrNull { it.isMe }
    val others = rows.filterNot { it.isMe }
    val myLatLng = liveMe ?: me?.latLng
    val myAccuracy = myLocation?.accuracy ?: me?.member?.accuracy

    var selectedUid by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = others.firstOrNull { it.uid == selectedUid }

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val camera = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    var fitted by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val peekHeight = 168.dp + navBottom

    fun allPoints(): List<LatLng> = others.mapNotNull { it.latLng } + listOfNotNull(myLatLng)

    fun select(row: MemberUi) {
        selectedUid = row.uid
        scope.launch { sheetState.expand() }
        row.latLng?.let { pos -> scope.launch { camera.animateSafely(CameraUpdateFactory.newLatLngZoom(pos, 16f)) } }
    }

    fun deselect() {
        selectedUid = null
        scope.launch { sheetState.partialExpand() }
    }

    fun recenter() {
        val target = selected?.latLng ?: myLatLng
        scope.launch {
            if (target != null) camera.animateSafely(CameraUpdateFactory.newLatLngZoom(target, 16f))
            else camera.fitTo(allPoints())
        }
    }

    BackHandler(enabled = selected != null) { deselect() }
    BackHandler(enabled = selected == null && sheetState.currentValue == SheetValue.Expanded) {
        scope.launch { sheetState.partialExpand() }
    }

    // Fit everyone once, after the map has loaded and the first snapshot arrived.
    LaunchedEffect(mapLoaded, members != null, myLatLng != null) {
        if (mapLoaded && members != null && !fitted) {
            val points = allPoints()
            if (points.isNotEmpty()) {
                camera.fitTo(points)
                fitted = true
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetShape = SheetShape,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 16.dp,
        sheetDragHandle = { SheetDragHandle() },
        sheetContent = {
            FamilySheetContent(
                code = session.code,
                me = me,
                myName = session.name,
                others = others,
                selected = selected,
                bottomInset = navBottom,
                onSelect = ::select,
                onMe = {
                    selectedUid = null
                    scope.launch { sheetState.partialExpand() }
                    recenter()
                },
                onInvite = { shareCode(context, session.code) },
                onCenter = { row ->
                    row.latLng?.let { pos -> scope.launch { camera.animateSafely(CameraUpdateFactory.newLatLngZoom(pos, 17f)) } }
                },
                onBack = ::deselect,
            )
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxHeightPx = constraints.maxHeight
            val dark = isSystemInDarkTheme()
            val mapStyle = remember(dark) {
                MapStyleOptions.loadRawResourceStyle(context, if (dark) R.raw.map_style_dark else R.raw.map_style_light)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                properties = MapProperties(mapStyleOptions = mapStyle),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = false,
                    mapToolbarEnabled = false,
                    indoorLevelPickerEnabled = false,
                ),
                contentPadding = PaddingValues(
                    top = statusTop + TopCardSpace,
                    bottom = if (selected != null) DetailSheetHeight + navBottom else peekHeight,
                ),
                onMapLoaded = { mapLoaded = true },
                onMapClick = { if (selected != null) deselect() },
            ) {
                others.forEach { row ->
                    val pos = row.latLng ?: return@forEach
                    key(row.uid) {
                        MemberMarker(row, pos, onClick = { select(row) })
                    }
                }
                myLatLng?.let { MyLocationMarker(it, myAccuracy) }
            }

            MyLocationPulse(camera, myLatLng, Modifier.fillMaxSize())

            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                TopCard(
                    code = session.code,
                    sharing = sharing,
                    weakAccuracy = myLocation?.accuracy?.takeIf { it > WEAK_ACCURACY_M },
                    menuOpen = menuOpen,
                    onOpenList = {
                        selectedUid = null
                        scope.launch { sheetState.expand() }
                    },
                    onMenu = { menuOpen = it },
                    onShareCode = { menuOpen = false; shareCode(context, session.code) },
                    onToggleSharing = { vm.setSharing(!sharing) },
                    onLeave = { menuOpen = false; confirmLeave = true },
                )
                if (!batteryExempt) {
                    Spacer(Modifier.height(8.dp))
                    BatteryBanner(onClick = { requestBatteryExemption(context) })
                }
            }

            // Floats just above the sheet's top edge and follows it while dragging.
            Surface(
                onClick = ::recenter,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .offset {
                        val sheetTop = runCatching { sheetState.requireOffset() }
                            .getOrDefault(maxHeightPx - peekHeight.toPx())
                        val y = sheetTop - 48.dp.toPx() - 16.dp.toPx()
                        IntOffset(0, y.coerceAtLeast((statusTop + 180.dp).toPx()).roundToInt())
                    }
                    .size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.recenter))
                }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.leave_confirm_title)) },
            text = { Text(stringResource(R.string.leave_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; vm.leave() }) {
                    Text(stringResource(R.string.leave), color = AlertRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = CardShape,
        )
    }
}

@Composable
private fun TopCard(
    code: String,
    sharing: Boolean,
    weakAccuracy: Float?,
    menuOpen: Boolean,
    onOpenList: () -> Unit,
    onMenu: (Boolean) -> Unit,
    onShareCode: () -> Unit,
    onToggleSharing: () -> Unit,
    onLeave: () -> Unit,
) {
    Surface(
        onClick = onOpenList,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.titleMedium)
                val subtitle = when {
                    !sharing -> stringResource(R.string.sharing_paused)
                    weakAccuracy != null -> stringResource(R.string.weak_location, Format.distance(weakAccuracy))
                    else -> stringResource(R.string.family_label, code)
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sharing && weakAccuracy == null) MaterialTheme.colorScheme.onSurfaceVariant else AlertRed,
                )
            }
            Box {
                IconButton(onClick = { onMenu(true) }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenu(false) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = CardShape,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_code)) },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        onClick = onShareCode,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_my_location)) },
                        leadingIcon = { Icon(Icons.Filled.MyLocation, contentDescription = null) },
                        trailingIcon = { Switch(checked = sharing, onCheckedChange = null) },
                        onClick = onToggleSharing,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.leave_family), color = AlertRed) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AlertRed) },
                        onClick = onLeave,
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Icon(Icons.Filled.BatteryAlert, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.battery_banner), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.fix), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun MemberMarker(row: MemberUi, position: LatLng, onClick: () -> Unit) {
    val state = remember { MarkerState(position) }
    LaunchedEffect(position) { state.position = position }
    MarkerComposable(
        row.name, row.dimmed,
        state = state,
        anchor = Offset(0.5f, 0.5f),
        zIndex = if (row.dimmed) 0f else 1f,
        onClick = { onClick(); true },
    ) {
        Avatar(row.name, 44.dp, ring = true, modifier = Modifier.alpha(if (row.dimmed) 0.55f else 1f))
    }
}

private suspend fun CameraPositionState.animateSafely(update: com.google.android.gms.maps.CameraUpdate) {
    try {
        animate(update, durationMs = 700)
    } catch (_: Exception) {
        // Interrupted by a user gesture, or the map isn't laid out yet.
    }
}

private suspend fun CameraPositionState.fitTo(points: List<LatLng>) {
    if (points.isEmpty()) return
    val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
    val span = StayTracker.androidDistance(
        bounds.southwest.latitude, bounds.southwest.longitude,
        bounds.northeast.latitude, bounds.northeast.longitude,
    )
    // newLatLngBounds zooms to street level when everyone is in one spot.
    if (span < 300f) animateSafely(CameraUpdateFactory.newLatLngZoom(bounds.center, 15f))
    else animateSafely(CameraUpdateFactory.newLatLngBounds(bounds, 160))
}

private fun shareCode(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_code_text, code))
    context.startActivity(Intent.createChooser(send, null))
}

private fun requestBatteryExemption(context: Context) {
    try {
        context.startActivity(Permissions.batteryOptimizationIntent(context))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}
