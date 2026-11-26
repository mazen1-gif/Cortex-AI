package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.CommandGuideDialog
import com.example.ui.screens.FileVaultScreen
import com.example.ui.screens.MainAssistantScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SystemTelemetryScreen
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberpunkAITheme
import com.example.ui.theme.HologramTextMuted
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before any UI or Firebase operations
        com.example.util.configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            CyberpunkAITheme {
                CyberpunkAssistantApp(
                    onActivityCreated = { vm ->
                        if (intent?.getBooleanExtra("EXTRA_ARM_ANTI_THEFT", false) == true) {
                            vm.armAntiTheft()
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun CyberpunkAssistantApp(
    viewModel: MainViewModel = viewModel(),
    onActivityCreated: (MainViewModel) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        onActivityCreated(viewModel)
    }
    val currentTab by viewModel.currentNavTab.collectAsState()
    val isArabic by viewModel.isArabicVoice.collectAsState()
    val isAntiTheftAlarmTriggered by viewModel.isAntiTheftAlarmTriggered.collectAsState()
    val isAntiTheftLockOverlayEnabled by viewModel.isAntiTheftLockOverlayEnabled.collectAsState()
    val antiTheftAlarmReason by viewModel.antiTheftAlarmReason.collectAsState()
    var showCommandGuide by remember { mutableStateOf(false) }

    // Audio Permission Request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.log(
                com.example.model.LogLevel.SUCCESS,
                "الصوت",
                "تم تفعيل إذن الميكروفون بنجاح."
            )
        } else {
            viewModel.log(
                com.example.model.LogLevel.WARN,
                "الصوت",
                "تم رفض إذن الميكروفون، يمكنك كتابة الأوامر يدوياً."
            )
        }
    }

    // Location Permission Request (runtime) - يُطلب فقط عند الحاجة لتحديد الموقع
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onLocationPermissionResult(result.values.any { it })
    }

    LaunchedEffect(Unit) {
        viewModel.locationPermissionRequest.collect {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

<<<<<<< HEAD
    // Media/Files permission + PDF picker (يُطلبان فقط عند الحاجة)
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onStoragePermissionResult(result.values.any { it })
    }
    LaunchedEffect(Unit) {
        viewModel.storagePermissionRequest.collect {
            storagePermissionLauncher.launch(viewModel.mediaPermissionsToRequest())
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onPdfPicked(uri)
    }
    LaunchedEffect(Unit) {
        viewModel.pdfPickerRequest.collect {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }
    }

=======
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    // System Voice Recognition Dialog Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setListeningState(false)
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.processVoiceCommand(spokenText)
            } else {
                viewModel.onVoiceInputFailed(
                    if (isArabic) "لم يتم التقاط أي كلام واضح، يرجى المحاولة والتحدث بصوت واضح."
                    else "No words detected, please try again speaking clearly."
                )
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            viewModel.onVoiceInputFailed(
                if (isArabic) "تعذر التقاط الصوت، جرب التحدث مجدداً أو كتابة طلبك."
                else "Could not capture voice, please try again."
            )
        } else {
            viewModel.onVoiceInputCancelled()
        }
    }

    val onPressStart: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.startHoldToTalk()
        }
    }

    val onPressEnd: () -> Unit = {
        viewModel.stopHoldToTalk()
    }

    val onTriggerVoiceInput: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            val hasSystemRecognizer = try {
                val testIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                val activities = context.packageManager.queryIntentActivities(testIntent, 0)
                activities.isNotEmpty()
            } catch (e: Exception) {
                false
            }

            if (hasSystemRecognizer) {
                try {
                    viewModel.setListeningState(true)
                    val langTag = if (isArabic) "ar-SA" else "en-US"
                    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, if (isArabic) "تحدث الآن، المساعد يستمع إليك..." else "Speak now, Assistant is listening...")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ar", "ar-SA", "en", "en-US"))
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    }
                    speechLauncher.launch(speechIntent)
                } catch (e: Exception) {
                    viewModel.triggerListening()
                }
            } else {
                viewModel.triggerListening()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.systemSpeechRequest.collect {
            onTriggerVoiceInput()
        }
    }

    BackHandler(enabled = currentTab != 0) {
        viewModel.setNavTab(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = CyberDarkBg,
        bottomBar = {
            CyberBottomNavigation(
                currentTab = currentTab,
                isArabic = isArabic,
                onTabSelect = { viewModel.setNavTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> MainAssistantScreen(
                    viewModel = viewModel,
                    onOpenCommandGuide = { showCommandGuide = true },
                    onPressStart = onPressStart,
                    onPressEnd = onPressEnd,
                    onTriggerVoiceDialog = onTriggerVoiceInput
                )
                1 -> RoutinesScreen(
                    viewModel = viewModel
                )
                2 -> AppManagerScreen(
                    viewModel = viewModel
                )
                3 -> FileVaultScreen(
                    viewModel = viewModel
                )
                4 -> SystemTelemetryScreen(
                    viewModel = viewModel
                )
            }

            if (showCommandGuide) {
                CommandGuideDialog(
                    prompts = viewModel.commandPrompts,
                    isArabic = isArabic,
                    onDismiss = { showCommandGuide = false },
                    onSelectCommand = { command ->
                        viewModel.processVoiceCommand(command)
                    }
                )
            }

            if (isAntiTheftAlarmTriggered && isAntiTheftLockOverlayEnabled) {
                com.example.ui.screens.AntiTheftAlarmOverlay(
                    alarmReason = antiTheftAlarmReason,
                    onVerifyPin = { pin -> viewModel.verifyAntiTheftPin(pin) },
                    onBiometricUnlock = { viewModel.disarmAntiTheftWithBiometric() }
                )
            }
        }
    }
}

@Composable
fun CyberBottomNavigation(
    currentTab: Int,
    isArabic: Boolean,
    onTabSelect: (Int) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = CyberSurface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(if (isArabic) "المساعد" else "ASSISTANT", Icons.Default.GraphicEq, 0),
            Triple(if (isArabic) "السيناريوهات" else "ROUTINES", Icons.Default.ElectricBolt, 1),
            Triple(if (isArabic) "التطبيقات" else "APPS", Icons.Default.Apps, 2),
            Triple(if (isArabic) "الملفات" else "FILES", Icons.Default.FolderOpen, 3),
            Triple(if (isArabic) "التحكم" else "CONTROLS", Icons.Default.Memory, 4)
        )

        items.forEach { (label, icon, index) ->
            val isSelected = currentTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelect(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberDarkBg,
                    selectedTextColor = NeonCyan,
                    indicatorColor = NeonCyan,
                    unselectedIconColor = HologramTextMuted,
                    unselectedTextColor = HologramTextMuted
                ),
                modifier = Modifier.testTag("nav_tab_$index")
            )
        }
    }
}
