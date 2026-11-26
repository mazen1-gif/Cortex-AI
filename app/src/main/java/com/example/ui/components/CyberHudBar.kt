package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.HologramTextPrimary
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun CyberHudBar(
    batteryLevel: Int,
    isCharging: Boolean,
    isArabic: Boolean,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
<<<<<<< HEAD
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface.copy(alpha = 0.92f))
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
=======
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface.copy(alpha = 0.85f))
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
<<<<<<< HEAD
                    .background(NeonCyan)
=======
                    .background(MatrixGreen)
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "رفيقك" else "CORTEX",
                fontFamily = OrbitronFontFamily,
<<<<<<< HEAD
                fontSize = 18.sp,
=======
                fontSize = 15.sp,
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = HologramTextPrimary
            )
        }

        // Status Controls & Telemetry
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Language Switch Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurfaceBorder.copy(alpha = 0.6f))
                    .clickable(onClick = onToggleLanguage)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("language_toggle_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = NeonMagenta,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
<<<<<<< HEAD
                    text = if (isArabic) "العربية" else "English",
=======
                    text = if (isArabic) "العربية" else "ENGLISH",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                    fontFamily = OrbitronFontFamily,
                    fontSize = 10.sp,
                    color = HologramTextPrimary
                )
            }

            // Battery Telemetry
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurfaceBorder.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    tint = MatrixGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$batteryLevel%",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = MatrixGreen
                )
            }
        }
    }
}
