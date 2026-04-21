package com.emergent.codeide.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergent.codeide.ui.theme.*
import java.io.File

@Composable
fun FileExplorer(
    root: File?,
    onOpenFile: (File) -> Unit,
    onPickWorkspace: () -> Unit,
    onCreateFile: (parent: File) -> Unit,
    onCreateFolder: (parent: File) -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(VsSidebar)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "EXPLORER",
                color = VsTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            root?.let {
                IconButton(onClick = { onCreateFile(it) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Outlined.NoteAdd, "New file", tint = VsTextMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onCreateFolder(it) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Outlined.CreateNewFolder, "New folder", tint = VsTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
        Divider(color = VsBorder)
        if (root == null) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No folder opened", color = VsTextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onPickWorkspace,
                    colors = ButtonDefaults.buttonColors(containerColor = VsAccent)
                ) { Text("Open Folder", fontSize = 12.sp) }
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TreeNode(
                    file = root,
                    depth = 0,
                    defaultExpanded = true,
                    onOpenFile = onOpenFile,
                    onCreateFile = onCreateFile,
                    onCreateFolder = onCreateFolder,
                    onRename = onRename,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun TreeNode(
    file: File,
    depth: Int,
    defaultExpanded: Boolean = false,
    onOpenFile: (File) -> Unit,
    onCreateFile: (File) -> Unit,
    onCreateFolder: (File) -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit,
) {
    var expanded by remember(file.absolutePath) { mutableStateOf(defaultExpanded) }
    var menuOpen by remember { mutableStateOf(false) }
    val isDir = file.isDirectory
    val indent = (8 + depth * 14).dp

    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clickable {
                if (isDir) expanded = !expanded else onOpenFile(file)
            }
            .padding(start = indent, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isDir) (if (expanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder)
            else Icons.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = if (isDir) Color(0xFFDCB67A) else VsTextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            file.name,
            color = VsText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Outlined.MoreVert, "More", tint = VsTextMuted, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (isDir) {
                    DropdownMenuItem(text = { Text("New File") }, onClick = {
                        menuOpen = false; onCreateFile(file)
                    })
                    DropdownMenuItem(text = { Text("New Folder") }, onClick = {
                        menuOpen = false; onCreateFolder(file)
                    })
                    Divider()
                } else {
                    DropdownMenuItem(text = { Text("Open") }, onClick = {
                        menuOpen = false; onOpenFile(file)
                    })
                }
                DropdownMenuItem(text = { Text("Rename") }, onClick = {
                    menuOpen = false; onRename(file)
                })
                DropdownMenuItem(text = { Text("Delete") }, onClick = {
                    menuOpen = false; onDelete(file)
                })
            }
        }
    }
    if (isDir && expanded) {
        val children = remember(file, expanded) {
            file.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: emptyList()
        }
        children.forEach { child ->
            TreeNode(child, depth + 1, false,
                onOpenFile, onCreateFile, onCreateFolder, onRename, onDelete)
        }
    }
}
