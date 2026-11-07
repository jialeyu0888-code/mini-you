package com.miniyou.app.ui.checkup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miniyou.app.data.CheckupSchedule
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.scheduleFor
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.BulletList
import com.miniyou.app.ui.components.Pill
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout

@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    val settings by app().settings.state.collectAsState()
    val info = settings.lmp?.let { PregnancyInfo(it) }
    val current = info?.let { scheduleFor(it.weeks) }
    val layout = LocalWindowLayout.current
    AppScaffold(topBar = { BackTopBar("产检时间表参考", onBack) }) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(layout.gridColumns),
            contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, top = pad.calculateTopPadding() + 4.dp, bottom = pad.calculateBottomPadding() + 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    (info?.let { "你现在 ${it.weekLabel}。" } ?: "") + "以下为国内常见产检安排，具体项目和时间以建档医院为准。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(CheckupSchedule) { e ->
                val now = e == current
                SoftCard(color = if (now) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    if (now) { Pill("当前阶段", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.height(6.dp)) }
                    Text(e.range, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(e.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    BulletList(e.items)
                    if (e.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("💡 ${e.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
