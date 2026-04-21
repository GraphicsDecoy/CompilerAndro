package com.emergent.codeide.ui.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emergent.codeide.ui.theme.VsBg
import com.emergent.codeide.ui.theme.VsBorder
import com.emergent.codeide.ui.theme.VsText
import com.emergent.codeide.ui.theme.VsTextMuted

@Composable
fun TopBar(
    title: String,
    onToggleSidebar: () -> Unit,
    onToggleTerminal: () -> Unit,
    onOpenSettings: () -> Unit,
    onRun: () -> Unit,
    onSave: () -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onPickFolder: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = VsBg) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(VsBg)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.Menu, "Menu", tint = VsText)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Open Folder…") },
                            leadingIcon = { Icon(Icons.Outlined.FolderOpen, null) },
                            onClick = { menuOpen = false; onPickFolder() })
                        DropdownMenuItem(
                            text = { Text("New File") },
                            leadingIcon = { Icon(Icons.Outlined.InsertDriveFile, null) },
                            onClick = { menuOpen = false; onNewFile() })
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) },
                            onClick = { menuOpen = false; onNewFolder() })
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Save") },
                            leadingIcon = { Icon(Icons.Outlined.Save, null) },
                            onClick = { menuOpen = false; onSave() })
                        DropdownMenuItem(
                            text = { Text("Run / Build") },
                            leadingIcon = { Icon(Icons.Outlined.PlayArrow, null) },
                            onClick = { menuOpen = false; onRun() })
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                            onClick = { menuOpen = false; onOpenSettings() })
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(title, color = VsText, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleSidebar) {
                    Icon(Icons.Outlined.ViewSidebar, "Sidebar", tint = VsTextMuted)
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Outlined.Save, "Save", tint = VsTextMuted)
                }
                IconButton(onClick = onRun) {
                    Icon(Icons.Outlined.PlayArrow, "Run", tint = VsTextMuted)
                }
                IconButton(onClick = onToggleTerminal) {
                    Icon(Icons.Outlined.Terminal, "Terminal", tint = VsTextMuted)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, "Settings", tint = VsTextMuted)
                }
            }
            Divider(color = VsBorder, thickness = 1.dp)
        }
    }
}
