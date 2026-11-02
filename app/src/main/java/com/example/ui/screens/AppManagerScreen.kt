package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shop
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InstalledApp
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
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun AppManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.installedApps.collectAsState()
    val isArabic by viewModel.isArabicVoice.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyUserApps by remember { mutableStateOf(false) }

    val filteredApps = apps.filter { app ->
        (searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)) &&
                (!showOnlyUserApps || !app.isSystemApp)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isArabic) "تطبيقات هاتفك" else "INSTALLED APPLICATIONS",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 15.sp,
                    color = NeonCyan
                )
                Text(
                    text = if (isArabic) "تقدر تقول بصوتك: 'احذف تطبيق كذا' أو 'افتح كذا'" else "Voice: 'Uninstall [app]' or 'Open [app]'",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = HologramTextMuted
                )
            }

            IconButton(
                onClick = { viewModel.loadInstalledApps() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Apps",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (isArabic) "بحث في التطبيقات..." else "Filter applications...",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 13.sp,
                    color = HologramTextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("✕", color = HologramTextMuted, fontSize = 14.sp)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_field"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberSurfaceBorder,
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filters row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredApps.size} ${if (isArabic) "تطبيق موجود" else "APPS FOUND"}",
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                color = HologramTextSecondary
            )

            // Install from Store Direct button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceBorder.copy(alpha = 0.6f))
                    .clickable {
                        viewModel.processVoiceCommand(if (isArabic) "ثبت تطبيق جديد من المتجر" else "Install app from store")
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shop,
                    contentDescription = "Store",
                    tint = NeonCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isArabic) "تثبيت تطبيق جديد" else "INSTALL NEW APP",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Apps List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                AppRowCard(
                    app = app,
                    isArabic = isArabic,
                    onLaunch = { viewModel.launchAppDirect(app) },
                    onUninstall = { viewModel.uninstallAppDirect(app) }
                )
            }
        }
    }
}

@Composable
fun AppRowCard(
    app: InstalledApp,
    isArabic: Boolean,
    onLaunch: () -> Unit,
    onUninstall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon / Placeholder
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceVariant)
                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = app.appName,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = app.appName,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 13.sp,
                    color = HologramTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = HologramTextMuted,
                    maxLines = 1
                )
                Text(
                    text = if (app.isSystemApp) (if (isArabic) "تطبيق نظام" else "System App") else (if (isArabic) "مثبت في هاتفك" else "User App"),
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = if (app.isSystemApp) NeonCyan.copy(alpha = 0.8f) else MatrixGreen.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Launch Button
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "فتح التطبيق",
                    tint = CyberDarkBg,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (isArabic) "فتح" else "OPEN",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = CyberDarkBg
                )
            }

            // Uninstall Button
            Button(
                onClick = onUninstall,
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف التطبيق",
                    tint = NeonMagenta,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (isArabic) "حذف" else "DELETE",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = NeonMagenta
                )
            }
        }
    }
}
