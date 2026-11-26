package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.service.FloatingOrbService
import com.example.ui.components.CyberHudBar
import com.example.ui.components.TerminalConsole
import com.example.ui.components.VisualizerOrb
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

@Composable
fun MainAssistantScreen(
    viewModel: MainViewModel,
    onOpenCommandGuide: () -> Unit,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onTriggerVoiceDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.assistantState.collectAsState()
    val audioRms by viewModel.audioRmsLevel.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val logs by viewModel.terminalLogs.collectAsState()
    val transcript by viewModel.lastSpokenTranscript.collectAsState()
    val response by viewModel.lastVoiceResponse.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val isArabic by viewModel.isArabicVoice.collectAsState()
    val isFloatingMicActive by viewModel.isFloatingOrbActive.collectAsState()
    val voiceNotes by viewModel.voiceNotes.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val showIntrudersDialog by viewModel.showIntrudersDialog.collectAsState()
    val hasNewIntruderAlert by viewModel.hasNewIntruderAlert.collectAsState()
    val intruderRecords by viewModel.intruderRecords.collectAsState()

    var manualTextInput by remember { mutableStateOf("") }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }

    val todayExpenseTotal = remember(expenses) { viewModel.expenseManager.getTodayTotal() }
    val remainingBudget = remember(expenses) { viewModel.expenseManager.getRemainingBudget() }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
            FloatingOrbService.start(context)
            viewModel.setFloatingOrbActive(true)
        } else {
            viewModel.log(
                com.example.model.LogLevel.WARN,
                "رفيق الشاشة",
                "لم يتم منح إذن الظهور فوق التطبيقات."
            )
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(context)
            ) {
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } else {
                FloatingOrbService.start(context)
                viewModel.setFloatingOrbActive(true)
            }
        } else {
            viewModel.log(
                com.example.model.LogLevel.WARN,
                "الميكروفون",
                "يلزم إذن الميكروفون لتفعيل رفيق الشاشة الصوتي."
            )
        }
    }

    val toggleFloatingMic = {
        if (isFloatingMicActive || FloatingOrbService.isServiceActive) {
            FloatingOrbService.stop(context)
            viewModel.setFloatingOrbActive(false)
        } else if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(context)
            ) {
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } else {
                FloatingOrbService.start(context)
                viewModel.setFloatingOrbActive(true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF1C1814), CyberDarkBg, CyberDarkBg)
                )
            )
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Cyber HUD Bar
        CyberHudBar(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            isArabic = isArabic,
            onToggleLanguage = { viewModel.toggleVoiceLanguage() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // State Title Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceBorder.copy(alpha = 0.5f))
                .border(1.dp, if (isListening) NeonMagenta else NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isArabic) state.labelAr else state.labelEn,
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                color = if (isListening) NeonMagenta else NeonCyan
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Animated Visualizer Orb (Hold down to speak, release to execute!)
        VisualizerOrb(
            state = state,
            audioRms = audioRms,
            isListening = isListening,
            onPressStart = onPressStart,
            onPressEnd = onPressEnd,
            onClick = onTriggerVoiceDialog
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Big, clear Push-to-Talk button: Hold to talk, release to execute!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isListening) NeonMagenta else NeonCyan.copy(alpha = 0.12f))
                .border(1.dp, if (isListening) NeonMagenta else NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        }
                    )
                }
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("hold_to_talk_action_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = "Hold to talk",
                    tint = if (isListening) Color.White else NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isListening) {
                        if (isArabic) "استمر بالضغط وتحدث... (ارفع إصبعك للتنفيذ فوراً!)"
                        else "Hold down & speak... (Release to execute!)"
                    } else {
                        if (isArabic) "استمر بالضغط وتحدث (ارفع إصبعك للتنفيذ)"
                        else "Hold to Speak (Release to execute)"
                    },
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    color = if (isListening) Color.White else NeonCyan,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = toggleFloatingMic)
                .testTag("floating_mic_toggle"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStroke(
                1.dp,
                if (isFloatingMicActive) MatrixGreen.copy(alpha = 0.75f)
                else NeonCyan.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFloatingMicActive) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = null,
                    tint = if (isFloatingMicActive) MatrixGreen else NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFloatingMicActive) {
                            if (isArabic) "الميكروفون العائم مفعّل" else "Floating microphone enabled"
                        } else {
                            if (isArabic) "تفعيل الميكروفون العائم" else "Enable floating microphone"
                        },
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        color = if (isFloatingMicActive) MatrixGreen else NeonCyan
                    )
                    Text(
                        text = if (isArabic) {
                            "يبقى فوق التطبيقات؛ اضغطه للتحدث، واضغطه مجددًا للإيقاف."
                        } else {
                            "Stays above other apps. Tap to speak and tap again to stop."
                        },
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 12.sp,
                        color = HologramTextMuted
                    )
                }
                Text(
                    text = if (isFloatingMicActive) {
                        if (isArabic) "إيقاف" else "OFF"
                    } else {
                        if (isArabic) "تفعيل" else "ON"
                    },
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = if (isFloatingMicActive) MatrixGreen else NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active listening notification banner with live instructions
        AnimatedVisibility(
            visible = isListening,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonMagenta.copy(alpha = 0.15f))
                    .border(1.dp, NeonMagenta, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "أنا أستمع إليك الآن!" else "I am listening now!",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp,
                            color = NeonMagenta
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isArabic) "تحدث بصوت مسموع، مثلاً: «احذف واتساب» أو «افتح يوتيوب»" else "Speak clearly, e.g. 'Uninstall WhatsApp' or 'Open YouTube'",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 13.sp,
                            color = HologramTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurface)
                            .clickable { viewModel.onVoiceInputCancelled() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 11.sp,
                            color = HologramTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // User Transcript (if any)
        AnimatedVisibility(
            visible = transcript.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceVariant.copy(alpha = 0.7f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = if (isArabic) "ما قلته أنت:" else "WHAT YOU SAID:",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"$transcript\"",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 14.sp,
                        color = HologramTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Intruder Alert Banner (عند رصد متلصص حاول لمس الهاتف أو إدخال رمز خاطئ)
        AnimatedVisibility(
            visible = hasNewIntruderAlert,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openIntrudersDialog() }
                    .testTag("intruder_alert_banner"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NeonMagenta.copy(alpha = 0.18f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NeonMagenta))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = NeonMagenta,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنبيه أمني: تم التقاط صورة متلصص!",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "حاول شخص العبث بهاتفك، اضغط لعرض الصورة وسجل الحماية",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.closeIntrudersDialog() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Assistant AI Voice Response Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (state == AssistantState.SPEAKING) MatrixGreen else NeonCyan)
                        .padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "رد المساعد:" else "ASSISTANT RESPONSE:",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = response,
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 14.sp,
                        color = HologramTextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Smart Wallet & Expense Tracker Card (محفظتي ومسجل المصاريف الفوري)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, MatrixGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .clickable { showWalletDialog = true }
                .padding(12.dp)
                .testTag("wallet_expenses_card")
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MatrixGreen.copy(alpha = 0.15f))
                                .border(1.dp, MatrixGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "المحفظة والمصاريف",
                                tint = MatrixGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isArabic) "محفظتي ومسجل المصاريف" else "WALLET & EXPENSES",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = MatrixGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MatrixGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${todayExpenseTotal.toLong()} ريال اليوم",
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MatrixGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic) "المتبقي من ميزانيتك: ${remainingBudget.toLong()} ريال • انقر لفتح السجل الكامل"
                                else "Remaining budget: ${remainingBudget.toLong()} SAR • Tap to view",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.processVoiceCommand("كم صرفت اليوم")
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MatrixGreen.copy(alpha = 0.15f))
                            .border(1.dp, MatrixGreen.copy(alpha = 0.4f), CircleShape)
                            .testTag("read_expenses_quick_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "قراءة مصاريف اليوم",
                            tint = MatrixGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Voice Memos & Action Tasks Card (مذكرات الأفكار والمهام الفورية بالصوت)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, NeonYellow.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable { showNotesDialog = true }
                .padding(12.dp)
                .testTag("voice_memos_card")
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonYellow.copy(alpha = 0.15f))
                                .border(1.dp, NeonYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Note,
                                contentDescription = "Voice Notes",
                                tint = NeonYellow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isArabic) "مذكرات الأفكار والمهام الفورية" else "VOICE MEMOS & TASKS",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = NeonYellow
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonYellow.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${voiceNotes.size}",
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 11.sp,
                                        color = NeonYellow
                                    )
                                }
                            }
                            Text(
                                text = if (voiceNotes.isNotEmpty()) {
                                    "آخر تدوينة: ${voiceNotes.first().text}"
                                } else {
                                    if (isArabic) "قل: «سجل ملاحظة كذا» أو «احفظ فكرة كذا»" else "Say: 'Save note ...' or 'Save idea ...'"
                                },
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.readNotesAloud() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape)
                            .testTag("read_notes_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "قراءة الملاحظات بصوت المساعد",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Command Prompts Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "أوامر سريعة للتجربة:" else "QUICK SUGGESTIONS:",
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                color = HologramTextSecondary
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenCommandGuide)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Command Guide",
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isArabic) "أفكار أوامر أخرى (+20)" else "MORE COMMANDS",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.commandPrompts.take(7)) { prompt ->
                val label = if (isArabic) prompt.commandAr else prompt.commandEn
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceBorder.copy(alpha = 0.5f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.processVoiceCommand(label)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 12.sp,
                        color = HologramTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chained Smart Macros Quick Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "سيناريوهات سريعة بكلمة واحدة:" else "QUICK ACTIONS:",
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                color = NeonMagenta
            )

            Text(
                text = if (isArabic) "انقر للتشغيل" else "Tap to trigger",
                fontFamily = RajdhaniFontFamily,
                fontSize = 11.sp,
                color = HologramTextMuted
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                RoutineChip(
                    emoji = "🚗",
                    title = if (isArabic) "أنا في السيارة" else "Drive Mode",
                    accent = NeonCyan,
                    onClick = { viewModel.processVoiceCommand("أنا في السيارة") }
                )
            }
            item {
                RoutineChip(
                    emoji = "🌙",
                    title = if (isArabic) "وقت النوم" else "Bedtime",
                    accent = Color(0xFFB388FF),
                    onClick = { viewModel.processVoiceCommand("وقت النوم") }
                )
            }
            item {
                RoutineChip(
                    emoji = "💼",
                    title = if (isArabic) "أنا في العمل" else "Work Focus",
                    accent = Color(0xFFFFB74D),
                    onClick = { viewModel.processVoiceCommand("أنا في العمل") }
                )
            }
            item {
                RoutineChip(
                    emoji = "⚡",
                    title = if (isArabic) "وضع الطوارئ" else "Emergency",
                    accent = Color(0xFFFF1744),
                    onClick = { viewModel.processVoiceCommand("وضع الطوارئ") }
                )
            }
            item {
                RoutineChip(
                    emoji = "🧹",
                    title = if (isArabic) "إغلاق تطبيقات الخلفية" else "Close Background",
                    accent = MatrixGreen,
                    onClick = { viewModel.closeBackgroundAppsDirect() }
                )
            }
            item {
                RoutineChip(
                    emoji = "📸",
                    title = if (isArabic) "صور المتلصصين" else "Intruder Photos",
                    accent = NeonMagenta,
                    onClick = { viewModel.openIntrudersDialog() }
                )
            }
            item {
                RoutineChip(
                    emoji = "💧",
                    title = if (isArabic) "طرد الماء والغبار" else "Water Eject",
                    accent = NeonCyan,
                    onClick = { viewModel.processVoiceCommand("طرد الماء من السماعة") }
                )
            }
            item {
                RoutineChip(
                    emoji = "🛡️",
                    title = if (isArabic) "فحص أمان التطبيقات" else "Audit Security",
                    accent = MatrixGreen,
                    onClick = { viewModel.processVoiceCommand("افحص أمان التطبيقات") }
                )
            }
            item {
                RoutineChip(
                    emoji = "📄",
                    title = if (isArabic) "قراءة وتلخيص PDF" else "Read PDF",
                    accent = Color(0xFFFFB74D),
                    onClick = { viewModel.processVoiceCommand("افتح ملف الـ PDF واقرأ لي ملخصه") }
                )
            }
            item {
                RoutineChip(
                    emoji = "📷",
                    title = if (isArabic) "التقاط صورة فورية" else "Snap Photo",
                    accent = NeonYellow,
                    onClick = { viewModel.processVoiceCommand("التقط لي صورة بالكاميرا") }
                )
            }
            item {
                RoutineChip(
                    emoji = "🎙️",
                    title = if (isArabic) "رفيق الشاشة" else "Screen Companion",
                    accent = NeonYellow,
                    onClick = { viewModel.setNavTab(1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual Text Command Input (For Silent / Typing mode)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualTextInput,
                onValueChange = { manualTextInput = it },
                placeholder = {
                    Text(
                        text = if (isArabic) "اكتب طلبك هنا (مثال: احذف واتساب، افتح يوتيوب)..." else "Type your request (e.g. Open YouTube)...",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 12.sp,
                        color = HologramTextMuted
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_text_input"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 14.sp,
                    color = HologramTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberSurfaceBorder,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (manualTextInput.isNotBlank()) {
                        viewModel.processVoiceCommand(manualTextInput)
                        manualTextInput = ""
                        focusManager.clearFocus()
                    }
                })
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (manualTextInput.isNotBlank()) {
                        viewModel.processVoiceCommand(manualTextInput)
                        manualTextInput = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonCyan)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Command",
                    tint = CyberDarkBg,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Fast Mic Button with Hold-to-Talk and Tap support
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isListening) NeonMagenta else CyberSurfaceVariant)
                    .border(1.dp, if (isListening) NeonMagenta else NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onPressStart()
                                tryAwaitRelease()
                                onPressEnd()
                            }
                        )
                    }
                    .testTag("mic_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = "Toggle Mic",
                    tint = if (isListening) Color.White else NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System Terminal Console
        TerminalConsole(
            logs = logs,
            onClearLogs = { viewModel.clearLogs() }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showNotesDialog) {
        VoiceNotesDialog(
            viewModel = viewModel,
            onDismiss = { showNotesDialog = false }
        )
    }

    if (showWalletDialog) {
        WalletExpensesDialog(
            viewModel = viewModel,
            onDismiss = { showWalletDialog = false }
        )
    }

    if (showIntrudersDialog) {
        IntruderRecordsDialog(
            records = intruderRecords,
            onDismiss = { viewModel.closeIntrudersDialog() },
            onDeleteRecord = { id -> viewModel.deleteIntruderRecord(id) },
            onClearAll = { viewModel.clearAllIntruderRecords() }
        )
    }
}

@Composable
fun RoutineChip(
    emoji: String,
    title: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}
