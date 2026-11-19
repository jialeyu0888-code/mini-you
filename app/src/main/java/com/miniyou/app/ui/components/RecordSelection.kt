package com.miniyou.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Stable
internal class RecordSelection(private val ids: MutableState<LongArray?>) {
    val active: Boolean get() = ids.value != null
    var busy by mutableStateOf(false)
    fun has(id: Long): Boolean = ids.value?.contains(id) == true
    /** 长按进入多选：激活并把长按的这条直接选中。 */
    fun beginWith(id: Long) { ids.value = longArrayOf(id) }
    fun stop() { ids.value = null }
    fun replace(values: List<Long>) { ids.value = values.toLongArray() }
    fun toggle(id: Long) {
        val current = ids.value ?: return
        if (!busy) ids.value = if (id in current) current.filterNot { it == id }.toLongArray() else current + id
    }
}

@Composable
internal fun rememberRecordSelection(): RecordSelection {
    val ids = rememberSaveable { mutableStateOf<LongArray?>(null) }
    val selection = remember(ids) { RecordSelection(ids) }
    BackHandler(enabled = selection.active) { if (!selection.busy) selection.stop() }
    return selection
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecordSelectionBar(
    selection: RecordSelection,
    visibleIds: List<Long>,
    recordName: String,
    modifier: Modifier = Modifier,
    onDelete: suspend (Set<Long>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var confirm by rememberSaveable { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val chosen = visibleIds.filter(selection::has)
    // 非多选状态不占任何空间：进入多选靠列表项长按。
    if (!selection.active) return

    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Text("已选 ${chosen.size} 项", Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.labelMedium)
        val allSelected = visibleIds.isNotEmpty() && chosen.size == visibleIds.size
        TextButton(
            enabled = !selection.busy && visibleIds.isNotEmpty(),
            onClick = { selection.replace(if (allSelected) emptyList() else visibleIds) },
        ) { Text(if (allSelected) "取消全选" else "全选") }
        TextButton(enabled = !selection.busy, onClick = selection::stop) { Text("退出多选") }
        TextButton(enabled = chosen.isNotEmpty() && !selection.busy, onClick = { confirm = true }) {
            Text("删除所选", color = if (chosen.isEmpty() || selection.busy)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error)
        }
    }
    if (failed) Text("删除失败，请重试。", color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall)
    if (confirm && selection.active && chosen.isNotEmpty()) {
        ConfirmDialog(
            title = "删除所选",
            message = "确定删除选中的 ${chosen.size} 项${recordName}吗？删除后无法恢复。",
            onDismiss = { confirm = false },
        ) {
            val batch = chosen.toSet()
            confirm = false
            failed = false
            selection.busy = true
            scope.launch {
                try {
                    onDelete(batch)
                    selection.stop()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failed = true
                } finally {
                    selection.busy = false
                }
            }
        }
    }
}

@Composable
internal fun RecordSelectionMark(selection: RecordSelection, id: Long, recordName: String) {
    Checkbox(
        checked = selection.has(id),
        onCheckedChange = { selection.toggle(id) },
        enabled = !selection.busy,
        modifier = Modifier.size(42.dp).semantics { contentDescription = "选择$recordName" },
    )
}
