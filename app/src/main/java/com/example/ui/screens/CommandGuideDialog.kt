package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CommandPrompt
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.HologramTextMuted
import com.example.ui.theme.HologramTextPrimary
import com.example.ui.theme.HologramTextSecondary
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun CommandGuideDialog(
    prompts: List<CommandPrompt>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSelectCommand: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDarkBg)
                .border(1.dp, NeonCyan, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "أوامر صوتية",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "أمثلة على أوامر يمكنك قولها للمساعد" else "COMMAND SUGGESTIONS",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = HologramTextMuted
                        )
                    }
                }

                Text(
                    text = if (isArabic) "تحدث بأي جملة طبيعية بصوتك أو اضغط 'تجربة' للتنفيذ المباشر:" else "Speak naturally or tap 'Try' to test immediately:",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = HologramTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(prompts) { prompt ->
                        val commandText = if (isArabic) prompt.commandAr else prompt.commandEn
                        val category = if (isArabic) prompt.categoryAr else prompt.categoryEn
                        val desc = if (isArabic) prompt.descriptionAr else prompt.descriptionEn

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberSurface)
                                .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectCommand(commandText)
                                    onDismiss()
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyberSurfaceVariant)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = category,
                                            fontFamily = OrbitronFontFamily,
                                            fontSize = 9.sp,
                                            color = NeonMagenta
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = commandText,
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 14.sp,
                                        color = HologramTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    onSelectCommand(commandText)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "تجربة الأمر",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isArabic) "تجربة" else "TRY",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 10.sp,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
