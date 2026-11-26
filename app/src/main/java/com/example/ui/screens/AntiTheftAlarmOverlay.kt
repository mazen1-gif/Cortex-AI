package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.HologramTextMuted
import com.example.ui.theme.HologramTextPrimary
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

/**
 * شاشة إنذار السرقة الكاملة المقفلة (Anti-Theft Intruder Alarm Overlay)
 * تنطلق فوراً عند فصل الشاحن أو تحريك الهاتف ولا تزول إلا بإدخال الرمز الصحيح أو البصمة.
 */
@Composable
fun AntiTheftAlarmOverlay(
    alarmReason: String,
    onVerifyPin: (String) -> Boolean,
    onBiometricUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var wrongAttemptCount by remember { mutableStateOf(0) }

    // Pulsing siren animation
    val infiniteTransition = rememberInfiniteTransition(label = "siren_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val redGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "red_glow"
    )

    Dialog(
        onDismissRequest = { /* Cannot dismiss without PIN or Fingerprint */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .border(6.dp, NeonMagenta.copy(alpha = redGlowAlpha))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Siren Warning Icon with pulse
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(NeonMagenta.copy(alpha = redGlowAlpha))
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Siren Alert",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
<<<<<<< HEAD
                    text = "إنذار أمني: رصد سرقة!",
=======
                    text = "🚨 إنذار أمني: رصد سرقة!",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                    fontFamily = OrbitronFontFamily,
                    fontSize = 22.sp,
                    color = NeonMagenta
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (alarmReason.isNotEmpty()) alarmReason else "تم فصل كابل الشاحن أو تحريك الهاتف دون إذن!",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 14.sp,
                    color = Color.White
                )

                Text(
                    text = "أدخل رمز الأمان (PIN) أو استخدم البصمة فوراً لإيقاف الإنذار",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = HologramTextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isPinError -> NeonMagenta
                                        isFilled -> MatrixGreen
                                        else -> CyberSurfaceBorder
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isPinError) NeonMagenta else if (isFilled) MatrixGreen else CyberSurfaceBorder,
                                    CircleShape
                                )
                        )
                    }
                }

                if (isPinError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
<<<<<<< HEAD
                        text = if (wrongAttemptCount >= 2) "تم التقاط صورة المتلصص بالكاميرا وسُجلت!" else "رمز الأمان خاطئ! حاول مجدداً",
=======
                        text = if (wrongAttemptCount >= 2) "🚨 تم التقاط صورة المتلصص بالكاميرا وسُجلت!" else "رمز الأمان خاطئ! حاول مجدداً",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = NeonMagenta
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cyber Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("FP", "0", "DEL")
                    )

                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { key ->
                                when (key) {
                                    "FP" -> {
                                        // Fingerprint button
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .clip(CircleShape)
                                                .background(MatrixGreen.copy(alpha = 0.2f))
                                                .border(1.dp, MatrixGreen, CircleShape)
                                                .clickable { onBiometricUnlock() }
                                                .testTag("biometric_unlock_btn"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "بصمة الإصبع",
                                                tint = MatrixGreen,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    "DEL" -> {
                                        // Delete button
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .clip(CircleShape)
                                                .background(CyberSurface)
                                                .border(1.dp, CyberSurfaceBorder, CircleShape)
                                                .clickable {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        isPinError = false
                                                    }
                                                }
                                                .testTag("delete_pin_digit_btn"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "حذف",
                                                tint = HologramTextMuted,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        // Digit button
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .clip(CircleShape)
                                                .background(CyberSurface)
                                                .border(1.dp, CyberSurfaceBorder, CircleShape)
                                                .clickable {
                                                    if (enteredPin.length < 4) {
                                                        val next = enteredPin + key
                                                        enteredPin = next
                                                        isPinError = false
                                                        if (next.length == 4) {
                                                            val success = onVerifyPin(next)
                                                            if (!success) {
                                                                wrongAttemptCount++
                                                                isPinError = true
                                                                enteredPin = ""
                                                            } else {
                                                                wrongAttemptCount = 0
                                                            }
                                                        }
                                                    }
                                                }
                                                .testTag("keypad_digit_$key"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = key,
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 20.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "الرمز الافتراضي: 1234 (أو بصمة إصبعك)",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = HologramTextMuted
                )
            }
        }
    }
}
