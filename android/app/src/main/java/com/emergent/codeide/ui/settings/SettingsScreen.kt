package com.emergent.codeide.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergent.codeide.data.AppSettings
import com.emergent.codeide.ui.theme.*
import com.emergent.codeide.util.ToolchainResolver

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onShowKeyboardChange: (Boolean) -> Unit,
    onSyntaxChange: (Boolean) -> Unit,
    onWrapChange: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onTabSizeChange: (Int) -> Unit,
    termuxInstalled: Boolean,
    onInstallTermux: () -> Unit,
    toolchainStatus: Map<String, String?>,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(VsBg)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = VsText)
            }
            Text("Settings",
                color = VsText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold)
        }
        Divider(color = VsBorder)

        SectionHeader("Editor")
        ToggleRow(
            title = "Show Android soft keyboard",
            subtitle = "Turn off to edit using the keyboard bar only (great for external keyboards).",
            checked = settings.showSoftKeyboard,
            onCheck = onShowKeyboardChange,
        )
        ToggleRow(
            title = "Syntax highlighting",
            subtitle = "Color-code C/C++, Python, JS, Kotlin.",
            checked = settings.syntaxHighlighting,
            onCheck = onSyntaxChange,
        )
        ToggleRow(
            title = "Word wrap",
            subtitle = "Wrap long lines.",
            checked = settings.wordWrap,
            onCheck = onWrapChange,
        )
        SliderRow(
            title = "Font size",
            value = settings.fontSize, min = 10f, max = 22f, step = 1f,
            display = "${settings.fontSize.toInt()}sp",
            onValue = onFontSizeChange,
        )
        SliderRow(
            title = "Tab size",
            value = settings.tabSize.toFloat(), min = 2f, max = 8f, step = 1f,
            display = settings.tabSize.toString(),
            onValue = { onTabSizeChange(it.toInt()) },
        )

        SectionHeader("Toolchains")
        InfoRow(
            title = "Termux",
            detail = if (termuxInstalled) "Installed" else "Not found",
            accent = termuxInstalled,
            action = if (!termuxInstalled) "Install" else null,
            onAction = onInstallTermux,
        )
        toolchainStatus.forEach { (tool, path) ->
            InfoRow(
                title = tool,
                detail = path ?: "Not found",
                accent = path != null,
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "Tip: Install Termux (F-Droid), then inside Termux run:\n" +
                        "  pkg update && pkg install clang python cmake make git",
                color = VsTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = VsTextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .background(VsPanel)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String, subtitle: String? = null,
    checked: Boolean, onCheck: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = VsText, fontSize = 14.sp)
            subtitle?.let {
                Text(it, color = VsTextMuted, fontSize = 11.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheck,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VsAccent,
                checkedTrackColor = VsAccent.copy(alpha = 0.4f)
            ))
    }
    Divider(color = VsBorder.copy(alpha = 0.4f))
}

@Composable
private fun SliderRow(
    title: String, value: Float, min: Float, max: Float, step: Float,
    display: String, onValue: (Float) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = VsText, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(display, color = VsTextMuted, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = min..max,
            steps = ((max - min) / step).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = VsAccent, activeTrackColor = VsAccent
            )
        )
    }
    Divider(color = VsBorder.copy(alpha = 0.4f))
}

@Composable
private fun InfoRow(
    title: String, detail: String, accent: Boolean,
    action: String? = null, onAction: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = VsText, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Text(detail,
                color = if (accent) VsAccent else VsTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace)
        }
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, color = VsAccent) }
        }
    }
    Divider(color = VsBorder.copy(alpha = 0.4f))
}
