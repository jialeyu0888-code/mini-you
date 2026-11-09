package com.miniyou.app.ui.checkup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniyou.app.data.CheckupSchedule
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.scheduleFor
import com.miniyou.app.ui.components.BulletList
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout

@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    val settings by app().settings.state.collectAsState()
    val info = settings.lmp?.let { PregnancyInfo(it) }
    val current = info?.let { scheduleFor(it.weeks) }
    val layout = LocalWindowLayout.current
    PrenatalPage { pad ->
        val colors = LocalPrenatalColors.current
        Column(Modifier.fillMaxSize().padding(pad)) {
            PrenatalHeader("产检时间表", onBack)
            LazyVerticalGrid(
                columns = GridCells.Fixed(layout.gridColumns),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        (info?.let { "你现在 ${it.weekLabel}。" } ?: "") + "以下为国内常见产检安排，具体项目和时间以建档医院为准。",
                        color = colors.muted, fontSize = 13.sp, lineHeight = 19.sp,
                    )
                }
                items(CheckupSchedule) { entry ->
                    val now = entry == current
                    PrenatalCard(color = if (now) colors.pinkCard else colors.card) {
                        Column {
                            if (now) {
                                PrenatalPill("当前阶段")
                                Spacer(Modifier.height(6.dp))
                            }
                            Text(entry.range, color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(entry.title, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            BulletList(entry.items, color = colors.ink)
                            if (entry.note.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(entry.note, color = colors.muted, fontSize = 12.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
