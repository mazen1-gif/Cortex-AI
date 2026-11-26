package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogLevel
import com.example.model.TerminalLog
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.HologramTextMuted
import com.example.ui.theme.HologramTextPrimary
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDim
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun TerminalConsole(
    logs: List<TerminalLog>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberDarkBg.copy(alpha = 0.92f))
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("terminal_console_panel")
    ) {
        // Activity Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "سجل الأنشطة",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
<<<<<<< HEAD
                    text = "آخر ما حدث",
=======
                    text = "سجل النشاط والمتابعة",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    color = NeonCyan
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MatrixGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "نشط",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 10.sp,
                    color = MatrixGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "مسح السجل",
                        tint = HologramTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CyberSurfaceBorder)
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Logs Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 140.dp)
        ) {
            if (logs.isEmpty()) {
                item {
                    Text(
<<<<<<< HEAD
                        text = "لا شيء بعد. اضغط زر التحدث وقل ما تريد.",
=======
                        text = "لا توجد أنشطة سابقة، اضغط على زر التحدث وأخبرني بما تحتاجه...",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 13.sp,
                        color = HologramTextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    TerminalLogRow(log)
                }
            }
        }
    }
}

@Composable
fun TerminalLogRow(log: TerminalLog) {
    val tagColor = when (log.level) {
        LogLevel.INFO -> NeonCyanDim
        LogLevel.INTENT -> NeonCyan
        LogLevel.EXEC -> NeonMagenta
        LogLevel.SUCCESS -> MatrixGreen
        LogLevel.WARN -> Color(0xFFFFB300)
        LogLevel.ERROR -> Color(0xFFFF2222)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${log.timestamp} • ",
            fontFamily = RajdhaniFontFamily,
            fontSize = 11.sp,
            color = HologramTextMuted
        )
        Text(
            text = "${log.tag}: ",
            fontFamily = RajdhaniFontFamily,
            fontSize = 12.sp,
            color = tagColor
        )
        Text(
            text = log.message,
            fontFamily = RajdhaniFontFamily,
            fontSize = 13.sp,
            color = if (log.level == LogLevel.SUCCESS) MatrixGreen else HologramTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
