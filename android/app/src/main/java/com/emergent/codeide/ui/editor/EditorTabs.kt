package com.emergent.codeide.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergent.codeide.data.OpenFile
import com.emergent.codeide.ui.theme.*

@Composable
fun EditorTabs(
    tabs: List<OpenFile>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
) {
    if (tabs.isEmpty()) return
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(VsPanel)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, of ->
                val active = i == activeIndex
                Row(
                    Modifier
                        .background(if (active) VsTabActive else VsTabInactive)
                        .clickable { onSelect(i) }
                        .padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        of.file.name,
                        color = if (active) VsText else VsTextMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { onClose(i) }, modifier = Modifier.size(22.dp)) {
                        if (of.dirty) {
                            Icon(
                                Icons.Outlined.FiberManualRecord,
                                "Unsaved",
                                tint = VsText,
                                modifier = Modifier.size(10.dp)
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Close,
                                "Close",
                                tint = VsTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(
                    Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(VsBorder)
                )
            }
        }
        Divider(color = VsBorder)
    }
}
