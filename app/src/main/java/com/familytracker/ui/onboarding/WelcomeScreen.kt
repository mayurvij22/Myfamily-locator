package com.familytracker.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.familytracker.R
import com.familytracker.ui.components.PrimaryButton
import com.familytracker.ui.theme.Black
import com.familytracker.ui.theme.MeBlue
import com.familytracker.ui.theme.White

/** Always black, regardless of the system theme. */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displaySmall, color = White)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.welcome_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = White.copy(alpha = 0.7f),
        )
        FamilyDotsArt(Modifier.weight(1f).fillMaxWidth())
        PrimaryButton(
            text = stringResource(R.string.get_started),
            onClick = onGetStarted,
            containerColor = White,
            contentColor = Black,
        )
    }
}

/** A few connected dots: the family on a map. */
@Composable
private fun FamilyDotsArt(modifier: Modifier) {
    Canvas(modifier.padding(vertical = 32.dp)) {
        val points = listOf(
            Offset(size.width * 0.22f, size.height * 0.30f),
            Offset(size.width * 0.70f, size.height * 0.22f),
            Offset(size.width * 0.55f, size.height * 0.62f),
            Offset(size.width * 0.18f, size.height * 0.78f),
        )
        val me = points[2]
        points.forEach { if (it != me) drawLine(White.copy(alpha = 0.25f), me, it, 2.dp.toPx(), StrokeCap.Round) }
        points.forEach { p ->
            if (p == me) {
                drawCircle(MeBlue.copy(alpha = 0.25f), 28.dp.toPx(), p)
                drawCircle(White, 12.dp.toPx(), p)
                drawCircle(MeBlue, 9.dp.toPx(), p)
            } else {
                drawCircle(White, 18.dp.toPx(), p)
                drawCircle(Black, 15.dp.toPx(), p)
                drawCircle(White, 15.dp.toPx(), p, style = Stroke(2.dp.toPx()))
            }
        }
    }
}
