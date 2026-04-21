package com.emergent.codeide

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emergent.codeide.data.EditorViewModel
import com.emergent.codeide.ui.editor.CodeEditor
import com.emergent.codeide.ui.editor.EditorTabs
import com.emergent.codeide.ui.editor.KeyboardShortcutBar
import com.emergent.codeide.ui.explorer.FileExplorer
import com.emergent.codeide.ui.settings.SettingsScreen
import com.emergent.codeide.ui.terminal.TerminalPane
import com.emergent.codeide.ui.theme.*
import com.emergent.codeide.ui.topbar.TopBar
import com.emergent.codeide.util.SyntaxHighlighter
import com.emergent.codeide.util.ToolchainResolver
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request MANAGE_EXTERNAL_STORAGE on Android 11+ so we can read arbitrary project folders.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && !Environment.isExternalStorageManager()) {
            // Launched lazily from UI button too. Non-fatal if user skips.
        }

        setContent {
            CodeIDETheme { CodeIDEApp() }
        }
    }
}

@Composable
fun CodeIDEApp() {
    val vm: EditorViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsState()

    // Initialise default workspace = app's private files dir on first run.
    LaunchedEffect(Unit) {
        if (vm.workspaceRoot == null) {
            val saved = settings.workspaceRoot
            val dir = if (saved.isNotBlank() && File(saved).exists()) File(saved)
            else File(context.filesDir, "workspace").apply { mkdirs() }
            // Seed a C++ starter if empty
            if (dir.listFiles().isNullOrEmpty()) {
                File(dir, "main.cpp").writeText(
                    """
                    #include <iostream>
                    using namespace std;

                    int main() {
                        cout << "Hello from CodeIDE!" << endl;
                        return 0;
                    }
                    """.trimIndent()
                )
                File(dir, "README.md").writeText(
                    "# CodeIDE Workspace\nEdit files here. Open Terminal to compile.\n"
                )
            }
            vm.setWorkspace(dir)
        }
    }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Resolve a File-equivalent path when possible; otherwise fall back to private copy root.
        val tree = uri.path ?: return@rememberLauncherForActivityResult
        val seg = tree.substringAfter("/tree/").replace("primary:", "").replace(':', '/')
        val root = File("/storage/emulated/0/$seg")
        if (root.exists() && root.canRead()) {
            vm.setWorkspace(root)
        } else {
            // As a safe default for newer Androids, export a dir in app files and inform.
            val fallback = File(context.filesDir, "workspace").apply { mkdirs() }
            vm.setWorkspace(fallback)
        }
    }

    fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && !Environment.isExternalStorageManager()) {
            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            i.data = Uri.parse("package:${context.packageName}")
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    // --- Dialog states ---
    var creatingFileIn by remember { mutableStateOf<File?>(null) }
    var creatingFolderIn by remember { mutableStateOf<File?>(null) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var nameInput by remember { mutableStateOf("") }

    if (vm.showSettings) {
        val termuxOk = ToolchainResolver.isTermuxInstalled(context)
        val tools = remember(termuxOk) {
            listOf("g++","gcc","clang++","clang","python","python3","node","cmake","make")
                .associateWith { ToolchainResolver.resolve(it) }
        }
        SettingsScreen(
            settings = settings,
            onBack = { vm.showSettings = false },
            onShowKeyboardChange = { scope.launch { vm.settingsRepo.setShowSoftKeyboard(it) } },
            onSyntaxChange = { scope.launch { vm.settingsRepo.setSyntax(it) } },
            onWrapChange = { scope.launch { vm.settingsRepo.setWordWrap(it) } },
            onFontSizeChange = { scope.launch { vm.settingsRepo.setFontSize(it) } },
            onTabSizeChange = { scope.launch { vm.settingsRepo.setTabSize(it) } },
            termuxInstalled = termuxOk,
            onInstallTermux = { ToolchainResolver.openTermuxPlayStore(context) },
            toolchainStatus = tools,
        )
        return
    }

    val title = vm.openFiles.getOrNull(vm.activeIndex)?.file?.name ?: "CodeIDE"
    Scaffold(
        topBar = {
            TopBar(
                title = title,
                onToggleSidebar = { vm.showSidebar = !vm.showSidebar },
                onToggleTerminal = { vm.showTerminal = !vm.showTerminal },
                onOpenSettings = { vm.showSettings = true },
                onRun = {
                    vm.saveActive { file ->
                        vm.showTerminal = true
                        file?.let { scheduleBuildCommand(vm, it) }
                    }
                },
                onSave = { vm.saveActive() },
                onNewFile = { creatingFileIn = vm.workspaceRoot },
                onNewFolder = { creatingFolderIn = vm.workspaceRoot },
                onPickFolder = {
                    requestAllFilesAccess()
                    pickFolderLauncher.launch(null)
                }
            )
        },
        containerColor = VsBg,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.weight(1f)) {
                if (vm.showSidebar) {
                    FileExplorer(
                        root = vm.workspaceRoot,
                        onOpenFile = { vm.openFile(it) },
                        onPickWorkspace = {
                            requestAllFilesAccess()
                            pickFolderLauncher.launch(null)
                        },
                        onCreateFile = { parent -> creatingFileIn = parent; nameInput = "" },
                        onCreateFolder = { parent -> creatingFolderIn = parent; nameInput = "" },
                        onRename = { target -> renameTarget = target; nameInput = target.name },
                        onDelete = { target -> deleteTarget = target }
                    )
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    EditorTabs(
                        tabs = vm.openFiles,
                        activeIndex = vm.activeIndex,
                        onSelect = { vm.activeIndex = it },
                        onClose = { vm.closeTab(it) }
                    )
                    Box(Modifier.weight(1f)) {
                        val of = vm.openFiles.getOrNull(vm.activeIndex)
                        if (of != null) {
                            CodeEditor(
                                value = of.content,
                                onValueChange = { vm.updateActiveContent(it) },
                                lang = SyntaxHighlighter.detectLang(of.file.name),
                                fontSize = settings.fontSize,
                                showSoftKeyboard = settings.showSoftKeyboard,
                                syntaxOn = settings.syntaxHighlighting,
                            )
                        } else {
                            EmptyEditor()
                        }
                    }
                    if (of_is_open(vm)) {
                        KeyboardShortcutBar(
                            onInsert = { s -> insertIntoActive(vm, s) },
                            onTab = { insertIntoActive(vm, "    ") },
                            onUndo = { /* Simple no-op; hook future history */ },
                            onRedo = { },
                        )
                    }
                }
            }
            if (vm.showTerminal && vm.workspaceRoot != null) {
                TerminalPane(
                    workingDir = vm.workspaceRoot!!,
                    onClose = { vm.showTerminal = false },
                    pendingCommand = vm.pendingCommand,
                    onPendingConsumed = { vm.pendingCommand = null },
                )
            }
            StatusBar(
                fileName = vm.openFiles.getOrNull(vm.activeIndex)?.file?.name,
                lang = vm.openFiles.getOrNull(vm.activeIndex)
                    ?.file?.name?.let { SyntaxHighlighter.detectLang(it) } ?: "plain"
            )
        }
    }

    // Dialogs
    creatingFileIn?.let { parent ->
        NameDialog(
            title = "New file",
            initial = nameInput,
            onDismiss = { creatingFileIn = null },
            onConfirm = { name -> vm.newFileAt(parent, name); creatingFileIn = null }
        )
    }
    creatingFolderIn?.let { parent ->
        NameDialog(
            title = "New folder",
            initial = nameInput,
            onDismiss = { creatingFolderIn = null },
            onConfirm = { name -> vm.newFolderAt(parent, name); creatingFolderIn = null }
        )
    }
    renameTarget?.let { target ->
        NameDialog(
            title = "Rename",
            initial = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name -> vm.renameNode(target, name); renameTarget = null }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            containerColor = VsPanel,
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${target.name}?", color = VsText) },
            text = { Text("This cannot be undone.", color = VsTextMuted) },
            confirmButton = {
                TextButton(onClick = { vm.deleteNode(target); deleteTarget = null }) {
                    Text("Delete", color = VsAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = VsTextMuted)
                }
            }
        )
    }
}

private fun of_is_open(vm: EditorViewModel): Boolean =
    vm.openFiles.isNotEmpty() && vm.activeIndex in vm.openFiles.indices

private fun insertIntoActive(vm: EditorViewModel, s: String) {
    val i = vm.activeIndex
    val of = vm.openFiles.getOrNull(i) ?: return
    val v = of.content
    val start = v.selection.start.coerceIn(0, v.text.length)
    val end = v.selection.end.coerceIn(0, v.text.length)
    val newText = v.text.substring(0, start) + s + v.text.substring(end)
    val newPos = start + s.length
    vm.updateActiveContent(
        TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newPos, newPos)
        )
    )
}

/** Kick off build+run command in the terminal session for the saved file. */
private fun scheduleBuildCommand(vm: EditorViewModel, f: File) {
    val ext = f.extension.lowercase()
    val cmd = when (ext) {
        "cpp","cc","cxx" -> {
            val cxx = ToolchainResolver.resolve("g++") ?: ToolchainResolver.resolve("clang++")
            if (cxx == null) {
                "echo '[CodeIDE] No C++ compiler found. Install Termux then: pkg install clang'"
            } else {
                val out = File(f.parentFile, f.nameWithoutExtension).absolutePath
                "$cxx '${f.absolutePath}' -std=c++17 -O2 -o '$out' && '$out'"
            }
        }
        "c" -> {
            val cc = ToolchainResolver.resolve("gcc") ?: ToolchainResolver.resolve("clang")
            if (cc == null) "echo '[CodeIDE] No C compiler found. Install Termux then: pkg install clang'"
            else {
                val out = File(f.parentFile, f.nameWithoutExtension).absolutePath
                "$cc '${f.absolutePath}' -O2 -o '$out' && '$out'"
            }
        }
        "py" -> {
            val py = ToolchainResolver.resolve("python3") ?: ToolchainResolver.resolve("python")
            if (py == null) "echo '[CodeIDE] Python not found. Install Termux then: pkg install python'"
            else "$py '${f.absolutePath}'"
        }
        "js" -> {
            val node = ToolchainResolver.resolve("node")
            if (node == null) "echo '[CodeIDE] Node not found. Install Termux then: pkg install nodejs'"
            else "$node '${f.absolutePath}'"
        }
        else -> "echo '[CodeIDE] No run action for .$ext files'"
    }
    // The terminal session reads stdin; we queue via a small deferred loop.
    // Since TerminalPane owns its own session, we emit the command as the user's next input:
    vm.pendingCommand = cmd
}

@Composable
private fun EmptyEditor() {
    Column(
        Modifier.fillMaxSize().background(VsBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CodeIDE", color = VsText, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text("Open a folder and a file to start editing.",
            color = VsTextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun StatusBar(fileName: String?, lang: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(VsStatusBar)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(fileName ?: "No file", color = VsText, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text(lang.uppercase(), color = VsText, fontSize = 11.sp)
    }
}

@Composable
private fun NameDialog(
    title: String, initial: String,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        containerColor = VsPanel,
        onDismissRequest = onDismiss,
        title = { Text(title, color = VsText) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VsText, unfocusedTextColor = VsText,
                    focusedBorderColor = VsAccent, unfocusedBorderColor = VsBorder,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("OK", color = VsAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = VsTextMuted) }
        }
    )
}
