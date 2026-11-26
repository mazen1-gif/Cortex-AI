package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.NoteType
import com.example.model.VoiceNote
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
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.viewmodel.MainViewModel

/**
 * نافذة مذكرات الأفكار والمهام الفورية بالصوت (Voice Memos & Action Tasks)
 * تنظيم الملاحظات مع التاريخ والوقت، وقراءتها كاملة بصوت المساعد.
 */
@Composable
fun VoiceNotesDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.voiceNotes.collectAsState()
    val isArabic by viewModel.isArabicVoice.collectAsState()

    var selectedFilter by remember { mutableStateOf<NoteType?>(null) }
    var showOnlyToday by remember { mutableStateOf(false) }
    var isAddingNote by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    var newNoteType by remember { mutableStateOf(NoteType.NOTE) }

    val todayStr = remember {
        java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    val filteredNotes = notes.filter { note ->
        val matchesType = selectedFilter == null || note.type == selectedFilter
        val matchesToday = !showOnlyToday || note.formattedDate == todayStr
        matchesType && matchesToday
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(18.dp))
                .background(CyberDarkBg)
                .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Note,
                                contentDescription = "Notes",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "مذكرات الأفكار والمهام الصوتية" else "VOICE MEMOS & TASKS",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 14.sp,
                                color = NeonCyan
                            )
                            Text(
                                text = if (isArabic) "حفظ فوري بالصوت مع التاريخ والوقت وقراءة آلية" else "Instant voice notes organized with date & time",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyberSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = HologramTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fast Action Buttons: Read Aloud & Add Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.readNotesAloud(showOnlyToday) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .testTag("read_notes_aloud_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "قراءة الملاحظات",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
<<<<<<< HEAD
                                text = if (isArabic) "قراءة الملاحظات بصوتك" else "Read Out Loud",
=======
                                text = if (isArabic) "🔊 قراءة الملاحظات بصوتك" else "🔊 Read Out Loud",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    Button(
                        onClick = { isAddingNote = !isAddingNote },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .border(1.dp, MatrixGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .testTag("toggle_add_note_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إضافة ملاحظة",
                                tint = MatrixGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
<<<<<<< HEAD
                                text = if (isArabic) "إضافة تدوينة" else "New Note",
=======
                                text = if (isArabic) "➕ إضافة تدوينة" else "➕ New Note",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = MatrixGreen
                            )
                        }
                    }
                }

                // Add Note Panel
                AnimatedVisibility(visible = isAddingNote, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurface)
                            .border(1.dp, MatrixGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) "اكتب أو الصق الملاحظة/الفكرة:" else "Enter note or idea:",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            color = HologramTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            placeholder = { Text("مثال: شراء بطارية جديدة، أو موعد الاجتماع غداً...", color = HologramTextMuted, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MatrixGreen,
                                unfocusedBorderColor = CyberSurfaceBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("new_note_text_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
<<<<<<< HEAD
                                Triple(NoteType.NOTE, "ملاحظة", NeonCyan),
                                Triple(NoteType.IDEA, "فكرة", NeonYellow),
                                Triple(NoteType.TASK, "مهمة", NeonMagenta)
=======
                                Triple(NoteType.NOTE, "📝 ملاحظة", NeonCyan),
                                Triple(NoteType.IDEA, "💡 فكرة", NeonYellow),
                                Triple(NoteType.TASK, "⚡ مهمة", NeonMagenta)
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                            ).forEach { (type, label, color) ->
                                val isSelected = newNoteType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.25f) else CyberSurfaceVariant)
                                        .border(1.dp, if (isSelected) color else CyberSurfaceBorder, RoundedCornerShape(8.dp))
                                        .clickable { newNoteType = type }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 11.sp,
                                        color = if (isSelected) color else HologramTextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (newNoteText.isNotBlank()) {
                                    viewModel.saveVoiceNote(newNoteText.trim(), newNoteType)
                                    newNoteText = ""
                                    isAddingNote = false
                                }
                            },
                            enabled = newNoteText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("save_note_submit_btn")
                        ) {
                            Text(
                                text = if (isArabic) "حفظ في المفكرة" else "Save Note",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                color = CyberDarkBg
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filters Row: All, Notes, Ideas, Tasks, Today
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        title = "الكل (${notes.size})",
                        isSelected = selectedFilter == null && !showOnlyToday,
                        onClick = {
                            selectedFilter = null
                            showOnlyToday = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
<<<<<<< HEAD
                        title = "ملاحظات",
=======
                        title = "ملاحظات 📝",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                        isSelected = selectedFilter == NoteType.NOTE && !showOnlyToday,
                        onClick = {
                            selectedFilter = NoteType.NOTE
                            showOnlyToday = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
<<<<<<< HEAD
                        title = "أفكار",
=======
                        title = "أفكار 💡",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                        isSelected = selectedFilter == NoteType.IDEA && !showOnlyToday,
                        onClick = {
                            selectedFilter = NoteType.IDEA
                            showOnlyToday = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
<<<<<<< HEAD
                        title = "مهام",
=======
                        title = "مهام ⚡",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                        isSelected = selectedFilter == NoteType.TASK && !showOnlyToday,
                        onClick = {
                            selectedFilter = NoteType.TASK
                            showOnlyToday = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
<<<<<<< HEAD
                        title = "اليوم",
=======
                        title = "اليوم 📅",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                        isSelected = showOnlyToday,
                        onClick = { showOnlyToday = !showOnlyToday },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes List
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
<<<<<<< HEAD
                                text = "لا توجد ملاحظات في هذا التصنيف",
=======
                                text = "📝 لا توجد ملاحظات في هذا التصنيف",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                fontFamily = OrbitronFontFamily,
                                fontSize = 13.sp,
                                color = HologramTextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "قل للمساعد أو لرفيق الشاشة: «سجل ملاحظة كذا» أو «احفظ فكرة كذا»",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                isArabic = isArabic,
                                onReadAloud = {
                                    val speech = "${note.type.labelAr}: ${note.text}"
                                    viewModel.log(com.example.model.LogLevel.INFO, "قراءة ملاحظة", speech)
                                    viewModel.processVoiceCommand("اقرأ: $speech")
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("Voice Note", note.text)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ الملاحظة إلى الحافظة", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { viewModel.deleteVoiceNote(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberSurfaceVariant)
            .border(1.dp, if (isSelected) NeonCyan else CyberSurfaceBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontFamily = RajdhaniFontFamily,
            fontSize = 11.sp,
            color = if (isSelected) NeonCyan else HologramTextMuted,
            maxLines = 1
        )
    }
}

@Composable
fun NoteCardItem(
    note: VoiceNote,
    isArabic: Boolean,
    onReadAloud: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = when (note.type) {
        NoteType.IDEA -> NeonYellow
        NoteType.TASK -> NeonMagenta
        NoteType.NOTE -> NeonCyan
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("note_card_${note.id}")
    ) {
        Column {
            // Header with Type & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${note.type.emoji} ${note.type.labelAr}",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 11.sp,
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            tint = HologramTextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${note.formattedDate} • ${note.formattedTime}",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 10.sp,
                            color = HologramTextMuted
                        )
                    }
                }

                // Actions: Read, Copy, Delete
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onReadAloud,
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(CyberSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "قراءة",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(CyberSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = HologramTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(CyberSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = NeonMagenta,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Note Content Text
            Text(
                text = note.text,
                fontFamily = RajdhaniFontFamily,
                fontSize = 14.sp,
                color = Color.White,
                lineHeight = 20.sp
            )
        }
    }
}
