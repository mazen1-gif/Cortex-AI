package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CyberRoutine
import com.example.model.ParsedAction
import com.example.service.FloatingOrbService
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.HologramText
import com.example.ui.theme.HologramTextMuted
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.viewmodel.MainViewModel

@Composable
fun RoutinesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArabic by viewModel.isArabicVoice.collectAsState()
    val isOrbActive by viewModel.isFloatingOrbActive.collectAsState()

    var overlayGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            RoutinesHeader(isArabic = isArabic)
        }

        // Floating Orb Toggle Card
        item {
            FloatingOrbControllerCard(
                isArabic = isArabic,
                isOrbActive = isOrbActive && FloatingOrbService.isServiceActive,
                overlayGranted = overlayGranted,
                onToggleOrb = { enable ->
                    if (enable) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            FloatingOrbService.start(context)
                            viewModel.setFloatingOrbActive(true)
                        }
                    } else {
                        FloatingOrbService.stop(context)
                        viewModel.setFloatingOrbActive(false)
                    }
                },
                onRequestOverlayPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }
            )
        }

        // Routines Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 18.dp)
                        .background(NeonMagenta, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "الإجراءات اليومية الجاهزة" else "BUILT-IN DAILY ROUTINES",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // List of routines
        items(viewModel.cyberRoutines, key = { it.id }) { routine ->
            RoutineCard(
                routine = routine,
                isArabic = isArabic,
                onExecute = {
                    viewModel.processVoiceCommand(routine.triggerPhrase)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun RoutinesHeader(isArabic: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = if (isArabic) "الأوضاع والمساعد العائم" else "ROUTINES & COMPANION",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = if (isArabic) "تحدث معي ونفذ روتينك بكلمة واحدة، أو نادني فوق أي تطبيق"
                    else "Run actions with one word or call me over any app",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 13.sp,
                    color = HologramTextMuted
                )
            }
        }
    }
}

@Composable
fun FloatingOrbControllerCard(
    isArabic: Boolean,
    isOrbActive: Boolean,
    overlayGranted: Boolean,
    onToggleOrb: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        CyberSurfaceVariant,
                        CyberSurface
                    )
                )
            )
            .border(1.dp, if (isOrbActive) NeonCyan else CyberSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isOrbActive) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (isOrbActive) NeonCyan else HologramTextMuted, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Orb",
                            tint = if (isOrbActive) NeonCyan else HologramTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isArabic) "المساعد العائم فوق التطبيقات" else "OVERLAY ASSISTANT",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isOrbActive)
                                (if (isArabic) "جاهز للاستماع إليك الآن" else "Ready to listen")
                            else
                                (if (isArabic) "متوقف حالياً" else "Disabled"),
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 12.sp,
                            color = if (isOrbActive) MatrixGreen else HologramTextMuted
                        )
                    }
                }

                Switch(
                    checked = isOrbActive,
                    onCheckedChange = { onToggleOrb(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberDarkBg,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = HologramTextMuted,
                        uncheckedTrackColor = CyberDarkBg
                    ),
                    modifier = Modifier.testTag("floating_orb_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isArabic)
                    "أكون بجانبك في أي تطبيق تستخدمه، اضغط على الزر وتحدث معي فوراً وسأنفذ طلبك دون أن تخرج من شاشتك."
                else
                    "Speak with me anytime from any app. Tap and I will handle your requests instantly without leaving your screen.",
                fontFamily = RajdhaniFontFamily,
                fontSize = 13.sp,
                color = HologramText,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Ways to Access Anywhere Info Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberDarkBg)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isArabic) "تحدث من أي شاشة" else "Speak Anywhere",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberDarkBg)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isArabic) "اختصار الستارة العلوية" else "Quick Settings Tile",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 11.sp,
                        color = MatrixGreen
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberDarkBg)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isArabic) "تنفيذ فوري وبسيط" else "Instant Execution",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 11.sp,
                        color = NeonYellow
                    )
                }
            }

            if (!overlayGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestOverlayPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonYellow.copy(alpha = 0.15f),
                        contentColor = NeonYellow
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("grant_overlay_permission_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Permission",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "منح إذن الظهور فوق التطبيقات" else "Grant Display Over Apps Permission",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RoutineCard(
    routine: CyberRoutine,
    isArabic: Boolean,
    onExecute: () -> Unit
) {
    val routineIcon: ImageVector = when (routine.iconName) {
        "car" -> Icons.Default.DirectionsCar
        "bedtime" -> Icons.Default.Bedtime
        "work" -> Icons.Default.Work
        "morning" -> Icons.Default.WbSunny
        "emergency" -> Icons.Default.Warning
        "boost" -> Icons.Default.Speed
        "battery_saver" -> Icons.Default.ElectricBolt
        "security" -> Icons.Default.Security
        else -> Icons.Default.ElectricBolt
    }

    val accentColor: Color = when (routine.iconName) {
        "car" -> NeonCyan
        "bedtime" -> Color(0xFFB388FF)
        "work" -> Color(0xFFFFB74D)
        "morning" -> NeonYellow
        "emergency" -> Color(0xFFFF1744)
        "boost" -> MatrixGreen
        "battery_saver" -> NeonYellow
        "security" -> NeonMagenta
        else -> NeonMagenta
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurface)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("routine_card_${routine.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = routineIcon,
                            contentDescription = routine.titleAr,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isArabic) routine.titleAr else routine.titleEn,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isArabic) routine.descriptionAr else routine.descriptionEn,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 11.sp,
                            color = HologramTextMuted
                        )
                    }
                }

                // Run Button
                Button(
                    onClick = onExecute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("run_routine_${routine.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "تشغيل" else "RUN",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trigger Voice Phrase Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberDarkBg)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "قل للمساعد: \"${routine.triggerPhrase}\"" else "Say: \"${routine.triggerPhrase}\"",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chained Action Steps
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                routine.stepsAr.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Step",
                            tint = accentColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = step,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 11.sp,
                            color = HologramText
                        )
                    }
                }
            }
        }
    }
}
