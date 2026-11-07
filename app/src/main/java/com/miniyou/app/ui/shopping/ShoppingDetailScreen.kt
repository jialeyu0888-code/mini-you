package com.miniyou.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.InfoRow
import com.miniyou.app.ui.components.Pill
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.ContentWidth
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ShoppingDetailScreen(id: Long, nav: NavController) {
    val app = app()
    val item by app.db.shopping().observe(id).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    val back: () -> Unit = { nav.popBackStack() }

    AppScaffold(topBar = {
        BackTopBar("商品详情", back) {
            if (item != null) {
                IconButton(onClick = { nav.navigate(Routes.shoppingEdit(id)) }) { Icon(Icons.Outlined.Edit, "编辑") }
                IconButton(onClick = { confirm = true }) { Icon(Icons.Outlined.Delete, "删除") }
            }
        }
    }) { pad ->
        val current = item
        if (current == null) return@AppScaffold
        ContentWidth(maxWidth = 720.dp, modifier = Modifier.padding(pad)) {
            ShoppingDetailContent(current, onEdit = { nav.navigate(Routes.shoppingEdit(id)) }) {
                scope.launch {
                    app.db.shopping().upsert(current.copy(status = (current.status + 1).coerceAtMost(ShoppingStatus.BOUGHT), updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }
    val current = item
    if (confirm && current != null) {
        ConfirmDialog("删除商品", "确定删除「${current.name}」吗？", onDismiss = { confirm = false }) {
            scope.launch {
                app.db.shopping().delete(current)
                back()
            }
        }
    }
}

@Composable
private fun ShoppingDetailContent(item: ShoppingItem, onEdit: () -> Unit, onAdvance: () -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (item.photo != null) {
            AsyncImage(
                File(item.photo), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(24.dp)),
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Text(categoryEmoji(item.category), fontSize = 48.sp) }
        }
        SoftCard {
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(ShoppingStatus.labels.getOrElse(item.status) { "计划购买" }, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                Pill(item.category)
                if (item.channel.isNotBlank()) Pill(item.channel, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        SoftCard {
            InfoRow("单价", item.price?.let(::money) ?: "未填")
            InfoRow("数量", item.quantity.toString())
            if (item.price != null) InfoRow("合计", money(item.total))
            InfoRow("渠道", item.channel.ifBlank { "未填" })
            if (item.note.isNotBlank()) InfoRow("备注", item.note)
        }
        if (item.status < ShoppingStatus.BOUGHT) {
            Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Text(if (item.status == ShoppingStatus.PLANNED) "改为「决定未买」" else "标记为已购买", fontWeight = FontWeight.Medium)
            }
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
            Text("  编辑这条记录")
        }
        Spacer(Modifier.height(24.dp))
    }
}
