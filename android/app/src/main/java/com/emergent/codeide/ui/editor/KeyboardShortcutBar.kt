package com.emergent.codeide.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.emergent.codeide.ui.theme.*

/** Scrollable bar above the keyboard with developer-friendly keys — Tab, Esc, arrows, brackets. */
@Composable
fun KeyboardShortcutBar(
    onInsert: (String) -> Unit,
    onTab: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    val keys = listOf(
        "ESC" to { onInsert("\u001B") },
        "TAB" to { onTab() },
        "{" to { onInsert("{") },
        "}" to { onInsert("}") },
        "(" to { onInsert("(") },
        ")" to { onInsert(")") },
        "[" to { onInsert("[") },
        "]" to { onInsert("]") },
        "<" to { onInsert("<") },
        ">" to { onInsert(">") },
        ";" to { onInsert(";") },
        "\"" to { onInsert("\"") },
        "'" to { onInsert("'") },
        "/" to { onInsert("/") },
        "\\" to { onInsert("\\") },
        "|" to { onInsert("|") },
        "&" to { onInsert("&") },
        "*" to { onInsert("*") },
        "#" to { onInsert("#") },
        "->" to { onInsert("->") },
        "UNDO" to { onUndo() },
        "REDO" to { onRedo() },
    )
    Surface(color = VsPanel) {
        Column {
            Divider(color = VsBorder)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                keys.forEach { (label, action) ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(VsTabInactive)
                            .clickable { action() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            label,
                            color = VsText,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
