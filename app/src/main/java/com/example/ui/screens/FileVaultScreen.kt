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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceFile
import com.example.model.FileCategory
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

@Composable
fun FileVaultScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val files by viewModel.deviceFiles.collectAsState()
    val selectedCategory by viewModel.selectedFileCategory.collectAsState()
    val isArabic by viewModel.isArabicVoice.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

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
                    text = if (isArabic) "ملفات الهاتف والمستندات" else "DEVICE FILES & DOCUMENTS",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 15.sp,
                    color = NeonCyan
                )
                Text(
                    text = if (isArabic) "تقدر تقول بصوتك: 'ابحث عن ملف PDF' أو 'افتح ملف كذا'" else "Voice: 'Find PDF files' or 'Search file [name]'",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = HologramTextMuted
                )
            }

            IconButton(
                onClick = { viewModel.loadDeviceFiles(searchQuery, selectedCategory) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Files",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.loadDeviceFiles(it, selectedCategory)
            },
            placeholder = {
                Text(
                    text = if (isArabic) "ابحث عن أي ملف أو مستند بالاسم..." else "Search files by name...",
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("file_search_field"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberSurfaceBorder,
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Categories Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FileCategory.values()) { cat ->
                val isSelected = selectedCategory == cat
                val label = if (isArabic) cat.labelAr else cat.labelEn
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonCyan else CyberSurfaceVariant)
                        .border(1.dp, if (isSelected) NeonCyan else CyberSurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.loadDeviceFiles(searchQuery, cat)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = if (isSelected) CyberDarkBg else HologramTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${files.size} ${if (isArabic) "ملف موجود في هاتفك" else "FILES FOUND"}",
            fontFamily = OrbitronFontFamily,
            fontSize = 11.sp,
            color = HologramTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Files List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files, key = { it.id }) { file ->
                FileRowCard(
                    file = file,
                    onOpen = { viewModel.openFileDirect(file) }
                )
            }
        }
    }
}

@Composable
fun FileRowCard(
    file: DeviceFile,
    onOpen: () -> Unit
) {
    val icon: ImageVector = when (file.category) {
        FileCategory.PDF -> Icons.Default.PictureAsPdf
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.MEDIA -> Icons.Default.Image
        FileCategory.AUDIO -> Icons.Default.Audiotrack
        FileCategory.DOWNLOADS -> Icons.Default.Download
        FileCategory.ALL -> Icons.Default.Folder
    }

    val iconColor = when (file.category) {
        FileCategory.PDF -> NeonMagenta
        FileCategory.MEDIA -> NeonCyan
        FileCategory.AUDIO -> MatrixGreen
        else -> NeonCyan
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceVariant)
                    .border(1.dp, iconColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = file.name,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = file.name,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 13.sp,
                    color = HologramTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${file.formattedSize} • ${file.path}",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = HologramTextMuted,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onOpen,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open File",
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
