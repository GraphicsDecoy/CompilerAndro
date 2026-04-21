package com.emergent.codeide.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class OpenFile(
    val file: File,
    var dirty: Boolean = false,
    var content: TextFieldValue = TextFieldValue(""),
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    val settingsRepo = SettingsRepository(app)
    val settings = settingsRepo.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly, AppSettings()
    )

    val openFiles = mutableStateListOf<OpenFile>()
    var activeIndex by mutableStateOf(0)
    var workspaceRoot by mutableStateOf<File?>(null)
    var showSidebar by mutableStateOf(true)
    var showTerminal by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    /** Command queued from Run button, consumed by TerminalPane once. */
    var pendingCommand by mutableStateOf<String?>(null)

    fun setWorkspace(dir: File) {
        workspaceRoot = dir
        viewModelScope.launch { settingsRepo.setWorkspaceRoot(dir.absolutePath) }
    }

    fun openFile(f: File) {
        val existing = openFiles.indexOfFirst { it.file.absolutePath == f.absolutePath }
        if (existing >= 0) { activeIndex = existing; return }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                try { f.readText() } catch (e: Exception) { "// Cannot read: ${e.message}" }
            }
            openFiles.add(OpenFile(f, false, TextFieldValue(text)))
            activeIndex = openFiles.lastIndex
        }
    }

    fun updateActiveContent(v: TextFieldValue) {
        val i = activeIndex
        if (i !in openFiles.indices) return
        val cur = openFiles[i]
        openFiles[i] = cur.copy(content = v, dirty = cur.content.text != v.text || cur.dirty)
    }

    fun saveActive(onDone: (File?) -> Unit = {}) {
        val i = activeIndex
        if (i !in openFiles.indices) { onDone(null); return }
        val of = openFiles[i]
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try { of.file.writeText(of.content.text) } catch (_: Exception) {}
            }
            openFiles[i] = of.copy(dirty = false)
            onDone(of.file)
        }
    }

    fun closeTab(index: Int) {
        if (index !in openFiles.indices) return
        openFiles.removeAt(index)
        if (activeIndex >= openFiles.size) activeIndex = (openFiles.size - 1).coerceAtLeast(0)
    }

    fun newFileAt(dir: File, name: String): File? {
        val f = File(dir, name)
        return try {
            if (!f.exists()) f.createNewFile()
            openFile(f); f
        } catch (_: Exception) { null }
    }

    fun newFolderAt(dir: File, name: String): File? {
        val f = File(dir, name)
        return try { f.mkdirs(); f } catch (_: Exception) { null }
    }

    fun renameNode(src: File, newName: String): File? {
        val dest = File(src.parentFile, newName)
        return if (src.renameTo(dest)) {
            openFiles.forEachIndexed { i, of ->
                if (of.file.absolutePath == src.absolutePath)
                    openFiles[i] = of.copy(file = dest)
            }
            dest
        } else null
    }

    fun deleteNode(src: File): Boolean {
        val ok = src.deleteRecursively()
        if (ok) openFiles.removeAll { it.file.absolutePath.startsWith(src.absolutePath) }
        return ok
    }
}
