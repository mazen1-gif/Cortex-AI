package com.example.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.IntentParser
import com.example.engine.SystemCommander
import com.example.engine.VoiceEngine
import com.example.security.AntiTheftManager
import com.example.notes.VoiceNotesManager
import com.example.model.AssistantState
import com.example.model.CommandPrompt
import com.example.model.CyberRoutine
import com.example.model.DeviceFile
import com.example.model.FileCategory
import com.example.model.InstalledApp
import com.example.model.LogLevel
import com.example.model.NoteType
import com.example.model.ParsedAction
import com.example.model.TerminalLog
import com.example.model.VoiceNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val systemCommander = SystemCommander(application.applicationContext)

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLog>> = _terminalLogs.asStateFlow()

    private val _lastSpokenTranscript = MutableStateFlow("")
    val lastSpokenTranscript: StateFlow<String> = _lastSpokenTranscript.asStateFlow()

    private val _lastVoiceResponse = MutableStateFlow("أهلاً بك! أنا مساعدك الصوتي، استمر بالضغط وتحدث معي بأي شيء تحتاجه.")
    val lastVoiceResponse: StateFlow<String> = _lastVoiceResponse.asStateFlow()

    private val _systemSpeechRequest = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val systemSpeechRequest: kotlinx.coroutines.flow.SharedFlow<Unit> = _systemSpeechRequest

    private val _isArabicVoice = MutableStateFlow(true)
    val isArabicVoice: StateFlow<Boolean> = _isArabicVoice.asStateFlow()

    // Apps & Files State
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _deviceFiles = MutableStateFlow<List<DeviceFile>>(emptyList())
    val deviceFiles: StateFlow<List<DeviceFile>> = _deviceFiles.asStateFlow()

    private val _selectedFileCategory = MutableStateFlow(FileCategory.ALL)
    val selectedFileCategory: StateFlow<FileCategory> = _selectedFileCategory.asStateFlow()

    // Telemetry State
    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _batteryTemp = MutableStateFlow("32.5°C")
    val batteryTemp: StateFlow<String> = _batteryTemp.asStateFlow()

    private val _memoryUsage = MutableStateFlow("3150MB / 6144MB")
    val memoryUsage: StateFlow<String> = _memoryUsage.asStateFlow()

    private val _isFlashlightActive = MutableStateFlow(false)
    val isFlashlightActive: StateFlow<Boolean> = _isFlashlightActive.asStateFlow()

    private val _volumePercent = MutableStateFlow(60)
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    // Battery Sentinel State (الحارس الأمني للبطارية)
    private val _isFullChargeSentinelEnabled = MutableStateFlow(true)
    val isFullChargeSentinelEnabled: StateFlow<Boolean> = _isFullChargeSentinelEnabled.asStateFlow()

    private val _isLowBatterySentinelEnabled = MutableStateFlow(true)
    val isLowBatterySentinelEnabled: StateFlow<Boolean> = _isLowBatterySentinelEnabled.asStateFlow()

    // Smart Voice Message Responder State (مجيب الرسائل الذكي الصوتي)
    private val responderPrefs = application.getSharedPreferences(
        com.example.service.SmartMessageResponderService.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val _isMessageVoiceReadingEnabled = MutableStateFlow(
        responderPrefs.getBoolean(com.example.service.SmartMessageResponderService.KEY_VOICE_READING_ENABLED, false)
    )
    val isMessageVoiceReadingEnabled: StateFlow<Boolean> = _isMessageVoiceReadingEnabled.asStateFlow()

    private val _isAutoReplyDrivingEnabled = MutableStateFlow(
        responderPrefs.getBoolean(com.example.service.SmartMessageResponderService.KEY_AUTO_REPLY_DRIVING, true)
    )
    val isAutoReplyDrivingEnabled: StateFlow<Boolean> = _isAutoReplyDrivingEnabled.asStateFlow()

    private val _isAutoReplyWorkEnabled = MutableStateFlow(
        responderPrefs.getBoolean(com.example.service.SmartMessageResponderService.KEY_AUTO_REPLY_WORK, false)
    )
    val isAutoReplyWorkEnabled: StateFlow<Boolean> = _isAutoReplyWorkEnabled.asStateFlow()

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    // Anti-Theft Guard (حارس السرقة وفصل الشاحن)
    val antiTheftManager by lazy { AntiTheftManager(application, voiceEngine) }
    val isAntiTheftArmed: StateFlow<Boolean> by lazy { antiTheftManager.isArmed }
    val isAntiTheftArmingCountdown: StateFlow<Boolean> by lazy { antiTheftManager.isArmingCountdown }
    val antiTheftCountdownSeconds: StateFlow<Int> by lazy { antiTheftManager.countdownSeconds }
    val isAntiTheftAlarmTriggered: StateFlow<Boolean> by lazy { antiTheftManager.isAlarmTriggered }
    val antiTheftAlarmReason: StateFlow<String> by lazy { antiTheftManager.alarmReason }
    val antiTheftTriggerOnCharger: StateFlow<Boolean> by lazy { antiTheftManager.triggerOnChargerDisconnect }
    val antiTheftTriggerOnMotion: StateFlow<Boolean> by lazy { antiTheftManager.triggerOnMotion }
    val antiTheftPin: StateFlow<String> by lazy { antiTheftManager.securityPin }
    val isAntiTheftLockOverlayEnabled: StateFlow<Boolean> by lazy { antiTheftManager.isLockOverlayEnabled }

    // Smart Voice Memos & Action Tasks (مذكرات الأفكار والمهام الفورية)
    val voiceNotesManager = VoiceNotesManager.getInstance(application)
    val voiceNotes: StateFlow<List<VoiceNote>> = voiceNotesManager.notes

    // Voice Expense Tracker & Wallet (مسجل المصاريف والمحفظة الصوتية الفورية)
    val expenseManager = com.example.wallet.ExpenseManager.getInstance(application)
    val expenses: StateFlow<List<com.example.model.VoiceExpense>> = expenseManager.expenses
    val monthlyBudget: StateFlow<Double> = expenseManager.monthlyBudget

    fun addExpenseDirect(amount: Double, category: com.example.model.ExpenseCategory, desc: String) {
        val item = expenseManager.addExpense(amount, category, desc)
        log(LogLevel.EXEC, "تسجيل مصروف", "تم تسجيل ${item.amount} ريال (${item.category.labelAr})")
    }

    fun deleteExpenseDirect(id: String) {
        expenseManager.deleteExpense(id)
        log(LogLevel.INFO, "حذف مصروف", "تم حذف المصروف بنجاح.")
    }

    fun setBudgetDirect(budget: Double) {
        expenseManager.setMonthlyBudget(budget)
        log(LogLevel.INFO, "الميزانية", "تم تحديث الميزانية الشهرية إلى $budget ريال.")
    }

    // Intruder Silent Selfie & Security Records (تصوير وسجل المتلصصين)
    val intruderSecurityManager = com.example.security.IntruderSecurityManager.getInstance(application)
    val intruderRecords: StateFlow<List<com.example.model.IntruderRecord>> = intruderSecurityManager.intruderRecords
    val latestIntruderAlert: StateFlow<com.example.model.IntruderRecord?> = intruderSecurityManager.latestAlert
    val hasNewIntruderAlert: StateFlow<Boolean> = intruderSecurityManager.hasNewIntruderAlert

    private val _showIntrudersDialog = MutableStateFlow(false)
    val showIntrudersDialog: StateFlow<Boolean> = _showIntrudersDialog.asStateFlow()

    fun openIntrudersDialog() {
        _showIntrudersDialog.value = true
    }

    fun closeIntrudersDialog() {
        _showIntrudersDialog.value = false
        intruderSecurityManager.dismissLatestAlert()
    }

    fun deleteIntruderRecord(id: String) {
        intruderSecurityManager.deleteRecord(id)
        log(LogLevel.INFO, "سجل الحماية", "تم حذف السجل المحدد.")
    }

    fun clearAllIntruderRecords() {
        intruderSecurityManager.clearAllRecords()
        log(LogLevel.INFO, "سجل الحماية", "تم مسح جميع سجلات المتلصصين.")
    }

    fun closeBackgroundAppsDirect() {
        val (success, msg) = systemCommander.closeBackgroundAppsAndFreeMemory()
        refreshTelemetry()
        log(if (success) LogLevel.EXEC else LogLevel.WARN, "إغلاق التطبيقات", msg)
        voiceEngine?.speak(msg, _isArabicVoice.value)
    }

    private var hasAlertedFullCharge = false
    private var hasAlertedLowBattery = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (rawLevel != -1 && scale != -1) (rawLevel * 100 / scale) else _batteryLevel.value
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    val temp = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10.0

                    _batteryLevel.value = pct
                    _isCharging.value = charging
                    _batteryTemp.value = "${temp}°C"

                    checkBatterySentinelTriggers(pct, charging)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    _isCharging.value = false
                    hasAlertedFullCharge = false
                    log(LogLevel.INFO, "الشاحن", "تم فصل كابل الشاحن.")
                    voiceEngine?.speak("تم فصل كابل الشاحن.", _isArabicVoice.value)
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    _isCharging.value = true
                    hasAlertedLowBattery = false
                    log(LogLevel.INFO, "الشاحن", "تم توصيل كابل الشاحن، جاري الشحن...")
                    playAlertSound()
                    voiceEngine?.speak("تم توصيل الشاحن، جاري شحن جهازك وحماية البطارية.", _isArabicVoice.value)
                }
            }
        }
    }

    // Active Navigation Tab
    private val _currentNavTab = MutableStateFlow(0)
    val currentNavTab: StateFlow<Int> = _currentNavTab.asStateFlow()

    // Audio Visualizer RMS
    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var voiceEngine: VoiceEngine? = null

    val commandPrompts = listOf(
        CommandPrompt("المحفظة والمصاريف", "EXPENSE", "سجل صرفت 500 ريال تسوق", "Log 500 shopping", "تسجيل مصروف جديد بصوتك فوراً", "Record expense by voice", "wallet"),
        CommandPrompt("المحفظة والمصاريف", "EXPENSE", "حط 100 ريال بنزين", "Log 100 fuel", "تسجيل وقود وبنزين للمركبة", "Log fuel expense", "gas"),
        CommandPrompt("حساب المصاريف", "EXPENSE", "كم صرفت اليوم؟", "How much spent today?", "قراءة مجموع مصاريف اليوم والمتبقي", "Read today's total expenses", "calc"),
        CommandPrompt("حساب المصاريف", "EXPENSE", "احسب لي مصاريف الأسبوع", "Week expenses", "معرفة مصاريف الأسبوع كاملة", "Weekly expense report", "report"),
        CommandPrompt("واتساب بصوتك", "WHATSAPP", "اشتيك تفتح لي الواتس وتعمل رسالة لـ محمد وقله أهلاً بك", "Open WhatsApp & message", "فتح واتساب وتجهيز الرسالة لجهة الاتصال بصوتك", "Compose WhatsApp voice message", "send"),
        CommandPrompt("فحص الهاتف", "DIAGNOSTICS", "افحص لي التلفون فحص عميق", "Deep device scan", "فحص المعالج والذاكرة والتخزين والبطارية ومحتويات الهاتف كاملاً", "Inspect CPU, RAM, storage, battery & files", "scanner"),
        CommandPrompt("صياغة رسالة", "WRITING", "اكتب لي رسالة شكر رسمية لمديري في العمل", "Draft formal email", "صياغة نصوص ورسائل واستشارات راقية وذكية", "Text drafting", "edit"),
        CommandPrompt("استشارة ونقاش", "CONSULT", "كيف أطور مهاراتي وأستغل وقتي؟", "Self improvement advice", "نقاش فكري واستشارات عميقة بصوت المساعد", "Personal consultation", "chat"),
        CommandPrompt("أوامر متعددة", "COMPOUND", "شغل الكشاف واكتم الصوت", "Flashlight on and mute", "تنفيذ أمرين معاً بجملة واحدة بصوتك", "Execute 2 actions in one command", "multi"),
        CommandPrompt("الكشاف", "TORCH", "شغل الكشاف", "Turn on flashlight", "إضاءة كشاف الهاتف", "Turn on flashlight", "flash_on"),
        CommandPrompt("الكشاف", "TORCH", "طفي الكشاف", "Turn off flashlight", "إطفاء كشاف الهاتف", "Turn off flashlight", "flash_off"),
        CommandPrompt("الصوت", "AUDIO", "اكتم الصوت", "Mute sound", "كتم الصوت ووضع الهاتف على الصامت", "Silence all sounds", "mute"),
        CommandPrompt("الصوت", "AUDIO", "ارفع الصوت", "Volume up", "رفع صوت نغمات والوسائط", "Increase media volume", "vol_up"),
        CommandPrompt("البطارية", "BATTERY", "كم باقي بالبطارية؟", "Check battery", "معرفة نسبة الشحن وحرارة الجهاز", "Check battery status", "battery"),
        CommandPrompt("الكاميرا", "CAMERA", "افتح الكاميرا", "Open camera", "فتح كاميرا الهاتف لالتقاط صورة", "Open camera app", "camera"),
        CommandPrompt("التطبيقات", "APPS", "افتح يوتيوب", "Open YouTube", "تشغيل التطبيق فوراً", "Open app immediately", "open"),
        CommandPrompt("التطبيقات", "APPS", "احذف واتساب", "Uninstall WhatsApp", "فتح نافذة حذف التطبيق", "Open uninstall screen", "delete"),
        CommandPrompt("التطبيقات", "APPS", "ثبت تيليجرام", "Install Telegram", "البحث والتثبيت من متجر قوقل", "Search on Play Store", "install"),
        CommandPrompt("الملفات", "FILES", "ابحث عن ملف PDF", "Find PDF files", "البحث عن كل مستندات الـ PDF", "Scan all PDF files", "file"),
        CommandPrompt("الهدوء", "QUIET", "تفعيل وضع صامت", "Quiet mode", "كتم الأصوات وتفعيل الاهتزاز", "Vibrate only mode", "stealth"),
        CommandPrompt("الشبكات", "NETWORK", "افتح الواي فاي", "Open Wi-Fi", "فتح إعدادات شبكات الواي فاي", "Open Wi-Fi settings", "wifi"),
        CommandPrompt("الشبكات", "NETWORK", "افتح البلوتوث", "Open Bluetooth", "فتح إعدادات أجهزة البلوتوث", "Open Bluetooth settings", "bluetooth"),
        CommandPrompt("المتصفح", "WEB", "ابحث في قوقل عن الطقس اليوم", "Search Google for weather", "البحث السريع في محرك قوقل", "Search on Google", "search"),
        CommandPrompt("الوقت", "TIME", "كم الساعة؟", "What time is it?", "معرفة الوقت الحالي بالدقائق", "Check current time", "clock"),
        CommandPrompt("الحساب", "MATH", "احسب 25 في 4", "Calculate 25 * 4", "حساب العمليات الرياضية فوراً", "Instant math calculations", "calc"),
        CommandPrompt("المحادثة", "CHAT", "كيف حالك يا مساعد؟", "How are you?", "محادثة طبيعية وودية", "Natural friendly conversation", "chat"),
        CommandPrompt("ترفيه", "JOKE", "احكي لي نكتة", "Tell me a joke", "نكت ومواقف طريفة تروق مزاجك", "Light-hearted jokes", "smile"),
        CommandPrompt("السيناريوهات", "ROUTINE", "أنا في السيارة", "Car mode", "رفع الصوت 80% وتشغيل الخرائط والبلوتوث", "Drive routine", "car"),
        CommandPrompt("السيناريوهات", "ROUTINE", "وقت النوم", "Bedtime", "كتم الصوت وخفض الإضاءة وإيقاف الواي فاي", "Bedtime routine", "sleep"),
        CommandPrompt("السيناريوهات", "ROUTINE", "أنا في العمل", "Work mode", "تفعيل الاهتزاز وفتح المفكرة للتركيز", "Work routine", "work"),
        CommandPrompt("السيناريوهات", "ROUTINE", "وضع الطوارئ", "Emergency", "تشغيل الفلاش ورفع الصوت للحد الأقصى", "Emergency routine", "emergency"),
        CommandPrompt("حارس البطارية", "SENTINEL", "حارس البطارية", "Battery Sentinel", "تنبيه صوتي طبيعي عند 100% وعند 15%", "Voice alerts at 100% and 15%", "battery_shield"),
        CommandPrompt("توفير الطاقة", "SAVER", "وفر الطاقة", "Save battery", "إطفاء الفلاش وخفض الصوت والسطوع", "Turn off torch, mute, and dim display", "battery_saver"),
        CommandPrompt("حارس السرقة", "ANTI-THEFT", "فعل حارس السرقة", "Arm anti-theft", "صفارة إنذار وفلاش فوري عند فصل الشاحن أو تحريك الهاتف", "Siren and strobe if unplugged or moved", "anti_theft"),
        CommandPrompt("الملاحظات", "MEMO", "سجل ملاحظة: الاجتماع غداً الساعة 4 عصراً", "Save memo: Meeting tomorrow", "حفظ وتوثيق الملاحظات فوراً مع التاريخ والوقت", "Record voice notes", "memo"),
        CommandPrompt("الأفكار", "IDEA", "احفظ لي فكرة شراء بطارية جديدة", "Save idea: Buy battery", "تنظيم الأفكار الفورية وحفظها", "Save quick ideas", "idea"),
        CommandPrompt("الملاحظات", "READ", "ايش عندي ملاحظات؟", "What are my notes?", "قراءة كل الملاحظات والأفكار بصوت المساعد", "Read aloud all saved notes", "read_notes")
    )

    // Floating Orb State
    private val _isFloatingOrbActive = MutableStateFlow(false)
    val isFloatingOrbActive: StateFlow<Boolean> = _isFloatingOrbActive.asStateFlow()

    fun setFloatingOrbActive(active: Boolean) {
        _isFloatingOrbActive.value = active
    }

    val cyberRoutines = listOf(
        CyberRoutine(
            id = "car",
            titleAr = "وضع القيادة والسيارة",
            titleEn = "Car / Drive Mode",
            descriptionAr = "تجهيز كل ما يلزم أثناء القيادة بكلمة واحدة",
            descriptionEn = "Setup maps, bluetooth and audio while driving",
            triggerPhrase = "أنا في السيارة",
            alternativeTriggers = listOf("وضع القيادة", "ركبت السيارة", "سواقة", "drive mode"),
            iconName = "car",
            stepsAr = listOf("رفع الصوت لـ 80%", "تشغيل وتفقد البلوتوث", "فتح تطبيق الملاحة والخرائط"),
            actionType = ParsedAction.ROUTINE_CAR
        ),
        CyberRoutine(
            id = "bedtime",
            titleAr = "وضع النوم والهدوء",
            titleEn = "Bedtime / Sleep Mode",
            descriptionAr = "كتم الهاتف وإطفاء الإزعاج للنوم بسلام",
            descriptionEn = "Mute phone, silence alerts, and dim screen",
            triggerPhrase = "وقت النوم",
            alternativeTriggers = listOf("تصبح على خير", "وضع النوم", "بدخل أنام", "sleep mode"),
            iconName = "bedtime",
            stepsAr = listOf("كتم الصوت بالكامل (صامت)", "خفض إضاءة الشاشة", "إيقاف الإشعارات المزعجة"),
            actionType = ParsedAction.ROUTINE_BEDTIME
        ),
        CyberRoutine(
            id = "work",
            titleAr = "وضع العمل والتركيز",
            titleEn = "Work & Focus Mode",
            descriptionAr = "تحويل الهاتف لوضع الصامت مع الاهتزاز والتركيز",
            descriptionEn = "Vibrate only, open notes, and enhance focus",
            triggerPhrase = "أنا في العمل",
            alternativeTriggers = listOf("عندي اجتماع", "وضع العمل", "بدأ الدوام", "work mode"),
            iconName = "work",
            stepsAr = listOf("وضع الهاتف على الاهتزاز", "كتم وسائط الترفيه", "تجهيز تطبيق المفكرة والمهام"),
            actionType = ParsedAction.ROUTINE_WORK
        ),
        CyberRoutine(
            id = "morning",
            titleAr = "صباح الخير وبداية اليوم",
            titleEn = "Morning Routine",
            descriptionAr = "إحاطة كاملة بالوقت والبطارية لبدء يومك بنشاط",
            descriptionEn = "Time, battery level, and moderate audio setup",
            triggerPhrase = "صباح الخير",
            alternativeTriggers = listOf("ابدأ يومي", "بداية اليوم", "صحيت", "good morning"),
            iconName = "morning",
            stepsAr = listOf("قراءة الوقت والتاريخ", "فحص نسبة البطارية", "ضبط الصوت على مستوى معتدل 50%"),
            actionType = ParsedAction.ROUTINE_MORNING
        ),
        CyberRoutine(
            id = "emergency",
            titleAr = "وضع الطوارئ الفوري",
            titleEn = "Emergency Quick Mode",
            descriptionAr = "إنارة كشاف الفلاش ورفع الصوت للحد الأقصى فوراً",
            descriptionEn = "Turn on torch, set volume 100%, and ready dialer",
            triggerPhrase = "وضع الطوارئ",
            alternativeTriggers = listOf("حالة طوارئ", "طوارئ", "emergency mode"),
            iconName = "emergency",
            stepsAr = listOf("تشغيل كشاف الفلاش فوراً", "رفع الصوت إلى 100%", "تجهيز لوحة الاتصال"),
            actionType = ParsedAction.ROUTINE_EMERGENCY
        ),
        CyberRoutine(
            id = "boost",
            titleAr = "تسريع وتبريد الهاتف",
            titleEn = "Performance Boost",
            descriptionAr = "تنظيف الذاكرة المؤقتة وتبريد المعالج لسرعة قصوى",
            descriptionEn = "Clean RAM cache and optimize device speed",
            triggerPhrase = "نظف الهاتف",
            alternativeTriggers = listOf("تبريد الهاتف", "صيانة سريعة", "تفريغ الرام", "boost"),
            iconName = "boost",
            stepsAr = listOf("تفريغ الذاكرة العشوائية RAM", "تبريد المعالج وإغلاق العمليات الزائدة", "فحص كفاءة النظام"),
            actionType = ParsedAction.ROUTINE_BOOST
        ),
        CyberRoutine(
            id = "power_saver",
            titleAr = "وضع توفير الطاقة الفائق",
            titleEn = "Super Power Saver",
            descriptionAr = "حفظ أقصى قدر من طاقة البطارية عند الحاجة بنقرة واحدة",
            descriptionEn = "Conserve maximum battery: torch off, dim screen, minimal audio",
            triggerPhrase = "وفر الطاقة",
            alternativeTriggers = listOf("وضع التوفير", "انقذ البطارية", "توفير الطاقة", "power saver"),
            iconName = "battery_saver",
            stepsAr = listOf("إطفاء كشاف الفلاش فوراً", "خفض الصوت لـ 15% ووضع الصامت", "تجهيز إعدادات السطوع والواي فاي لحفظ الطاقة"),
            actionType = ParsedAction.ROUTINE_POWER_SAVER
        ),
        CyberRoutine(
            id = "anti_theft",
            titleAr = "حارس السرقة وفصل الشاحن",
            titleEn = "Anti-Theft Charger Guard",
            descriptionAr = "حماية الهاتف من التحريك أو سحب كابل الشاحن مع إنذار فوري",
            descriptionEn = "Blasts siren, strobes flashlight and locks screen if moved or unplugged",
            triggerPhrase = "فعل حارس السرقة",
            alternativeTriggers = listOf("حارس السرقة", "حماية الشاحن", "وضع السرقة", "anti theft"),
            iconName = "security",
            stepsAr = listOf("بدء عد تنازلي 5 ثوانٍ لوضع الهاتف", "تفعيل مراقبة كابل الشاحن وحساس الحركة", "إطلاق صفارة إنذار وفلاش الكشاف حال العبث به"),
            actionType = ParsedAction.ROUTINE_ANTI_THEFT
        )
    )

    init {
        initializeEngine()
        refreshTelemetry()
        loadInstalledApps()
        loadDeviceFiles()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        getApplication<Application>().registerReceiver(batteryReceiver, filter)

        log(LogLevel.INFO, "المساعد", "المساعد الصوتي جاهز ومستعد لمساعدتك.")
        log(LogLevel.SUCCESS, "النظام", "الهاتف متصل وكافة الأوامر جاهزة للاستماع.")
        log(LogLevel.INFO, "حارس البطارية", "الحارس الأمني للبطارية يعمل بالخلفية لمراقبة الـ 100% وأقل من 20%.")
        checkNotificationAccess()
    }

    private fun initializeEngine() {
        voiceEngine = VoiceEngine(
            context = getApplication(),
            onCommandReceived = { spokenCommand ->
                _isListening.value = false
                _audioRmsLevel.value = 0f
                _lastSpokenTranscript.value = spokenCommand
                processVoiceCommand(spokenCommand)
            },
            onStateChange = { newState ->
                _assistantState.value = newState
                if (newState == AssistantState.LISTENING) {
                    _isListening.value = true
                } else if (newState == AssistantState.IDLE) {
                    _isListening.value = false
                    _audioRmsLevel.value = 0f
                }
            },
            onErrorOccurred = { errorMsg ->
                _isListening.value = false
                _audioRmsLevel.value = 0f
                _assistantState.value = AssistantState.IDLE
                _lastVoiceResponse.value = errorMsg
                log(LogLevel.WARN, "تنبيه الصوت", errorMsg)
            },
            onPartialReceived = { partialText ->
                _lastSpokenTranscript.value = partialText
            },
            onRequestSystemDialog = {
                _systemSpeechRequest.tryEmit(Unit)
            }
        )

        viewModelScope.launch {
            voiceEngine?.audioRms?.collect { rms ->
                _audioRmsLevel.value = rms
            }
        }
        viewModelScope.launch {
            voiceEngine?.isListening?.collect { listening ->
                _isListening.value = listening
            }
        }
    }

    fun setNavTab(tab: Int) {
        _currentNavTab.value = tab
    }

    fun toggleVoiceLanguage() {
        _isArabicVoice.value = !_isArabicVoice.value
        val lang = if (_isArabicVoice.value) "العربية" else "English"
        log(LogLevel.INFO, "اللغة", "تم تحويل لغة التحدث إلى: $lang")
    }

    fun isDirectSpeechAvailable(): Boolean {
        return voiceEngine?.isRecognitionAvailable() ?: false
    }

    fun setListeningState(listening: Boolean) {
        _isListening.value = listening
        if (listening) {
            _assistantState.value = AssistantState.LISTENING
            log(LogLevel.INFO, "استماع", "المساعد يستمع إليك الآن...")
        } else {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun onVoiceInputFailed(errorMsg: String) {
        _isListening.value = false
        _audioRmsLevel.value = 0f
        _assistantState.value = AssistantState.IDLE
        _lastVoiceResponse.value = errorMsg
        log(LogLevel.WARN, "تنبيه الصوت", errorMsg)
    }

    fun onVoiceInputCancelled() {
        _isListening.value = false
        _audioRmsLevel.value = 0f
        _assistantState.value = AssistantState.IDLE
        log(LogLevel.INFO, "استماع", "تم إيقاف الاستماع.")
    }

    fun startHoldToTalk() {
        _lastSpokenTranscript.value = ""
        _isListening.value = true
        _assistantState.value = AssistantState.LISTENING
        val locale = if (_isArabicVoice.value) Locale.forLanguageTag("ar-SA") else Locale.US
        log(LogLevel.INTENT, "استماع بالضغط", "أستمع لك الآن... استمر بالضغط وتحدث")
        voiceEngine?.startListening(locale)
    }

    fun stopHoldToTalk() {
        if (_isListening.value) {
            log(LogLevel.INFO, "تنفيذ فوري", "رفعت إصبعك! جارٍ تنفيذ الأمر فوراً...")
            voiceEngine?.stopListening()
        }
    }

    fun triggerListening() {
        if (_isListening.value) {
            voiceEngine?.stopListening()
            _isListening.value = false
            _assistantState.value = AssistantState.IDLE
            log(LogLevel.INFO, "استماع", "تم إيقاف الاستماع الصوتي.")
        } else {
            val locale = if (_isArabicVoice.value) Locale.forLanguageTag("ar-SA") else Locale.US
            log(LogLevel.INTENT, "استماع", "أنا أستمع إليك الآن، تحدث بوضوح...")
            voiceEngine?.startListening(locale)
        }
    }

    private fun isBroadQuestion(query: String): Boolean {
        val lower = query.lowercase().trim()
        val questionWords = listOf("ما هو", "ما هي", "من هو", "من هي", "كيف", "لماذا", "ليه", "ليش", "متى", "اين", "اكتب لي", "صغ لي", "انصحني", "ما رايك", "حدثني عن", "قصة", "شرح")
        return questionWords.any { lower.contains(it) }
    }

    fun processVoiceCommand(input: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _lastSpokenTranscript.value = input
            _assistantState.value = AssistantState.PROCESSING
            log(LogLevel.INTENT, "أمر صوتي", "\"$input\"")

            val intentResult = IntentParser.parse(input)

            if (intentResult.subActions.isNotEmpty()) {
                _assistantState.value = AssistantState.EXECUTING
                val summaries = mutableListOf<String>()
                for (sub in intentResult.subActions) {
                    val summary = executeIntent(sub.action, sub.targetParameter, sub.secondaryParameter)
                    if (summary.isNotBlank()) summaries.add(summary)
                }
                val spokenMessage = if (_isArabicVoice.value) {
                    "${intentResult.feedbackMessageAr}${if (summaries.isNotEmpty()) " (${summaries.joinToString("، ")})" else ""} هل تطلب مني شيء آخر يا غالي؟"
                } else {
                    "${intentResult.feedbackMessageEn}${if (summaries.isNotEmpty()) " (${summaries.joinToString(", ")})" else ""} Anything else?"
                }
                _lastVoiceResponse.value = spokenMessage
                log(LogLevel.SUCCESS, "الرد", spokenMessage)
                voiceEngine?.speak(spokenMessage, _isArabicVoice.value)
                refreshTelemetry()
            } else if (com.example.ai.HybridAiBrain.isLocalDeviceAction(intentResult.action)) {
                _assistantState.value = AssistantState.EXECUTING
                val executionSummary = executeIntent(intentResult.action, intentResult.targetParameter, intentResult.secondaryParameter)
                val baseMsg = if (_isArabicVoice.value) {
                    if (executionSummary.isNotBlank()) "${intentResult.feedbackMessageAr} $executionSummary" else intentResult.feedbackMessageAr
                } else {
                    if (executionSummary.isNotBlank()) "${intentResult.feedbackMessageEn} $executionSummary" else intentResult.feedbackMessageEn
                }
                val spokenMessage = if (_isArabicVoice.value && !baseMsg.contains("هل تطلب") && !baseMsg.contains("هل تأمر") && !baseMsg.contains("شيء آخر")) {
                    "$baseMsg هل تطلب مني شيء آخر يا غالي؟"
                } else {
                    baseMsg
                }
                _lastVoiceResponse.value = spokenMessage
                log(LogLevel.SUCCESS, "الرد المحلي", spokenMessage)
                voiceEngine?.speak(spokenMessage, _isArabicVoice.value)
                refreshTelemetry()
            } else if (intentResult.action == ParsedAction.WEB_SEARCH) {
                _assistantState.value = AssistantState.EXECUTING
                val executionSummary = executeIntent(intentResult.action, intentResult.targetParameter, intentResult.secondaryParameter)
                val spokenMessage = "${intentResult.feedbackMessageAr} $executionSummary"
                _lastVoiceResponse.value = spokenMessage
                log(LogLevel.SUCCESS, "بحث قوقل", spokenMessage)
                voiceEngine?.speak(spokenMessage, _isArabicVoice.value)
            } else {
                // If there's an instant conversational response (e.g. math calculation, clock, date)
                if (intentResult.feedbackMessageAr.isNotBlank() && !isBroadQuestion(input)) {
                    _assistantState.value = AssistantState.EXECUTING
                    val baseMsg = intentResult.feedbackMessageAr
                    val spokenMessage = if (_isArabicVoice.value && !baseMsg.contains("هل تطلب") && !baseMsg.contains("هل تأمر") && !baseMsg.contains("شيء آخر")) {
                        "$baseMsg هل تطلب مني شيء آخر يا غالي؟"
                    } else {
                        baseMsg
                    }
                    _lastVoiceResponse.value = spokenMessage
                    log(LogLevel.SUCCESS, "رد فوري", spokenMessage)
                    voiceEngine?.speak(spokenMessage, _isArabicVoice.value)
                } else {
                    // Open-ended conversation, consultation, drafting, or deep reasoning
                    _assistantState.value = AssistantState.PROCESSING
                    _lastVoiceResponse.value = "لحظة من فضلك يا غالي، أجهز لك الرد..."
                    log(LogLevel.INFO, "المساعد الشخصي", "جاري مراجعة وتحليل طلبك...")

                    val deviceCtx = "البطارية ${_batteryLevel.value}% ${if (_isCharging.value) "جار الشحن" else ""}، الحرارة ${_batteryTemp.value}"
                    val geminiAnswer = com.example.ai.HybridAiBrain.generateAnswer(input, deviceCtx)

                    _assistantState.value = AssistantState.EXECUTING
                    _lastVoiceResponse.value = geminiAnswer
                    log(LogLevel.SUCCESS, "المساعد الشخصي", geminiAnswer)
                    voiceEngine?.speak(geminiAnswer, _isArabicVoice.value)
                }
            }
        }
    }

    private fun executeIntent(action: ParsedAction, target: String, secondary: String = ""): String {
        return when (action) {
            ParsedAction.OPEN_APP_SEARCH -> {
                val (success, msg) = systemCommander.searchInApp(target, secondary)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "بحث في التطبيق", msg)
                msg
            }
            ParsedAction.OPEN_APP -> {
                val (success, msg) = systemCommander.launchApp(target)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "فتح تطبيق", msg)
                msg
            }
            ParsedAction.UNINSTALL_APP -> {
                val (success, msg) = systemCommander.uninstallApp(target)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "حذف تطبيق", msg)
                msg
            }
            ParsedAction.INSTALL_APP -> {
                val (success, msg) = systemCommander.installAppInStore(target)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "متجر التطبيقات", msg)
                msg
            }
            ParsedAction.SEARCH_FILE -> {
                _currentNavTab.value = 2 // Switch to Files tab
                loadDeviceFiles(target)
                "بحثت لك في ملفاتك عن: $target"
            }
            ParsedAction.FLASHLIGHT_ON -> {
                val (success, msg) = systemCommander.toggleFlashlight(true)
                _isFlashlightActive.value = systemCommander.isFlashlightActive()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "كشاف الهاتف", msg)
                msg
            }
            ParsedAction.FLASHLIGHT_OFF -> {
                val (success, msg) = systemCommander.toggleFlashlight(false)
                _isFlashlightActive.value = systemCommander.isFlashlightActive()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "كشاف الهاتف", msg)
                msg
            }
            ParsedAction.VOLUME_UP -> {
                val (success, msg) = systemCommander.adjustVolume(1)
                _volumePercent.value = systemCommander.getVolumeLevel()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "صوت الهاتف", msg)
                msg
            }
            ParsedAction.VOLUME_DOWN -> {
                val (success, msg) = systemCommander.adjustVolume(-1)
                _volumePercent.value = systemCommander.getVolumeLevel()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "صوت الهاتف", msg)
                msg
            }
            ParsedAction.VOLUME_MUTE -> {
                val (success, msg) = systemCommander.adjustVolume(0)
                _volumePercent.value = 0
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "كتم الصوت", msg)
                msg
            }
            ParsedAction.OPEN_WIFI -> {
                val (success, msg) = systemCommander.openWifiSettings()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "واي فاي", msg)
                msg
            }
            ParsedAction.OPEN_BLUETOOTH -> {
                val (success, msg) = systemCommander.openBluetoothSettings()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "بلوتوث", msg)
                msg
            }
            ParsedAction.BATTERY_STATUS -> {
                refreshTelemetry()
                "نسبة بطاريتك الآن ${_batteryLevel.value}% وحرارة الجهاز ${_batteryTemp.value} وكل شيء تمام."
            }
            ParsedAction.STEALTH_MODE -> {
                val (success, msg) = systemCommander.setStealthMode()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "وضع صامت", msg)
                msg
            }
            ParsedAction.OPEN_CAMERA -> {
                val (success, msg) = systemCommander.openCamera()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "الكاميرا", msg)
                msg
            }
            ParsedAction.WEB_SEARCH -> {
                val (success, msg) = systemCommander.searchWeb(target)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "بحث قوقل", msg)
                msg
            }
            ParsedAction.ROUTINE_CAR -> {
                com.example.service.SmartMessageResponderService.isDrivingModeActive = true
                com.example.service.SmartMessageResponderService.isWorkModeActive = false
                val (success, msg) = systemCommander.executeCarRoutine()
                _volumePercent.value = 80
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "وضع القيادة", msg)
                msg
            }
            ParsedAction.ROUTINE_BEDTIME -> {
                com.example.service.SmartMessageResponderService.isDrivingModeActive = false
                com.example.service.SmartMessageResponderService.isWorkModeActive = false
                val (success, msg) = systemCommander.executeBedtimeRoutine()
                _volumePercent.value = 0
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "وضع النوم", msg)
                msg
            }
            ParsedAction.ROUTINE_WORK -> {
                com.example.service.SmartMessageResponderService.isWorkModeActive = true
                com.example.service.SmartMessageResponderService.isDrivingModeActive = false
                val (success, msg) = systemCommander.executeWorkRoutine()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "وضع العمل", msg)
                msg
            }
            ParsedAction.ROUTINE_MORNING -> {
                com.example.service.SmartMessageResponderService.isDrivingModeActive = false
                com.example.service.SmartMessageResponderService.isWorkModeActive = false
                systemCommander.setVolumePercent(50)
                _volumePercent.value = 50
                refreshTelemetry()
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                val summary = "الساعة الآن $timeStr ونسبة شحن البطارية ${_batteryLevel.value}%"
                log(LogLevel.EXEC, "صباح الخير", summary)
                summary
            }
            ParsedAction.ROUTINE_EMERGENCY -> {
                val (success, msg) = systemCommander.executeEmergencyRoutine()
                _isFlashlightActive.value = true
                _volumePercent.value = 100
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "طوارئ", msg)
                msg
            }
            ParsedAction.ROUTINE_BOOST -> {
                val (success, msg) = systemCommander.executeBoostRoutine()
                refreshTelemetry()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "تسريع النظام", msg)
                msg
            }
            ParsedAction.ROUTINE_POWER_SAVER -> {
                executePowerSaver()
            }
            ParsedAction.BATTERY_SENTINEL -> {
                refreshTelemetry()
                val statusMsg = "حارس البطارية نشط ومفعل. نسبة الشحن حالياً ${_batteryLevel.value}% ${if (_isCharging.value) "وجار الشحن" else ""}، سأنبهك بصوتي فور اكتمال 100% لتفادي تلفها، وعند وصولها لأقل من 20% لتوصيل الشاحن."
                log(LogLevel.INFO, "حارس البطارية", statusMsg)
                statusMsg
            }
            ParsedAction.REPLY_MESSAGE -> {
                val (success, msg) = com.example.service.SmartMessageResponderService.sendReply(getApplication(), target)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "رد صوتي على الرسالة", msg)
                msg
            }
            ParsedAction.CANCEL_REPLY -> {
                com.example.service.SmartMessageResponderService.activeSession = null
                val msg = "تم، تم تجاهل الرسالة ولم يتم إرسال أي رد."
                log(LogLevel.INFO, "تجاهل الرسالة", msg)
                msg
            }
            ParsedAction.ROUTINE_ANTI_THEFT -> {
                armAntiTheft()
                val msg = "أبشر، جاري بدء العد التنازلي لتفعيل حارس السرقة وفصل الشاحن... ضع الهاتف بثبات."
                log(LogLevel.EXEC, "حارس السرقة", msg)
                msg
            }
            ParsedAction.DISARM_ANTI_THEFT -> {
                if (antiTheftManager.isAlarmTriggered.value) {
                    val msg = "لإيقاف الإنذار الأمني، يرجى إدخال رمز الأمان (PIN) أو استخدام بصمة الإصبع."
                    log(LogLevel.WARN, "إنذار السرقة", msg)
                    msg
                } else {
                    disarmAntiTheftQuietly()
                    val msg = "تم إلغاء تفعيل حارس السرقة بنجاح."
                    log(LogLevel.INFO, "حارس السرقة", msg)
                    msg
                }
            }
            ParsedAction.SAVE_NOTE -> {
                val type = try {
                    NoteType.valueOf(secondary)
                } catch (_: Exception) {
                    NoteType.NOTE
                }
                saveVoiceNote(target, type)
                ""
            }
            ParsedAction.READ_NOTES -> {
                val isTodayOnly = target.contains("اليوم") || secondary.contains("اليوم") || _lastSpokenTranscript.value.contains("اليوم")
                readNotesAloud(isTodayOnly, speakDirectly = false)
            }
            ParsedAction.SEND_WHATSAPP -> {
                val (success, msg) = systemCommander.sendWhatsAppMessage(target, secondary)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "إرسال واتساب", msg)
                msg
            }
            ParsedAction.DEEP_DEVICE_SCAN -> {
                val (success, msg) = systemCommander.performDeepDeviceInspection()
                refreshTelemetry()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "فحص عميق للهاتف", msg)
                msg
            }
            ParsedAction.ADD_EXPENSE -> {
                val amount = target.toDoubleOrNull() ?: 0.0
                val category = try {
                    com.example.model.ExpenseCategory.valueOf(secondary)
                } catch (_: Exception) {
                    com.example.model.ExpenseCategory.GENERAL
                }
                val saved = expenseManager.addExpense(amount, category, category.labelAr)
                val todayTotal = expenseManager.getTodayTotal()
                val todayTotalStr = if (todayTotal % 1.0 == 0.0) todayTotal.toLong().toString() else todayTotal.toString()
                val remaining = expenseManager.getRemainingBudget()
                val remainingStr = if (remaining % 1.0 == 0.0) remaining.toLong().toString() else remaining.toString()

                val msg = "أبشر، سجلت لك ${if (saved.amount % 1.0 == 0.0) saved.amount.toLong() else saved.amount} ريال في ${saved.category.labelAr}. مجموع ما صرفته اليوم أصبح $todayTotalStr ريال، والمتبقي من ميزانيتك لهذا الشهر هو $remainingStr ريال."
                log(LogLevel.EXEC, "تسجيل مصروف", msg)
                msg
            }
            ParsedAction.QUERY_EXPENSES -> {
                val speechSummary = expenseManager.getSpeechSummary(target)
                log(LogLevel.INFO, "استعلام المصاريف", speechSummary)
                speechSummary
            }
            ParsedAction.CLOSE_BACKGROUND_APPS -> {
                val (success, msg) = systemCommander.closeBackgroundAppsAndFreeMemory()
                refreshTelemetry()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "إغلاق التطبيقات", msg)
                msg
            }
            ParsedAction.VIEW_INTRUDERS -> {
                _showIntrudersDialog.value = true
                val count = intruderSecurityManager.intruderRecords.value.size
                val msg = if (count > 0) {
                    "أهلاً بك، فتحت لك سجل الحماية. تم رصد $count محاولة تطفل والتقاط الصور لها."
                } else {
                    "أهلاً بك، سجل الحماية نظيف تماماً ولم يتم رصد أي محاولة تطفل أو لمس لهاتفك."
                }
                log(LogLevel.INFO, "سجل الحماية", msg)
                msg
            }
            ParsedAction.READ_PDF -> {
                val (success, msg) = systemCommander.readAndSummarizePdf(target, secondary)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "قراءة PDF", msg)
                msg
            }
            ParsedAction.CAPTURE_PHOTO -> {
                val (success, msg) = systemCommander.captureCameraPhoto(target)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "التقاط صورة", msg)
                msg
            }
            ParsedAction.AUDIT_SECURITY -> {
                val (success, msg) = systemCommander.auditAppPermissionsAndSnoopers()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "فحص الأمان", msg)
                msg
            }
            ParsedAction.WATER_EJECT -> {
                val (success, msg) = systemCommander.ejectWaterAndDustWithAcousticPulse()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "طرد الماء والغبار", msg)
                msg
            }
            ParsedAction.CLEAN_STORAGE -> {
                val (success, msg) = systemCommander.scanAndCleanStorage()
                refreshTelemetry()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "تنظيف التخزين", msg)
                msg
            }
            ParsedAction.CURRENT_LOCATION -> {
                val (success, msg) = systemCommander.getCurrentGpsLocation()
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "موقعي في الخريطة", msg)
                msg
            }
            ParsedAction.NAVIGATE_TO -> {
                val (success, msg) = systemCommander.navigateToDestination(target)
                log(if (success) LogLevel.EXEC else LogLevel.WARN, "الملاحة والخريطة", msg)
                msg
            }
            ParsedAction.CONVERSATION_CHAT, ParsedAction.TALK_ASSISTANT, ParsedAction.UNKNOWN -> {
                ""
            }
        }
    }

    fun toggleFlashlightDirect() {
        val next = !_isFlashlightActive.value
        val (success, msg) = systemCommander.toggleFlashlight(next)
        _isFlashlightActive.value = systemCommander.isFlashlightActive()
        log(if (success) LogLevel.EXEC else LogLevel.WARN, "DIRECT_TORCH", msg)
    }

    fun adjustVolumeDirect(direction: Int) {
        systemCommander.adjustVolume(direction)
        _volumePercent.value = systemCommander.getVolumeLevel()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = systemCommander.getInstalledApps()
            _installedApps.value = apps
        }
    }

    fun loadDeviceFiles(query: String = "", category: FileCategory = _selectedFileCategory.value) {
        _selectedFileCategory.value = category
        viewModelScope.launch(Dispatchers.IO) {
            val files = systemCommander.searchDeviceFiles(query, category)
            _deviceFiles.value = files
        }
    }

    fun openFileDirect(file: DeviceFile) {
        systemCommander.openFile(file)
        log(LogLevel.EXEC, "FILE_ACCESS", "Opened: ${file.name}")
    }

    fun launchAppDirect(app: InstalledApp) {
        val (success, msg) = systemCommander.launchApp(app.packageName)
        log(if (success) LogLevel.EXEC else LogLevel.WARN, "DIRECT_LAUNCH", msg)
    }

    fun uninstallAppDirect(app: InstalledApp) {
        val (success, msg) = systemCommander.uninstallApp(app.packageName)
        log(if (success) LogLevel.EXEC else LogLevel.WARN, "DIRECT_PURGE", msg)
    }

    fun refreshTelemetry() {
        val (level, isCharging, temp) = systemCommander.getBatteryTelemetry()
        _batteryLevel.value = level
        _isCharging.value = isCharging
        _batteryTemp.value = temp

        val (mem, _) = systemCommander.getMemoryTelemetry()
        _memoryUsage.value = mem
        _volumePercent.value = systemCommander.getVolumeLevel()
    }

    fun clearLogs() {
        _terminalLogs.value = emptyList()
        log(LogLevel.INFO, "CONSOLE", "Terminal logs cleared.")
    }

    fun log(level: LogLevel, tag: String, message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
        val timestamp = sdf.format(Date())
        val newLog = TerminalLog(
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message
        )
        val updated = (_terminalLogs.value + newLog).takeLast(100)
        _terminalLogs.value = updated
    }

    fun toggleFullChargeSentinel(enabled: Boolean) {
        _isFullChargeSentinelEnabled.value = enabled
        log(LogLevel.INFO, "حارس البطارية", if (enabled) "تم تفعيل تنبيه اكتمال الشحن 100%." else "تم تعطيل تنبيه اكتمال الشحن 100%.")
    }

    private fun playAlertSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(getApplication<Application>(), notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            try {
                val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            } catch (_: Exception) {}
        }
    }

    fun toggleLowBatterySentinel(enabled: Boolean) {
        _isLowBatterySentinelEnabled.value = enabled
        log(LogLevel.INFO, "حارس البطارية", if (enabled) "تم تفعيل إنذار انخفاض الشحن أقل من 20%." else "تم تعطيل إنذار انخفاض الشحن.")
    }

    private fun checkBatterySentinelTriggers(pct: Int, charging: Boolean) {
        // 100% Full Charge Alert
        if (charging && pct >= 100) {
            if (_isFullChargeSentinelEnabled.value && !hasAlertedFullCharge) {
                hasAlertedFullCharge = true
                triggerFullChargeAlert()
            }
        } else if (!charging || pct < 98) {
            hasAlertedFullCharge = false
        }

        // Low Battery Alert (< 20%)
        if (!charging && pct <= 20) {
            if (_isLowBatterySentinelEnabled.value && !hasAlertedLowBattery) {
                hasAlertedLowBattery = true
                triggerLowBatteryAlert()
            }
        } else if (charging || pct > 25) {
            hasAlertedLowBattery = false
        }
    }

    fun triggerFullChargeAlert() {
        val messageAr = "تنبيه: جهازك ممتلئ الشحن 100%، يرجى فصل الشاحن الآن للحفاظ على سلامة البطارية وتفادي تلفها."
        val messageEn = "Alert: Your device is 100% fully charged! Please disconnect the charger now to protect your battery."
        val chosen = if (_isArabicVoice.value) messageAr else messageEn
        _lastVoiceResponse.value = chosen
        log(LogLevel.WARN, "حارس البطارية", "تنبيه 100%: $chosen")
        playAlertSound()
        voiceEngine?.speak(chosen, _isArabicVoice.value)
        triggerHapticAlert()
    }

    fun triggerLowBatteryAlert() {
        val messageAr = "إنذار: جهازك على وشك النفاد، شحن البطارية أقل من عشرين بالمئة! قم بشحن جهازك الآن وتوصيل الشاحن لتفادي إغلاق الهاتف."
        val messageEn = "Warning: Your device is running out of battery, below 20%! Please charge your device and connect charger now."
        val chosen = if (_isArabicVoice.value) messageAr else messageEn
        _lastVoiceResponse.value = chosen
        log(LogLevel.WARN, "حارس البطارية", "تحذير أقل من 20%: $chosen")
        playAlertSound()
        voiceEngine?.speak(chosen, _isArabicVoice.value)
        triggerHapticAlert()
    }

    fun executePowerSaver(): String {
        val (success, msg) = systemCommander.executePowerSaverRoutine()
        _isFlashlightActive.value = false
        _volumePercent.value = 15
        refreshTelemetry()
        val speech = if (_isArabicVoice.value) {
            "أبشر، فعلت لك وضع توفير الطاقة الفائق: طفيت الفلاش وخفضت الصوت والسطوع لحفظ البطارية."
        } else {
            "Power Saver activated: Torch off, volume reduced, and display dimmed to conserve battery."
        }
        _lastVoiceResponse.value = speech
        voiceEngine?.speak(speech, _isArabicVoice.value)
        log(if (success) LogLevel.EXEC else LogLevel.WARN, "توفير الطاقة", msg)
        return speech
    }

    private fun triggerHapticAlert() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        } catch (_: Exception) {}
    }

    fun checkNotificationAccess() {
        _isNotificationAccessGranted.value = com.example.service.SmartMessageResponderService.isNotificationAccessGranted(getApplication())
    }

    fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {}
    }

    fun toggleMessageVoiceReading(enabled: Boolean) {
        _isMessageVoiceReadingEnabled.value = enabled
        responderPrefs.edit().putBoolean(com.example.service.SmartMessageResponderService.KEY_VOICE_READING_ENABLED, enabled).apply()
        log(LogLevel.INFO, "مجيب الرسائل", if (enabled) "تم تفعيل قراءة الرسائل الواردة والرد الصوتي عليها." else "تم تعطيل قراءة الرسائل الواردة.")
    }

    fun toggleAutoReplyDriving(enabled: Boolean) {
        _isAutoReplyDrivingEnabled.value = enabled
        responderPrefs.edit().putBoolean(com.example.service.SmartMessageResponderService.KEY_AUTO_REPLY_DRIVING, enabled).apply()
        log(LogLevel.INFO, "الرد التلقائي", if (enabled) "تم تفعيل الرد الآلي في وضع القيادة." else "تم تعطيل الرد الآلي في وضع القيادة.")
    }

    fun toggleAutoReplyWork(enabled: Boolean) {
        _isAutoReplyWorkEnabled.value = enabled
        responderPrefs.edit().putBoolean(com.example.service.SmartMessageResponderService.KEY_AUTO_REPLY_WORK, enabled).apply()
        log(LogLevel.INFO, "الرد التلقائي", if (enabled) "تم تفعيل الرد الآلي في وضع العمل." else "تم تعطيل الرد الآلي في وضع العمل.")
    }

    fun simulateIncomingMessageTest() {
        val testAnnouncement = "رسالة جديدة من أحمد: \"أين أنت الآن؟ هل نتقابل؟\". هل تود الرد؟"
        _lastVoiceResponse.value = testAnnouncement
        log(LogLevel.INFO, "تجربة الرسائل", testAnnouncement)
        voiceEngine?.speak(testAnnouncement, _isArabicVoice.value)
    }

    // Anti-Theft & Intruder Methods
    fun armAntiTheft() {
        antiTheftManager.armWithCountdown()
        log(LogLevel.INFO, "حارس السرقة", "بدء العد التنازلي (5 ثوانٍ) لتفعيل حارس السرقة وفصل الشاحن.")
    }

    fun cancelAntiTheftArming() {
        antiTheftManager.cancelCountdown()
        log(LogLevel.INFO, "حارس السرقة", "تم إلغاء تفعيل حارس السرقة.")
    }

    fun disarmAntiTheftQuietly() {
        antiTheftManager.disarmQuietly()
        log(LogLevel.INFO, "حارس السرقة", "تم إيقاف حارس السرقة يدوياً.")
    }

    fun verifyAntiTheftPin(pin: String): Boolean {
        val success = antiTheftManager.verifyPinAndDisarm(pin)
        if (success) {
            log(LogLevel.SUCCESS, "حارس السرقة", "تم فك قفل الإنذار بنجاح برمز الأمان (PIN).")
        } else {
            log(LogLevel.WARN, "حارس السرقة", "محاولة فاشلة لفك قفل الإنذار برمز خاطئ.")
        }
        return success
    }

    fun disarmAntiTheftWithBiometric() {
        antiTheftManager.disarmSuccessfully()
        log(LogLevel.SUCCESS, "حارس السرقة", "تم فك قفل الإنذار بنجاح ببصمة الإصبع.")
    }

    fun testTriggerAntiTheftAlarm() {
        antiTheftManager.triggerAlarm("تجربة يدوية لصفارة إنذار السرقة وفصل الشاحن!")
        log(LogLevel.WARN, "إنذار السرقة", "تم إطلاق صفارة الإنذار وفلاش الكشاف التجريبي.")
    }

    fun updateSecurityPin(newPin: String) {
        antiTheftManager.setSecurityPin(newPin)
        log(LogLevel.INFO, "حارس السرقة", "تم تحديث رمز الأمان بنجاح.")
    }

    fun toggleAntiTheftChargerGuard(enabled: Boolean) {
        antiTheftManager.toggleTriggerOnChargerDisconnect(enabled)
        log(LogLevel.INFO, "حارس السرقة", if (enabled) "تم تفعيل الإنذار عند فصل الشاحن." else "تم تعطيل الإنذار عند فصل الشاحن.")
    }

    fun toggleAntiTheftMotionGuard(enabled: Boolean) {
        antiTheftManager.toggleTriggerOnMotion(enabled)
        log(LogLevel.INFO, "حارس السرقة", if (enabled) "تم تفعيل الإنذار عند تحريك الهاتف." else "تم تعطيل الإنذار عند تحريك الهاتف.")
    }

    fun toggleAntiTheftLockOverlay(enabled: Boolean) {
        antiTheftManager.toggleLockOverlay(enabled)
        log(LogLevel.INFO, "حارس السرقة", if (enabled) "تم تفعيل شاشة القفل التام النيون عند الإنذار." else "تم تعطيل شاشة القفل التام.")
    }

    // Voice Notes & Memos Methods
    fun saveVoiceNote(text: String, type: NoteType = NoteType.NOTE): VoiceNote {
        val note = voiceNotesManager.addNote(text, type)
        log(LogLevel.SUCCESS, "مفكرة الملاحظات", "تم حفظ ${type.labelAr}: \"${note.text}\"")
        return note
    }

    fun deleteVoiceNote(id: String) {
        voiceNotesManager.deleteNote(id)
        log(LogLevel.INFO, "مفكرة الملاحظات", "تم حذف الملاحظة من السجل.")
    }

    fun readNotesAloud(todayOnly: Boolean = false, speakDirectly: Boolean = true): String {
        val summary = voiceNotesManager.getSpeechSummary(todayOnly)
        _lastVoiceResponse.value = summary
        log(LogLevel.INFO, "قراءة الملاحظات", summary)
        if (speakDirectly) {
            voiceEngine?.speak(summary, _isArabicVoice.value)
        }
        return summary
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        antiTheftManager.onDestroy()
        voiceEngine?.destroy()
    }
}
