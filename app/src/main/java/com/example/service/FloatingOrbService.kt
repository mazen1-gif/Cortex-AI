package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ai.HybridAiBrain
import com.example.engine.IntentParser
import com.example.engine.SystemCommander
import com.example.model.ParsedAction
import com.example.ui.components.FloatingOrbView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class FloatingOrbService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "cortex_floating_orb_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_STOP_ORB = "com.example.service.ACTION_STOP_ORB"
        const val ACTION_START_LISTENING = "com.example.service.ACTION_START_LISTENING"

        @Volatile
        var isServiceActive = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, FloatingOrbService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOrbService::class.java)
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var orbView: FloatingOrbView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var systemCommander: SystemCommander
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    private var hasAlertedFullCharge = false
    private var hasAlertedLowBattery = false

    private fun playAlertChime() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            try {
                val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            } catch (_: Exception) {}
        }
    }

    private val orbBatteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (rawLevel != -1 && scale != -1) (rawLevel * 100 / scale) else -1
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                    // 100% Full Charge Autonomous Voice Alert
                    if (charging && pct >= 100 && !hasAlertedFullCharge) {
                        hasAlertedFullCharge = true
                        playAlertChime()
                        vibrate(500)
                        speak("تنبيه: جهازك ممتلئ الشحن 100%، يرجى فصل الشاحن الآن للحفاظ على سلامة البطارية وتفادي تلفها.")
                    } else if (!charging || pct < 98) {
                        hasAlertedFullCharge = false
                    }

                    // Low Battery Warning (< 20%)
                    if (!charging && pct in 1..20 && !hasAlertedLowBattery) {
                        hasAlertedLowBattery = true
                        playAlertChime()
                        vibrate(700)
                        speak("إنذار: جهازك على وشك النفاد، شحن البطارية أقل من عشرين بالمئة! قم بشحن جهازك الآن وتوصيل الشاحن لتفادي إغلاق الهاتف.")
                    } else if (charging || pct > 25) {
                        hasAlertedLowBattery = false
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    hasAlertedFullCharge = false
                    speak("تم فصل كابل الشاحن.")
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    hasAlertedLowBattery = false
                    playAlertChime()
                    speak("تم توصيل الشاحن، جاري شحن جهازك وحماية البطارية.")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        systemCommander = SystemCommander(applicationContext)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        initializeTts()
        initializeSpeechRecognizer()
        setupFloatingWindow()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(orbBatteryReceiver, filter)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cortex AI - رفيق الشاشة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "خدمة رفيق الشاشة الصوتي المساعد فوق التطبيقات"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingOrbService::class.java).apply {
            action = ACTION_STOP_ORB
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val listenIntent = Intent(this, FloatingOrbService::class.java).apply {
            action = ACTION_START_LISTENING
        }
        val pendingListen = PendingIntent.getService(
            this,
            2,
            listenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("رفيق الشاشة نشط")
            .setContentText("رفيقك معك فوق كل التطبيقات، اضغط وتحدث بأي أمر")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_btn_speak_now, "🎙️ تحدث الآن", pendingListen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ORB) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_START_LISTENING) {
            startListening()
            return START_STICKY
        }
        return START_STICKY
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.let { tts ->
                try {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts.setAudioAttributes(attrs)
                } catch (_: Exception) {}
                val arLocale = Locale.forLanguageTag("ar")
                val available = tts.isLanguageAvailable(arLocale)
                if (available != TextToSpeech.LANG_MISSING_DATA && available != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = arLocale
                } else {
                    tts.language = Locale.getDefault()
                }
                tts.setPitch(1.0f)
                tts.setSpeechRate(1.02f)
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                        setRecognitionListener(this@FloatingOrbService)
                    }
                }
            } catch (e: Exception) {
                speechRecognizer = null
            }
        }
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        orbView = FloatingOrbView(this).apply {
            onOrbClick = {
                toggleListening()
            }
            onOrbClose = {
                stopSelf()
            }
            onOpenMainActivity = {
                val intent = Intent(this@FloatingOrbService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
            onDragDelta = { dx, dy ->
                windowParams?.let { params ->
                    params.x += dx.toInt()
                    params.y += dy.toInt()
                    windowManager?.updateViewLayout(this@apply, params)
                }
            }
            onDragEnd = {
                snapToNearestScreenEdge()
            }
        }

        try {
            windowManager?.addView(orbView, windowParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun snapToNearestScreenEdge() {
        val wm = windowManager ?: return
        val view = orbView ?: return
        val params = windowParams ?: return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val currentX = params.x

        params.x = if (currentX + (view.width / 2) < screenWidth / 2) {
            20 // Snap left
        } else {
            screenWidth - view.width - 20 // Snap right
        }

        try {
            wm.updateViewLayout(view, params)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        vibrate(30)
        isListening = true
        orbView?.setListeningState(true)
        orbView?.showStatusBadge("أسمعك الآن، تفضل بأمرك...")

        mainHandler.post {
            if (speechRecognizer == null) {
                initializeSpeechRecognizer()
            }

            if (speechRecognizer == null) {
                isListening = false
                orbView?.setListeningState(false)
                speak("خدمة التعرف الصوتي غير متوفرة حالياً")
                return@post
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(
                    "ar", "ar-SA", "ar-EG", "ar-AE", "ar-KW", "ar-QA", "ar-BH", "ar-OM",
                    "ar-YE", "ar-IQ", "ar-SY", "ar-JO", "ar-LB", "ar-PS", "ar-SD", "ar-DZ",
                    "ar-MA", "ar-TN", "ar-LY", "en", "en-US", "en-GB"
                ))
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                orbView?.setListeningState(false)
            }
        }
    }

    private fun stopListening() {
        isListening = false
        orbView?.setListeningState(false)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // SpeechRecognizer Callbacks
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {
        val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        orbView?.updateRms(norm)
    }
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        orbView?.setListeningState(false)
        orbView?.showStatusBadge("جاري الفهم والتنفيذ...")
    }

    override fun onError(error: Int) {
        isListening = false
        orbView?.setListeningState(false)
        orbView?.updateRms(0f)
        orbView?.showStatusBadge("عذراً، لم أسمعك بوضوح")
        mainHandler.postDelayed({
            orbView?.hideStatusBadge()
        }, 2500)
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        orbView?.setListeningState(false)
        orbView?.updateRms(0f)

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val recognized = matches?.firstOrNull()?.trim()

        if (!recognized.isNullOrBlank()) {
            handleVoiceCommand(recognized)
        } else {
            orbView?.showStatusBadge("لم يتم التقاط أي كلام")
            mainHandler.postDelayed({
                orbView?.hideStatusBadge()
            }, 2500)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()?.trim()
        if (!partial.isNullOrBlank()) {
            orbView?.showStatusBadge(partial)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun isBroadQuestion(query: String): Boolean {
        val lower = query.lowercase().trim()
        val questionWords = listOf("ما هو", "ما هي", "من هو", "من هي", "كيف", "لماذا", "ليه", "ليش", "متى", "اين", "اكتب لي", "صغ لي", "انصحني", "ما رايك", "حدثني عن", "قصة", "شرح")
        return questionWords.any { lower.contains(it) }
    }

    private fun handleVoiceCommand(query: String) {
        val parsed = IntentParser.parse(query)
        if (HybridAiBrain.isLocalDeviceAction(parsed.action) || parsed.subActions.isNotEmpty() || parsed.action == ParsedAction.WEB_SEARCH) {
            val spokenFeedback = executeParsedIntent(parsed)
            val finalSpoken = if (!spokenFeedback.contains("هل تطلب") && !spokenFeedback.contains("هل تأمر") && !spokenFeedback.contains("شيء آخر")) {
                "$spokenFeedback هل تطلب مني شيء آخر يا غالي؟"
            } else {
                spokenFeedback
            }
            orbView?.showStatusBadge(finalSpoken)
            speak(finalSpoken)
            mainHandler.postDelayed({
                orbView?.hideStatusBadge()
            }, 5500)
        } else if (parsed.feedbackMessageAr.isNotBlank() && !isBroadQuestion(query)) {
            val spokenFeedback = parsed.feedbackMessageAr
            val finalSpoken = if (!spokenFeedback.contains("هل تطلب") && !spokenFeedback.contains("هل تأمر") && !spokenFeedback.contains("شيء آخر")) {
                "$spokenFeedback هل تطلب مني شيء آخر يا غالي؟"
            } else {
                spokenFeedback
            }
            orbView?.showStatusBadge(finalSpoken)
            speak(finalSpoken)
            mainHandler.postDelayed({
                orbView?.hideStatusBadge()
            }, 5500)
        } else {
            // General query / reasoning
            orbView?.showStatusBadge("لحظة من فضلك، أجهز لك الرد...")
            CoroutineScope(Dispatchers.IO).launch {
                val telemetry = systemCommander.getBatteryTelemetry()
                val deviceCtx = "البطارية ${telemetry.first}%, الحرارة ${telemetry.third}"
                val aiAnswer = HybridAiBrain.generateAnswer(query, deviceCtx)
                mainHandler.post {
                    orbView?.showStatusBadge(aiAnswer)
                    speak(aiAnswer)
                    mainHandler.postDelayed({
                        orbView?.hideStatusBadge()
                    }, 7000)
                }
            }
        }
    }

    private fun executeParsedIntent(result: com.example.model.IntentResult): String {
        return when (result.action) {
            ParsedAction.OPEN_APP_SEARCH -> {
                val (success, msg) = systemCommander.searchInApp(result.targetParameter, result.secondaryParameter)
                msg
            }
            ParsedAction.OPEN_APP -> {
                val (success, msg) = systemCommander.launchApp(result.targetParameter)
                msg
            }
            ParsedAction.UNINSTALL_APP -> {
                val (success, msg) = systemCommander.uninstallApp(result.targetParameter)
                msg
            }
            ParsedAction.INSTALL_APP -> {
                val (success, msg) = systemCommander.installAppInStore(result.targetParameter)
                msg
            }
            ParsedAction.FLASHLIGHT_ON -> {
                systemCommander.toggleFlashlight(true)
                result.feedbackMessageAr
            }
            ParsedAction.FLASHLIGHT_OFF -> {
                systemCommander.toggleFlashlight(false)
                result.feedbackMessageAr
            }
            ParsedAction.VOLUME_UP -> {
                systemCommander.adjustVolume(1)
                result.feedbackMessageAr
            }
            ParsedAction.VOLUME_DOWN -> {
                systemCommander.adjustVolume(-1)
                result.feedbackMessageAr
            }
            ParsedAction.VOLUME_MUTE -> {
                systemCommander.adjustVolume(0)
                result.feedbackMessageAr
            }
            ParsedAction.OPEN_WIFI -> {
                systemCommander.openWifiSettings()
                result.feedbackMessageAr
            }
            ParsedAction.OPEN_BLUETOOTH -> {
                systemCommander.openBluetoothSettings()
                result.feedbackMessageAr
            }
            ParsedAction.BATTERY_STATUS -> {
                val telemetry = systemCommander.getBatteryTelemetry()
                "نسبة بطاريتك الآن ${telemetry.first}% وحرارة الهاتف ${telemetry.third} وكل شيء ممتاز."
            }
            ParsedAction.STEALTH_MODE -> {
                systemCommander.setStealthMode()
                result.feedbackMessageAr
            }
            ParsedAction.OPEN_CAMERA -> {
                systemCommander.openCamera()
                result.feedbackMessageAr
            }
            ParsedAction.WEB_SEARCH -> {
                systemCommander.searchWeb(result.targetParameter)
                result.feedbackMessageAr
            }
            ParsedAction.ROUTINE_CAR -> {
                SmartMessageResponderService.isDrivingModeActive = true
                SmartMessageResponderService.isWorkModeActive = false
                systemCommander.executeCarRoutine()
                result.feedbackMessageAr
            }
            ParsedAction.ROUTINE_BEDTIME -> {
                SmartMessageResponderService.isDrivingModeActive = false
                SmartMessageResponderService.isWorkModeActive = false
                systemCommander.executeBedtimeRoutine()
                result.feedbackMessageAr
            }
            ParsedAction.ROUTINE_WORK -> {
                SmartMessageResponderService.isWorkModeActive = true
                SmartMessageResponderService.isDrivingModeActive = false
                systemCommander.executeWorkRoutine()
                result.feedbackMessageAr
            }
            ParsedAction.ROUTINE_MORNING -> {
                SmartMessageResponderService.isDrivingModeActive = false
                SmartMessageResponderService.isWorkModeActive = false
                systemCommander.setVolumePercent(50)
                val time = java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date())
                val bat = systemCommander.getBatteryTelemetry().first
                "صباح الخير! الساعة $time وبطاريتك $bat%، يومك سعيد يا غالي!"
            }
            ParsedAction.ROUTINE_EMERGENCY -> {
                systemCommander.executeEmergencyRoutine()
                result.feedbackMessageAr
            }
            ParsedAction.ROUTINE_BOOST -> {
                systemCommander.executeBoostRoutine()
                result.feedbackMessageAr
            }
            ParsedAction.ROUTINE_POWER_SAVER -> {
                systemCommander.executePowerSaverRoutine()
                "تم تفعيل وضع توفير الطاقة الفائق: أطفأت الفلاش وخفضت الصوت والسطوع لحفظ البطارية."
            }
            ParsedAction.BATTERY_SENTINEL -> {
                val telemetry = systemCommander.getBatteryTelemetry()
                "حارس البطارية نشط. نسبة الشحن ${telemetry.first}% وحرارة الهاتف ${telemetry.third}. سأنبهك بصوتي عند 100% وعند أقل من 20%."
            }
            ParsedAction.REPLY_MESSAGE -> {
                val (success, msg) = SmartMessageResponderService.sendReply(this, result.targetParameter)
                msg
            }
            ParsedAction.CANCEL_REPLY -> {
                SmartMessageResponderService.activeSession = null
                "تم تجاهل الرسالة ولم يتم إرسال أي رد."
            }
            ParsedAction.ROUTINE_ANTI_THEFT -> {
                try {
                    val appIntent = Intent(this, com.example.MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        putExtra("EXTRA_ARM_ANTI_THEFT", true)
                    }
                    startActivity(appIntent)
                } catch (_: Exception) {}
                "أبشر، جاري فتح حارس السرقة لبدء العد التنازلي وتأمين الهاتف على الشاحن."
            }
            ParsedAction.DISARM_ANTI_THEFT -> {
                "لإيقاف الإنذار وحارس السرقة، يرجى كتابة رمز الأمان (PIN) أو استخدام بصمة الإصبع."
            }
            ParsedAction.SAVE_NOTE -> {
                val type = try {
                    com.example.model.NoteType.valueOf(result.secondaryParameter)
                } catch (_: Exception) {
                    com.example.model.NoteType.NOTE
                }
                val saved = com.example.notes.VoiceNotesManager.getInstance(this).addNote(result.targetParameter, type)
                val typeLabel = when (type) {
                    com.example.model.NoteType.IDEA -> "الفكرة"
                    com.example.model.NoteType.TASK -> "المهمة"
                    com.example.model.NoteType.NOTE -> "الملاحظة"
                }
                "تم حفظ $typeLabel: \"${saved.text}\" في المفكرة بنجاح."
            }
            ParsedAction.READ_NOTES -> {
                com.example.notes.VoiceNotesManager.getInstance(this).getSpeechSummary(false)
            }
            ParsedAction.SEND_WHATSAPP -> {
                val (success, msg) = systemCommander.sendWhatsAppMessage(result.targetParameter, result.secondaryParameter)
                msg
            }
            ParsedAction.DEEP_DEVICE_SCAN -> {
                val (success, msg) = systemCommander.performDeepDeviceInspection()
                msg
            }
            ParsedAction.ADD_EXPENSE -> {
                val amount = result.targetParameter.toDoubleOrNull() ?: 0.0
                val category = try {
                    com.example.model.ExpenseCategory.valueOf(result.secondaryParameter)
                } catch (_: Exception) {
                    com.example.model.ExpenseCategory.GENERAL
                }
                val mgr = com.example.wallet.ExpenseManager.getInstance(this)
                val saved = mgr.addExpense(amount, category, category.labelAr)
                val todayTotal = mgr.getTodayTotal()
                val todayTotalStr = if (todayTotal % 1.0 == 0.0) todayTotal.toLong().toString() else todayTotal.toString()
                val remaining = mgr.getRemainingBudget()
                val remainingStr = if (remaining % 1.0 == 0.0) remaining.toLong().toString() else remaining.toString()

                "أبشر، سجلت لك ${if (saved.amount % 1.0 == 0.0) saved.amount.toLong() else saved.amount} ريال في ${saved.category.labelAr}. مجموع ما صرفته اليوم أصبح $todayTotalStr ريال، والمتبقي من ميزانيتك لهذا الشهر هو $remainingStr ريال."
            }
            ParsedAction.QUERY_EXPENSES -> {
                com.example.wallet.ExpenseManager.getInstance(this).getSpeechSummary(result.targetParameter)
            }
            ParsedAction.CLOSE_BACKGROUND_APPS -> {
                val (_, msg) = systemCommander.closeBackgroundAppsAndFreeMemory()
                msg
            }
            ParsedAction.VIEW_INTRUDERS -> {
                val count = com.example.security.IntruderSecurityManager.getInstance(this).intruderRecords.value.size
                if (count > 0) {
                    "أهلاً بك، تم رصد $count محاولة تطفل على الهاتف، يمكنك فتح التطبيق لمشاهدة الصور فوراً."
                } else {
                    "أهلاً بك، سجل الحماية نظيف تماماً ولم يتم رصد أي محاولة تطفل أو لمس لهاتفك."
                }
            }
            ParsedAction.CONVERSATION_CHAT, ParsedAction.TALK_ASSISTANT, ParsedAction.UNKNOWN -> {
                result.feedbackMessageAr
            }
            else -> {
                result.feedbackMessageAr
            }
        }
    }

    private fun speak(text: String) {
        if (!isTtsReady || textToSpeech == null) return
        try {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "orb_${System.currentTimeMillis()}")
            }
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "orb_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate(duration: Long) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onDestroy() {
        isServiceActive = false
        try {
            unregisterReceiver(orbBatteryReceiver)
        } catch (e: Exception) {
            // ignore
        }
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        if (orbView != null && windowManager != null) {
            try {
                windowManager?.removeView(orbView)
            } catch (e: Exception) {
                // ignore
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
