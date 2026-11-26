package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SystemTelemetryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val batteryTemp by viewModel.batteryTemp.collectAsState()
    val memoryUsage by viewModel.memoryUsage.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightActive.collectAsState()
    val volumePercent by viewModel.volumePercent.collectAsState()
    val isArabic by viewModel.isArabicVoice.collectAsState()
    val isFullChargeAlertEnabled by viewModel.isFullChargeSentinelEnabled.collectAsState()
    val isLowBatteryAlertEnabled by viewModel.isLowBatterySentinelEnabled.collectAsState()
    val isMessageVoiceReadingEnabled by viewModel.isMessageVoiceReadingEnabled.collectAsState()
    val isAutoReplyDrivingEnabled by viewModel.isAutoReplyDrivingEnabled.collectAsState()
    val isAutoReplyWorkEnabled by viewModel.isAutoReplyWorkEnabled.collectAsState()
    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val showIntrudersDialog by viewModel.showIntrudersDialog.collectAsState()
    val intruderRecords by viewModel.intruderRecords.collectAsState()
    val isAntiTheftArmed by viewModel.isAntiTheftArmed.collectAsState()
    val isAntiTheftArmingCountdown by viewModel.isAntiTheftArmingCountdown.collectAsState()
    val antiTheftCountdownSeconds by viewModel.antiTheftCountdownSeconds.collectAsState()
    val antiTheftTriggerOnCharger by viewModel.antiTheftTriggerOnCharger.collectAsState()
    val antiTheftTriggerOnMotion by viewModel.antiTheftTriggerOnMotion.collectAsState()
    val securityPin by viewModel.antiTheftPin.collectAsState()
    val isAntiTheftLockOverlayEnabled by viewModel.isAntiTheftLockOverlayEnabled.collectAsState()
    var showPinChangeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkNotificationAccess()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(14.dp)
            .verticalScroll(scrollState)
    ) {
        // Title
        Text(
            text = if (isArabic) "التحكم السريع بالجهاز" else "QUICK DEVICE CONTROLS",
            fontFamily = OrbitronFontFamily,
            fontSize = 15.sp,
            color = NeonCyan
        )
        Text(
            text = if (isArabic) "تحكم فوري بالكشاف، الصوت، البطارية، والإعدادات" else "Direct controls for torch, volume, battery, and settings",
            fontFamily = RajdhaniFontFamily,
            fontSize = 12.sp,
            color = HologramTextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Hardware Controls Grid: Flashlight + Stealth Mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Flashlight Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(
                        1.dp,
                        if (isFlashlightOn) NeonMagenta else CyberSurfaceBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.toggleFlashlightDirect() }
                    .padding(14.dp)
                    .testTag("flashlight_toggle_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Torch",
                            tint = if (isFlashlightOn) NeonMagenta else NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isFlashlightOn) NeonMagenta else HologramTextMuted)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "كشاف الهاتف" else "FLASHLIGHT",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        color = HologramTextPrimary
                    )
                    Text(
                        text = if (isFlashlightOn) (if (isArabic) "شغال الآن" else "ON") else (if (isArabic) "مطفأ" else "OFF"),
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 11.sp,
                        color = if (isFlashlightOn) NeonMagenta else HologramTextMuted
                    )
                }
            }

            // Stealth Protocol Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.processVoiceCommand(if (isArabic) "تفعيل وضع صامت" else "Quiet mode")
                    }
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Stealth",
                            tint = MatrixGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MatrixGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "الوضع الصامت" else "SILENT MODE",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        color = HologramTextPrimary
                    )
                    Text(
                        text = if (isArabic) "كتم الأصوات والاهتزاز" else "VIBRATE & SILENT",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 11.sp,
                        color = HologramTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sound Output Telemetry Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "مستوى صوت الهاتف" else "PHONE MEDIA VOLUME",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                    Text(
                        text = "$volumePercent%",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.adjustVolumeDirect(0) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeMute,
                            contentDescription = "كتم الصوت",
                            tint = NeonMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.adjustVolumeDirect(-1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = "خفض الصوت",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Slider(
                            value = volumePercent.toFloat(),
                            onValueChange = {},
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = CyberSurfaceBorder
                            )
                        )
                    }

                    IconButton(
                        onClick = { viewModel.adjustVolumeDirect(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "رفع الصوت",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Battery Sentinel & Power Management Card (الحارس الأمني للبطارية)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurface)
                .border(
                    1.dp,
                    if (batteryLevel <= 15) NeonMagenta else if (isCharging) MatrixGreen else CyberSurfaceBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(16.dp)
                .testTag("battery_sentinel_card")
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (batteryLevel <= 15) NeonMagenta.copy(alpha = 0.2f) else MatrixGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (batteryLevel <= 15) Icons.Default.BatteryAlert else if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.Shield,
                                contentDescription = "Battery Sentinel",
                                tint = if (batteryLevel <= 15) NeonMagenta else MatrixGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "الحارس الأمني للبطارية (Battery Sentinel)" else "BATTERY SENTINEL GUARD",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 13.sp,
                                color = if (batteryLevel <= 15) NeonMagenta else MatrixGreen
                            )
                            Text(
                                text = if (isArabic) "حماية البطارية من التلف مع التنبيه الصوتي المباشر" else "Voice alerts for 100% full charge & 15% power saving",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Level & Status Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$batteryLevel%",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 26.sp,
                                color = if (batteryLevel <= 15) NeonMagenta else if (batteryLevel >= 95) MatrixGreen else NeonCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCharging) MatrixGreen.copy(alpha = 0.15f) else CyberSurfaceVariant)
                                    .border(1.dp, if (isCharging) MatrixGreen.copy(alpha = 0.5f) else CyberSurfaceBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
<<<<<<< HEAD
                                    text = if (isCharging) (if (isArabic) "جارِ الشحن" else "CHARGING") else (if (isArabic) "على البطارية" else "BATTERY"),
=======
                                    text = if (isCharging) (if (isArabic) "⚡ جارِ الشحن" else "⚡ CHARGING") else (if (isArabic) "🔋 على البطارية" else "🔋 BATTERY"),
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = if (isCharging) MatrixGreen else HologramTextMuted
                                )
                            }
                        }
                        Text(
                            text = "${if (isArabic) "حرارة الجهاز:" else "Device Temp:"} $batteryTemp",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 11.sp,
                            color = HologramTextMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isArabic) "الذاكرة المستخدمة" else "RAM USAGE",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                        Text(
                            text = memoryUsage,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 12.sp,
                            color = HologramTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Battery Gauge Progress Bar
                LinearProgressIndicator(
                    progress = { (batteryLevel.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (batteryLevel <= 15) NeonMagenta else if (isCharging) MatrixGreen else NeonCyan,
                    trackColor = CyberSurfaceBorder
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sentinel Alert Feature 1: 100% Full Charge Alert
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, if (isFullChargeAlertEnabled) MatrixGreen.copy(alpha = 0.3f) else CyberSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "تنبيه اكتمال الشحن 100% (تجنب التلف)" else "100% FULL CHARGE VOICE ALERT",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "ينبهك بصوت طبيعي عند امتلاء البطارية لتفصل الشاحن فوراً وتحمي الخلايا من التلف الحراري." else "Speaks naturally when 100% full to unplug charger and protect battery health.",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isFullChargeAlertEnabled,
                                onCheckedChange = { viewModel.toggleFullChargeSentinel(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MatrixGreen,
                                    checkedTrackColor = MatrixGreen.copy(alpha = 0.3f),
                                    uncheckedThumbColor = HologramTextMuted,
                                    uncheckedTrackColor = CyberSurfaceBorder
                                ),
                                modifier = Modifier.testTag("full_charge_alert_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.triggerFullChargeAlert() },
                            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("test_full_charge_voice_button")
                        ) {
                            Text(
<<<<<<< HEAD
                                text = if (isArabic) "استمع لتنبيه اكتمال الشحن 100% (تجربة الصوت)" else "Play 100% Full Charge Voice Sample",
=======
                                text = if (isArabic) "🔊 استمع لتنبيه اكتمال الشحن 100% (تجربة الصوت)" else "🔊 Play 100% Full Charge Voice Sample",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 12.sp,
                                color = MatrixGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sentinel Alert Feature 2: 15% Low Battery Alert
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, if (isLowBatteryAlertEnabled) NeonMagenta.copy(alpha = 0.3f) else CyberSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "إنذار انخفاض البطارية أقل من 20% بالصوت" else "LOW BATTERY ALERT (< 20%) & VOICE ALARM",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "ينبهك صوتياً بإنذار عند وصول البطارية لأقل من 20% لتوصيل الشاحن وتفادي إغلاق الهاتف." else "Voice & alarm alerts when battery drops below 20% to connect charger.",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isLowBatteryAlertEnabled,
                                onCheckedChange = { viewModel.toggleLowBatterySentinel(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonMagenta,
                                    checkedTrackColor = NeonMagenta.copy(alpha = 0.3f),
                                    uncheckedThumbColor = HologramTextMuted,
                                    uncheckedTrackColor = CyberSurfaceBorder
                                ),
                                modifier = Modifier.testTag("low_battery_alert_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.triggerLowBatteryAlert() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("test_low_battery_voice_button")
                        ) {
                            Text(
<<<<<<< HEAD
                                text = if (isArabic) "تجربة إنذار البطارية أقل من 20% بصوت المساعد" else "Play <20% Low Battery Voice Alert",
=======
                                text = if (isArabic) "🔊 تجربة إنذار البطارية أقل من 20% بصوت المساعد" else "🔊 Play <20% Low Battery Voice Alert",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 12.sp,
                                color = NeonMagenta
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // One-tap Direct Power Saver Action
                Button(
                    onClick = { viewModel.executePowerSaver() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .border(1.dp, NeonYellow.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .testTag("execute_power_saver_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Power Saver",
                            tint = NeonYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
<<<<<<< HEAD
                            text = if (isArabic) "تفعيل وضع توفير الطاقة الفائق (إطفاء الفلاش وخفض الصوت والسطوع)" else "Activate Super Power Saver Mode Now",
=======
                            text = if (isArabic) "⚡ تفعيل وضع توفير الطاقة الفائق (إطفاء الفلاش وخفض الصوت والسطوع)" else "⚡ Activate Super Power Saver Mode Now",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            color = NeonYellow
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Smart Voice Message Responder Card (الرد الذكي والآلي على الرسائل)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurface)
                .border(
                    1.dp,
                    if (isMessageVoiceReadingEnabled) NeonCyan else CyberSurfaceBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(16.dp)
                .testTag("smart_voice_message_responder_card")
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isMessageVoiceReadingEnabled) NeonCyan.copy(alpha = 0.2f) else CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Smart Message Responder",
                                tint = if (isMessageVoiceReadingEnabled) NeonCyan else HologramTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "الرد الصوتي المباشر على الرسائل" else "VOICE MESSAGE RESPONDER",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 13.sp,
                                color = if (isMessageVoiceReadingEnabled) NeonCyan else HologramTextPrimary
                            )
                            Text(
                                text = if (isArabic) "قراءة رسائل واتساب وتيليجرام والرد الصوتي والآلي" else "Hands-free voice reading, voice replies & auto-responder",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notification Access Permission Status Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isNotificationAccessGranted) MatrixGreen.copy(alpha = 0.12f)
                            else NeonYellow.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isNotificationAccessGranted) MatrixGreen.copy(alpha = 0.4f)
                            else NeonYellow.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isNotificationAccessGranted) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                contentDescription = "Notification Access",
                                tint = if (isNotificationAccessGranted) MatrixGreen else NeonYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isNotificationAccessGranted) {
<<<<<<< HEAD
                                        if (isArabic) "إذن قراءة إشعارات الرسائل: مفعل بنجاح" else "Notification Listener Access: GRANTED"
=======
                                        if (isArabic) "إذن قراءة إشعارات الرسائل: مفعل بنجاح ✓" else "Notification Listener Access: GRANTED ✓"
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                    } else {
                                        if (isArabic) "إذن قراءة الإشعارات: مطلوب للبدء" else "Notification Listener Access: REQUIRED"
                                    },
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 11.sp,
                                    color = if (isNotificationAccessGranted) MatrixGreen else NeonYellow
                                )
                                Text(
                                    text = if (isNotificationAccessGranted) {
                                        if (isArabic) "المساعد جاهز لاكتشاف رسائل واتساب وتيليجرام وSMS فور وصولها." else "Active listening for WhatsApp, Telegram & SMS messages."
                                    } else {
                                        if (isArabic) "اضغط لمنح إذن قراءة الإشعارات في إعدادات النظام ليعمل المساعد." else "Grant permission in Android Settings to enable reading incoming chats."
                                    },
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 10.sp,
                                    color = HologramTextSecondary
                                )
                            }
                        }
                        if (!isNotificationAccessGranted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.openNotificationSettings() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonYellow.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("grant_notification_access_btn")
                            ) {
                                Text(
                                    text = if (isArabic) "تفعيل الإذن" else "GRANT",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = NeonYellow
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 1: Optional Master Setting for Voice Reading & Voice Reply
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(
                            1.dp,
                            if (isMessageVoiceReadingEnabled) NeonCyan.copy(alpha = 0.35f) else CyberSurfaceBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "تفعيل قراءة الرسائل والرد الصوتي عليها" else "VOICE READING & HANDS-FREE REPLY",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 12.sp,
                                color = HologramTextPrimary
                            )
                            Text(
                                text = if (isArabic) "ينطق: «رسالة جديدة من [فلان]: [النص]. هل تود الرد؟» وترد بصوتك: «نعم قل له أنا في الطريق» ويرسل فوراً!" else "Announces new messages and prompts for hands-free voice replies via Android RemoteInput without opening the app.",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isMessageVoiceReadingEnabled,
                            onCheckedChange = { viewModel.toggleMessageVoiceReading(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = HologramTextMuted,
                                uncheckedTrackColor = CyberSurfaceBorder
                            ),
                            modifier = Modifier.testTag("toggle_voice_message_reading_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle 2: Auto-reply during Driving Mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(
                            1.dp,
                            if (isAutoReplyDrivingEnabled) MatrixGreen.copy(alpha = 0.35f) else CyberSurfaceBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Driving Auto Reply",
                                tint = if (isAutoReplyDrivingEnabled) MatrixGreen else HologramTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "الرد الآلي في وضع القيادة" else "DRIVING MODE AUTO-REPLY",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "يرسل تلقائياً للراسل: «أنا أقود السيارة حالياً، سأعاود التواصل معك لاحقاً»" else "Automatically replies: 'I am driving right now, will contact you later.'",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAutoReplyDrivingEnabled,
                            onCheckedChange = { viewModel.toggleAutoReplyDriving(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MatrixGreen,
                                checkedTrackColor = MatrixGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = HologramTextMuted,
                                uncheckedTrackColor = CyberSurfaceBorder
                            ),
                            modifier = Modifier.testTag("toggle_driving_auto_reply_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle 3: Auto-reply during Work Mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(
                            1.dp,
                            if (isAutoReplyWorkEnabled) NeonMagenta.copy(alpha = 0.35f) else CyberSurfaceBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = "Work Auto Reply",
                                tint = if (isAutoReplyWorkEnabled) NeonMagenta else HologramTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "الرد الآلي في وضع العمل والاجتماعات" else "WORK MODE AUTO-REPLY",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "يرسل تلقائياً للراسل: «أنا في اجتماع عمل حالياً، سأعاود التواصل معك لاحقاً»" else "Automatically replies: 'I am in a meeting, will reply later.'",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAutoReplyWorkEnabled,
                            onCheckedChange = { viewModel.toggleAutoReplyWork(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonMagenta,
                                checkedTrackColor = NeonMagenta.copy(alpha = 0.3f),
                                uncheckedThumbColor = HologramTextMuted,
                                uncheckedTrackColor = CyberSurfaceBorder
                            ),
                            modifier = Modifier.testTag("toggle_work_auto_reply_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Audio Test Button
                Button(
                    onClick = { viewModel.simulateIncomingMessageTest() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("simulate_incoming_msg_test_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = "Test Voice Reply",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
<<<<<<< HEAD
                            text = if (isArabic) "تجربة نطق رسالة واردة وسؤال المساعد عن الرد" else "Test Incoming Message Voice Readout",
=======
                            text = if (isArabic) "🔊 تجربة نطق رسالة واردة وسؤال المساعد عن الرد" else "🔊 Test Incoming Message Voice Readout",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Anti-Theft & Intruder Alarm Card (حارس السرقة وفصل الشاحن)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurface)
                .border(
                    1.dp,
                    if (isAntiTheftArmed) NeonMagenta else CyberSurfaceBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(16.dp)
                .testTag("anti_theft_alarm_card")
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isAntiTheftArmed) NeonMagenta.copy(alpha = 0.2f) else CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAntiTheftArmed) Icons.Default.Security else Icons.Default.Shield,
                                contentDescription = "Anti-Theft Guard",
                                tint = if (isAntiTheftArmed) NeonMagenta else HologramTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "حارس السرقة وفصل الشاحن (Anti-Theft)" else "ANTI-THEFT & INTRUDER ALARM",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 13.sp,
                                color = if (isAntiTheftArmed) NeonMagenta else HologramTextPrimary
                            )
                            Text(
                                text = if (isArabic) "حماية الهاتف في المجلس والمكتب وإطلاق صفارة إنذار وفلاش" else "Siren, strobe flash & voice alert upon movement or unplug",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Armed Status Banner & Master Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isAntiTheftArmed -> NeonMagenta.copy(alpha = 0.12f)
                                isAntiTheftArmingCountdown -> NeonYellow.copy(alpha = 0.12f)
                                else -> CyberSurfaceVariant
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isAntiTheftArmed -> NeonMagenta.copy(alpha = 0.5f)
                                isAntiTheftArmingCountdown -> NeonYellow.copy(alpha = 0.5f)
                                else -> CyberSurfaceBorder
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
<<<<<<< HEAD
                                    isAntiTheftArmed -> if (isArabic) "وضع الحماية نشط ومفعل" else "ANTI-THEFT GUARD: ARMED"
                                    isAntiTheftArmingCountdown -> if (isArabic) "⏳ جاري التفعيل خلال $antiTheftCountdownSeconds ثوانٍ..." else "⏳ ARMING IN $antiTheftCountdownSeconds SECONDS..."
                                    else -> if (isArabic) "وضع الحماية متوقف حالياً" else "ANTI-THEFT: DISARMED"
=======
                                    isAntiTheftArmed -> if (isArabic) "🛡️ وضع الحماية نشط ومفعل" else "🛡️ ANTI-THEFT GUARD: ARMED"
                                    isAntiTheftArmingCountdown -> if (isArabic) "⏳ جاري التفعيل خلال $antiTheftCountdownSeconds ثوانٍ..." else "⏳ ARMING IN $antiTheftCountdownSeconds SECONDS..."
                                    else -> if (isArabic) "⚠️ وضع الحماية متوقف حالياً" else "⚠️ ANTI-THEFT: DISARMED"
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                },
                                fontFamily = OrbitronFontFamily,
                                fontSize = 12.sp,
                                color = when {
                                    isAntiTheftArmed -> NeonMagenta
                                    isAntiTheftArmingCountdown -> NeonYellow
                                    else -> HologramTextMuted
                                }
                            )
                            Text(
                                text = when {
                                    isAntiTheftArmed -> if (isArabic) "الهاتف محمي! أي حركة أو فصل للشاحن ستطلق الإنذار فوراً." else "Device armed. Any motion or unplugging will blast the alarm."
                                    isAntiTheftArmingCountdown -> if (isArabic) "ضع هاتفك بثبات على الطاولة قبل انتهاء الوقت." else "Place phone flat on table before timer ends."
                                    else -> if (isArabic) "اضغط للبدء قبل ترك هاتفك على الشاحن أو الطاولة." else "Tap to arm before leaving phone on desk or charger."
                                },
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        when {
                            isAntiTheftArmed -> {
                                Button(
                                    onClick = { viewModel.disarmAntiTheftQuietly() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp).testTag("disarm_anti_theft_btn")
                                ) {
                                    Text(
                                        text = if (isArabic) "إيقاف الحماية" else "DISARM",
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            isAntiTheftArmingCountdown -> {
                                Button(
                                    onClick = { viewModel.cancelAntiTheftArming() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp).testTag("cancel_anti_theft_btn")
                                ) {
                                    Text(
                                        text = if (isArabic) "إلغاء" else "CANCEL",
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 11.sp,
                                        color = NeonYellow
                                    )
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { viewModel.armAntiTheft() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp).testTag("arm_anti_theft_btn")
                                ) {
                                    Text(
                                        text = if (isArabic) "تفعيل الحماية" else "ARM NOW",
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 11.sp,
                                        color = NeonMagenta
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Setting 1: Trigger on Charger Disconnection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerOff,
                                contentDescription = "Charger Unplug Guard",
                                tint = if (antiTheftTriggerOnCharger) NeonMagenta else HologramTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "إنذار عند فصل كابل الشاحن" else "ALARM ON CHARGER UNPLUG",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "يطلق صفارة الإنذار فور سحب الشاحن من الهاتف." else "Blasts siren immediately if the charging cable is disconnected.",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = antiTheftTriggerOnCharger,
                            onCheckedChange = { viewModel.toggleAntiTheftChargerGuard(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonMagenta,
                                checkedTrackColor = NeonMagenta.copy(alpha = 0.3f),
                                uncheckedThumbColor = HologramTextMuted,
                                uncheckedTrackColor = CyberSurfaceBorder
                            ),
                            modifier = Modifier.testTag("toggle_charger_unplug_guard_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Setting 2: Trigger on Motion / Pick-up
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Motion Guard",
                                tint = if (antiTheftTriggerOnMotion) MatrixGreen else HologramTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "إنذار عند تحريك الهاتف أو لمسه (حساس الحركة)" else "ALARM ON MOTION / PICK-UP",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "يطلق صفارة الإنذار فور محاولة أي شخص رفع الهاتف أو تحريكه." else "Triggers immediately if anyone lifts or moves the phone.",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = antiTheftTriggerOnMotion,
                            onCheckedChange = { viewModel.toggleAntiTheftMotionGuard(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MatrixGreen,
                                checkedTrackColor = MatrixGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = HologramTextMuted,
                                uncheckedTrackColor = CyberSurfaceBorder
                            ),
                            modifier = Modifier.testTag("toggle_motion_guard_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Setting 3: Security PIN & Biometrics Config
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "PIN Lock",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "رمز الأمان والبصمة لإلغاء الإنذار" else "SECURITY PIN & BIOMETRICS",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "الرمز الحالي: •••• ($securityPin) - يدعم بصمة الإصبع أيضاً" else "Current PIN: •••• ($securityPin) - Fingerprint supported",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showPinChangeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("change_pin_btn")
                        ) {
                            Text(
                                text = if (isArabic) "تعديل الرمز" else "EDIT PIN",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Setting 4: Full-Screen Intruder Lock Overlay Switch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Lock Screen Overlay",
                                tint = if (isAntiTheftLockOverlayEnabled) NeonMagenta else HologramTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
<<<<<<< HEAD
                                    text = if (isArabic) "شاشة القفل عند محاولة الدخول" else "INTRUDER LOCK OVERLAY",
=======
                                    text = if (isArabic) "شاشة القفل التام النيون (Intruder Lock Overlay)" else "INTRUDER LOCK OVERLAY",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "شاشة حمراء نيون نابضة تمنع استخدام الهاتف ولا تزول إلا بالرمز أو البصمة." else "Locked pulsing red neon overlay blocking usage until PIN or Fingerprint verified.",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAntiTheftLockOverlayEnabled,
                            onCheckedChange = { viewModel.toggleAntiTheftLockOverlay(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonMagenta,
                                checkedTrackColor = NeonMagenta.copy(alpha = 0.3f),
                                uncheckedThumbColor = HologramTextMuted,
                                uncheckedTrackColor = CyberSurfaceBorder
                            ),
                            modifier = Modifier.testTag("toggle_lock_overlay_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Setting 5: Intruder Silent Selfie & Records
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable { viewModel.openIntrudersDialog() }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Intruder Selfie",
                                tint = NeonMagenta,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
<<<<<<< HEAD
                                    text = if (isArabic) "صور من حاول فتح هاتفك" else "INTRUDER SILENT SELFIE LOGS",
=======
                                    text = if (isArabic) "تصوير المتلصصين وسجل الحماية (Intruder Selfie)" else "INTRUDER SILENT SELFIE LOGS",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    color = HologramTextPrimary
                                )
                                Text(
                                    text = if (isArabic) "التقاط صورة صامتة للوجه فور محاولة اللمس أو خطأ الرمز مرتين." else "Silent front camera capture on unauthorized touch or 2 wrong PINs.",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.openIntrudersDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).testTag("open_intruders_gallery_btn")
                        ) {
                            Text(
                                text = if (isArabic) "عرض الصور (${intruderRecords.size})" else "VIEW",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = NeonMagenta
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Siren & Strobe Button
                Button(
                    onClick = { viewModel.testTriggerAntiTheftAlarm() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .border(1.dp, NeonMagenta.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .testTag("test_siren_alarm_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Test Siren",
                            tint = NeonMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
<<<<<<< HEAD
                            text = if (isArabic) "تجربة إطلاق صفارة الإنذار وفلاش الكشاف والرمز" else "Test Full Siren, Strobe & Lock Screen Now",
=======
                            text = if (isArabic) "🚨 تجربة إطلاق صفارة الإنذار وفلاش الكشاف والرمز" else "🚨 Test Full Siren, Strobe & Lock Screen Now",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 12.sp,
                            color = NeonMagenta
                        )
                    }
                }
            }
        }

        if (showPinChangeDialog) {
            var newPinInput by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showPinChangeDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberDarkBg)
                        .border(1.dp, NeonCyan, RoundedCornerShape(14.dp))
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isArabic) "تعديل رمز الأمان (PIN)" else "CHANGE SECURITY PIN",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 14.sp,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "أدخل رمز أمان جديد مكون من 4 أرقام لإيقاف الإنذار:" else "Enter a new 4-digit PIN to disarm anti-theft alarms:",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 12.sp,
                            color = HologramTextMuted
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                            placeholder = { Text("4 أرقام (مثال: 5678)", color = HologramTextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberSurfaceBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_pin_input_field")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showPinChangeDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "إلغاء" else "Cancel", color = HologramTextMuted)
                            }
                            Button(
                                onClick = {
                                    if (newPinInput.length == 4) {
                                        viewModel.updateSecurityPin(newPinInput)
                                        showPinChangeDialog = false
                                    }
                                },
                                enabled = newPinInput.length == 4,
                                colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen),
                                modifier = Modifier.weight(1f).testTag("save_new_pin_btn")
                            ) {
                                Text(if (isArabic) "حفظ الرمز" else "Save", color = CyberDarkBg)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // System Settings Direct Hub
        Text(
            text = if (isArabic) "اختصارات الإعدادات السريعة" else "QUICK SETTINGS SHORTCUTS",
            fontFamily = OrbitronFontFamily,
            fontSize = 12.sp,
            color = HologramTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickSystemBtn(
                title = "واي فاي",
                icon = Icons.Default.Wifi,
                onClick = { viewModel.systemCommander.openWifiSettings() },
                modifier = Modifier.weight(1f)
            )
            QuickSystemBtn(
                title = "بلوتوث",
                icon = Icons.Default.Bluetooth,
                onClick = { viewModel.systemCommander.openBluetoothSettings() },
                modifier = Modifier.weight(1f)
            )
            QuickSystemBtn(
                title = if (isArabic) "الشاشة" else "Display",
                icon = Icons.Default.BrightnessMedium,
                onClick = { viewModel.systemCommander.openDisplaySettings() },
                modifier = Modifier.weight(1f)
            )
            QuickSystemBtn(
                title = if (isArabic) "الكاميرا" else "Camera",
                icon = Icons.Default.CameraAlt,
                onClick = { viewModel.systemCommander.openCamera() },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Accessibility Service Human Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Accessibility,
                        contentDescription = "Accessibility",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "خاصية المساعدة التلقائية (خدمة الوصول Accessibility)" else "ACCESSIBILITY ASSISTANT",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isArabic)
                        "لتشغيل المساعد بأقصى طاقة ومساعدتك في التحكم بشاشة الهاتف والتنقل بين التطبيقات نيابة عنك، يمكنك تفعيل خدمة الوصول من إعدادات الهاتف."
                    else
                        "Enable Accessibility Service in Android Settings to grant automated UI interactions and screen navigation.",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = HologramTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.systemCommander.openAccessibilitySettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic) "فتح إعدادات خدمة الوصول" else "OPEN ACCESSIBILITY SETTINGS",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
fun QuickSystemBtn(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontFamily = RajdhaniFontFamily,
                fontSize = 11.sp,
                color = HologramTextPrimary
            )
        }
    }
}
