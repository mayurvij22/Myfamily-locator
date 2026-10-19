package com.familytracker.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.familytracker.ui.theme.MeBlue
import com.familytracker.ui.theme.White
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import androidx.compose.runtime.LaunchedEffect

/**
 * This phone's position, drawn by the map itself so it sits exactly on the right spot.
 * The light-blue circle is the reported accuracy, like in Google Maps.
 */
@Composable
fun MyLocationMarker(position: LatLng, accuracyMeters: Float?) {
    if (accuracyMeters != null && accuracyMeters > 0f) {
        Circle(
            center = position,
            radius = accuracyMeters.toDouble(),
            fillColor = MeBlue.copy(alpha = 0.12f),
            strokeColor = MeBlue.copy(alpha = 0.4f),
            strokeWidth = 2f,
            zIndex = 2f,
        )
    }
    val state = remember { MarkerState(position) }
    LaunchedEffect(position) { state.position = position }
    MarkerComposable(
        state = state,
        anchor = Offset(0.5f, 0.5f),
        zIndex = 3f,
        onClick = { true },
    ) {
        Box(Modifier.size(22.dp).background(White, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(16.dp).background(MeBlue, CircleShape))
        }
    }
}

/** Expanding ring around [position]; draw-only, so map gestures pass straight through. */
@Composable
fun MyLocationPulse(camera: CameraPositionState, position: LatLng?, modifier: Modifier = Modifier) {
    if (position == null) return
    camera.position // Read so this recomposes whenever the camera moves.
    val point = camera.projection?.toScreenLocation(position) ?: return
    val center = Offset(point.x.toFloat(), point.y.toFloat())
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    Spacer(
        modifier.drawBehind {
            val p = pulse.value
            drawCircle(
                MeBlue.copy(alpha = 0.5f * (1f - p)),
                radius = 11.dp.toPx() + 26.dp.toPx() * p,
                center = center,
                style = Stroke(3.dp.toPx()),
            )
        },
    )
}
