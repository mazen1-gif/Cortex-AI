package com.example.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.MainActivity
import com.example.engine.VoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * حارس السرقة وفصل الشاحن (Anti-Theft & Intruder Alarm)
 * يحمي الهاتف عند تركه على الشاحن أو على طاولة في مجلس، مكتب أو مكان عام.
 * عند فصل الشاحن أو تحريك الهاتف، يطلق صفارة إنذار عالية جداً، ويومض الفلاش بسرعة قصوى،
 * وينطق تحذيراً صوتياً مستمراً حتى كتابة الرمز أو بصمة الإصبع.
 */
class AntiTheftManager(
    private val context: Context,
    private val voiceEngine: VoiceEngine?
) : SensorEventListener {

    companion object {
        const val PREFS_NAME = "cortex_anti_theft_prefs"
        const val KEY_PIN = "security_pin"
        const val KEY_CHARGER_GUARD_ENABLED = "charger_guard_enabled"
        const val KEY_MOTION_GUARD_ENABLED = "motion_guard_enabled"
        const val KEY_LOCK_OVERLAY_ENABLED = "lock_overlay_enabled"
        const val DEFAULT_PIN = "1234"
        const val MOTION_THRESHOLD = 2.2f // Acceleration change threshold in m/s^2
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // State Flows
    private val _isArmed = MutableStateFlow(false)
    val isArmed: StateFlow<Boolean> = _isArmed.asStateFlow()

    private val _isArmingCountdown = MutableStateFlow(false)
    val isArmingCountdown: StateFlow<Boolean> = _isArmingCountdown.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(5)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _isAlarmTriggered = MutableStateFlow(false)
    val isAlarmTriggered: StateFlow<Boolean> = _isAlarmTriggered.asStateFlow()

    private val _alarmReason = MutableStateFlow("")
    val alarmReason: StateFlow<String> = _alarmReason.asStateFlow()

    private val _triggerOnChargerDisconnect = MutableStateFlow(
        prefs.getBoolean(KEY_CHARGER_GUARD_ENABLED, true)
    )
    val triggerOnChargerDisconnect: StateFlow<Boolean> = _triggerOnChargerDisconnect.asStateFlow()

    private val _triggerOnMotion = MutableStateFlow(
        prefs.getBoolean(KEY_MOTION_GUARD_ENABLED, true)
    )
    val triggerOnMotion: StateFlow<Boolean> = _triggerOnMotion.asStateFlow()

    private val _securityPin = MutableStateFlow(
        prefs.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    )
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    private val _isLockOverlayEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_LOCK_OVERLAY_ENABLED, true)
    )
    val isLockOverlayEnabled: StateFlow<Boolean> = _isLockOverlayEnabled.asStateFlow()

    fun toggleLockOverlay(enabled: Boolean) {
        _isLockOverlayEnabled.value = enabled
        prefs.edit().putBoolean(KEY_LOCK_OVERLAY_ENABLED, enabled).apply()
    }

    // Motion baseline
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    private var isCalibrated = false

    // Alarm jobs
    private var countdownJob: Job? = null
    private var alarmSirenPlayer: MediaPlayer? = null
    private var strobeJob: Job? = null
    private var voiceAlertJob: Job? = null
    private var isReceiverRegistered = false

    // Power Disconnect Receiver
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (!_isArmed.value || _isAlarmTriggered.value) return
            if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
                if (_triggerOnChargerDisconnect.value) {
                    triggerAlarm("تم فصل كابل الشاحن دون إذن!")
                }
            }
        }
    }

    init {
        registerPowerReceiver()
    }

    private fun registerPowerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
            context.registerReceiver(powerReceiver, filter)
            isReceiverRegistered = true
        }
    }

    fun isChargerConnected(): Boolean {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        } catch (_: Exception) {
            false
        }
    }

    /**
     * بدء عملية تفعيل حارس السرقة مع عد تنازلي 5 ثوانٍ لوضع الهاتف على الطاولة
     */
    fun armWithCountdown(onArmComplete: () -> Unit = {}) {
        if (_isArmed.value || _isArmingCountdown.value) return

        _isArmingCountdown.value = true
        _countdownSeconds.value = 5

        voiceEngine?.speak("سيتم تفعيل حارس السرقة خلال خمس ثوانٍ، يرجى وضع هاتفك بثبات.", true)

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in 5 downTo 1) {
                _countdownSeconds.value = i
                delay(1000)
            }
            _isArmingCountdown.value = false
            armDirectly()
            onArmComplete()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _isArmingCountdown.value = false
        _countdownSeconds.value = 5
        voiceEngine?.speak("تم إلغاء تفعيل حارس السرقة.", true)
    }

    /**
     * التفعيل الفوري للحارس
     */
    fun armDirectly() {
        _isArmed.value = true
        _isAlarmTriggered.value = false
        isCalibrated = false

        if (_triggerOnMotion.value && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val speech = "تم تفعيل حارس السرقة وفصل الشاحن بنجاح! سيتم إطلاق صفارة الإنذار فوراً إذا تم تحريك الهاتف أو فصل الشاحن."
        voiceEngine?.speak(speech, true)
    }

    /**
     * إيقاف الحارس يدوياً بدون إنذار
     */
    fun disarmQuietly() {
        _isArmed.value = false
        _isArmingCountdown.value = false
        _isAlarmTriggered.value = false
        sensorManager?.unregisterListener(this)
        stopAllAlarmEffects()
    }

    /**
     * إطلاق صفارة الإنذار وفلاش الكشاف والتحذير الصوتي والتقاط صورة المتلصص
     */
    fun triggerAlarm(reason: String) {
        if (_isAlarmTriggered.value) return

        _isAlarmTriggered.value = true
        _alarmReason.value = reason

        // 0. التقاط صورة المتلصص فوراً وسراً بالكاميرا الأمامية
        try {
            IntruderSecurityManager.getInstance(context).captureIntruderSilent(reason)
        } catch (_: Exception) {}

        // 1. Maximize Volume
        try {
            audioManager?.let { am ->
                val maxAlarm = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
                val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
            }
        } catch (_: Exception) {}

        // 2. Continuous Siren Audio
        startSirenPlayer()

        // 3. Strobe Flashlight
        startFlashlightStrobe()

        // 4. Strong Pulsing Vibration
        startVibration()

        // 5. Looping Loud Voice Alert
        startVoiceAlertLoop(reason)

        // 6. Bring App to Foreground if needed
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra("EXTRA_ALARM_TRIGGERED", true)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun startSirenPlayer() {
        try {
            stopSirenPlayer()
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            alarmSirenPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Fallback handled by voice alerts and tones
        }
    }

    private fun stopSirenPlayer() {
        try {
            alarmSirenPlayer?.stop()
            alarmSirenPlayer?.release()
            alarmSirenPlayer = null
        } catch (_: Exception) {}
    }

    private fun startFlashlightStrobe() {
        strobeJob?.cancel()
        strobeJob = scope.launch {
            try {
                val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return@launch
                var state = false
                while (isActive && _isAlarmTriggered.value) {
                    state = !state
                    try {
                        cameraManager.setTorchMode(cameraId, state)
                    } catch (_: Exception) {}
                    delay(120)
                }
                // Ensure torch is off when done
                try {
                    cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    private fun startVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 400, 150, 400, 150),
                        0 // loop from index 0
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 150, 400, 150), 0)
            }
        } catch (_: Exception) {}
    }

    private fun startVoiceAlertLoop(reason: String) {
        voiceAlertJob?.cancel()
        voiceAlertJob = scope.launch {
            val alertAr = "تحذير أمني! تم تحريك الهاتف وفصل الشاحن دون إذن! أدخل رمز الأمان فوراً!"
            while (isActive && _isAlarmTriggered.value) {
                voiceEngine?.speak(alertAr, true)
                delay(4000)
            }
        }
    }

    private fun stopAllAlarmEffects() {
        stopSirenPlayer()
        strobeJob?.cancel()
        voiceAlertJob?.cancel()
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
        try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (_: Exception) {}
    }

    private var failedPinAttempts = 0

    /**
     * إلغاء الإنذار والتحقق من صحة الرمز السري (PIN)
     */
    fun verifyPinAndDisarm(inputPin: String): Boolean {
        if (inputPin == _securityPin.value) {
            failedPinAttempts = 0
            disarmSuccessfully()
            return true
        } else {
            failedPinAttempts++
            if (failedPinAttempts >= 2) {
                try {
                    IntruderSecurityManager.getInstance(context).captureIntruderSilent(
                        "محاولة إدخال رمز الأمان بشكل خاطئ مرتين متتاليتين"
                    )
                } catch (_: Exception) {}
            }
            // Haptic error buzz
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } catch (_: Exception) {}
            return false
        }
    }

    /**
     * إلغاء الإنذار بنجاح (سواء عبر الرمز أو بصمة الإصبع)
     */
    fun disarmSuccessfully() {
        failedPinAttempts = 0
        stopAllAlarmEffects()
        sensorManager?.unregisterListener(this)
        _isAlarmTriggered.value = false
        _isArmed.value = false
        _alarmReason.value = ""

        val successMsg = "تم إيقاف الإنذار وإلغاء حماية السرقة بنجاح. أهلاً بك."
        voiceEngine?.speak(successMsg, true)
    }

    // Settings Updates
    fun setSecurityPin(newPin: String) {
        if (newPin.length == 4) {
            _securityPin.value = newPin
            prefs.edit().putString(KEY_PIN, newPin).apply()
        }
    }

    fun toggleTriggerOnChargerDisconnect(enabled: Boolean) {
        _triggerOnChargerDisconnect.value = enabled
        prefs.edit().putBoolean(KEY_CHARGER_GUARD_ENABLED, enabled).apply()
    }

    fun toggleTriggerOnMotion(enabled: Boolean) {
        _triggerOnMotion.value = enabled
        prefs.edit().putBoolean(KEY_MOTION_GUARD_ENABLED, enabled).apply()
        if (!enabled && _isArmed.value) {
            sensorManager?.unregisterListener(this)
        } else if (enabled && _isArmed.value && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Accelerometer Sensor Callbacks
    override fun onSensorChanged(event: SensorEvent?) {
        if (!_isArmed.value || _isAlarmTriggered.value || !_triggerOnMotion.value) return
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            if (!isCalibrated) {
                lastAccelX = x
                lastAccelY = y
                lastAccelZ = z
                isCalibrated = true
                return
            }

            val dx = x - lastAccelX
            val dy = y - lastAccelY
            val dz = z - lastAccelZ
            val delta = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

            if (delta > MOTION_THRESHOLD) {
                triggerAlarm("تم تحريك الهاتف من مكانه دون إذن!")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun onDestroy() {
        disarmQuietly()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(powerReceiver)
                isReceiverRegistered = false
            } catch (_: Exception) {}
        }
    }
}
