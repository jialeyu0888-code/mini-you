package com.miniyou.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecordActionsMenu(
    recordName: String,
    deleteMessage: String,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    deleteLabel: String = "删除$recordName",
    confirmLabel: String = "删除",
) {
    var expanded by remember { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }

    Box(modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.MoreHoriz, "${recordName}更多操作", tint = tint, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onEdit != null) {
                DropdownMenuItem(text = { Text("编辑$recordName") }, onClick = {
                    expanded = false
                    onEdit()
                })
            }
            DropdownMenuItem(text = { Text(deleteLabel, color = MaterialTheme.colorScheme.error) }, onClick = {
                expanded = false
                confirm = true
            })
        }
    }
    if (confirm) {
        ConfirmDialog(deleteLabel, deleteMessage, confirmLabel, onDismiss = { confirm = false }) {
            confirm = false
            onDelete()
        }
    }
}

@Composable
fun DeleteRecordButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label, color = MaterialTheme.colorScheme.error) }
}
