package com.familytracker.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.familytracker.R
import com.familytracker.ui.components.AlertTag
import com.familytracker.ui.components.Avatar
import com.familytracker.ui.components.PrimaryButton
import com.familytracker.ui.theme.AlertRed
import com.familytracker.ui.theme.MeBlue
import com.familytracker.util.Navigation

@Composable
fun SheetDragHandle() {
    Box(Modifier.padding(top = 10.dp, bottom = 10.dp)) {
        Surface(
            modifier = Modifier.size(width = 36.dp, height = 4.dp),
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.outline,
        ) {}
    }
}

@Composable
fun FamilySheetContent(
    code: String,
    me: MemberUi?,
    myName: String,
    others: List<MemberUi>,
    selected: MemberUi?,
    bottomInset: Dp,
    onSelect: (MemberUi) -> Unit,
    onMe: () -> Unit,
    onInvite: () -> Unit,
    onCenter: (MemberUi) -> Unit,
    onBack: () -> Unit,
) {
    AnimatedContent(
        targetState = selected,
        contentKey = { it?.uid },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "sheet",
    ) { target ->
        if (target != null) {
            MemberDetailCard(target, bottomInset, onCenter = { onCenter(target) }, onBack = onBack)
        } else {
            FamilyList(code, me, myName, others, bottomInset, onSelect, onMe, onInvite)
        }
    }
}

@Composable
private fun FamilyList(
    code: String,
    me: MemberUi?,
    myName: String,
    others: List<MemberUi>,
    bottomInset: Dp,
    onSelect: (MemberUi) -> Unit,
    onMe: () -> Unit,
    onInvite: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Text(
                stringResource(R.string.your_family),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { MemberChip(stringResource(R.string.you), me?.name ?: myName, isMe = true, alert = false, onClick = onMe) }
                items(others, key = { it.uid }) { row ->
                    MemberChip(row.name.substringBefore(' '), row.name, isMe = false, alert = row.longStay, onClick = { onSelect(row) })
                }
                item { InviteChip(onInvite) }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (others.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_members, code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
        items(others, key = { "row-" + it.uid }) { row ->
            MemberRow(row, onClick = { onSelect(row) })
            HorizontalDivider(Modifier.padding(start = 84.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
        item { Spacer(Modifier.height(bottomInset + 16.dp)) }
    }
}

@Composable
private fun MemberChip(label: String, name: String, isMe: Boolean, alert: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
    ) {
        Box {
            if (isMe) Avatar(name, 56.dp, color = MeBlue) else Avatar(name, 56.dp)
            if (alert) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .background(AlertRed, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InviteChip(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.invite), style = MaterialTheme.typography.labelMedium)
    }
}

/** Laid out like a ride option: avatar, name + area, then distance + last update on the right. */
@Composable
private fun MemberRow(row: MemberUi, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Avatar(row.name, 48.dp, modifier = Modifier.alpha(if (row.dimmed) 0.45f else 1f))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                row.area ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val stay = row.stayText
            if (row.longStay && stay != null) {
                AlertTag("At same place $stay", Modifier.padding(top = 4.dp))
            } else if (stay != null) {
                Text(
                    "At same place for $stay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(row.distance ?: "—", style = MaterialTheme.typography.titleMedium)
            Text(row.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Like a driver card: big name, avatar, key facts, one black action. */
@Composable
private fun MemberDetailCard(row: MemberUi, bottomInset: Dp, onCenter: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = bottomInset + 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    row.area ?: "—",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (row.longStay) AlertTag("At same place ${row.stayText}", Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Avatar(row.name, 64.dp, modifier = Modifier.alpha(if (row.dimmed) 0.45f else 1f))
        }
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 16.dp),
        ) {
            Stat(stringResource(R.string.distance), row.distance ?: "—", Modifier.weight(1f))
            VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
            Stat(
                stringResource(R.string.at_this_place),
                row.stayText ?: "—",
                Modifier.weight(1f),
                alert = row.longStay,
            )
            VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
            Stat(stringResource(R.string.last_update), row.lastUpdate, Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(20.dp))
        val context = LocalContext.current
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                text = stringResource(R.string.navigate),
                icon = Icons.Filled.Directions,
                enabled = row.latLng != null,
                onClick = { row.latLng?.let { Navigation.navigateTo(context, it.latitude, it.longitude, row.name) } },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.center_on_map),
                enabled = row.latLng != null,
                onClick = onCenter,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        if (row.latLng != null && !row.member.isFresh(System.currentTimeMillis())) {
            Text(
                stringResource(R.string.navigate_last_known, row.lastUpdate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                stringResource(R.string.show_all_family),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, alert: Boolean = false) {
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = if (alert) AlertRed else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
