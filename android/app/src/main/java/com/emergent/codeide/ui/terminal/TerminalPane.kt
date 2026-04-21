package com.emergent.codeide.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergent.codeide.ui.theme.*
import com.emergent.codeide.util.ShellSession
import java.io.File

@Composable
fun TerminalPane(
    workingDir: File,
    onClose: () -> Unit,
    heightFraction: Float = 0.4f,
    pendingCommand: String? = null,
    onPendingConsumed: () -> Unit = {},
) {
    val session = remember(workingDir) { ShellSession(workingDir) }
    var output by remember { mutableStateOf("") }
    var cmd by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    DisposableEffect(session) {
        session.start()
        onDispose { session.stop() }
    }
    LaunchedEffect(session) {
        session.output.collect { chunk ->
            output += chunk
            if (output.length > 20000) output = output.takeLast(20000)
            scroll.animateScrollTo(scroll.maxValue)
        }
    }
    LaunchedEffect(pendingCommand) {
        val c = pendingCommand
        if (!c.isNullOrBlank()) {
            // small delay to ensure shell started
            kotlinx.coroutines.delay(150)
            session.send(c)
            onPendingConsumed()
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFraction)
            .background(VsBg)
    ) {
        // Terminal tab header (VS Code style)
        Row(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(VsPanel)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TERMINAL",
                color = VsText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { output = "" }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.DeleteOutline, "Clear",
                    tint = VsTextMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = {
                session.stop(); output = ""; session.start()
            }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.RestartAlt, "Restart",
                    tint = VsTextMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Close, "Close",
                    tint = VsTextMuted, modifier = Modifier.size(16.dp))
            }
        }
        Divider(color = VsBorder)

        // Output
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                output,
                color = VsText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Prompt + input
        Row(
            Modifier
                .fillMaxWidth()
                .background(VsPanel)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\$ ",
                color = VsAccent,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold)
            BasicTextField(
                value = cmd,
                onValueChange = { cmd = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = VsText, fontSize = 13.sp, fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(VsAccent),
                singleLine = true,
            )
            TextButton(onClick = {
                if (cmd.isNotBlank()) {
                    session.send(cmd)
                    cmd = ""
                }
            }) { Text("RUN", color = VsAccent, fontSize = 12.sp) }
        }
    }
}
