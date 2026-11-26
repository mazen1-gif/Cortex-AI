package com.example.engine

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.AssistantState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val onCommandReceived: (String) -> Unit,
    private val onStateChange: (AssistantState) -> Unit,
    private val onErrorOccurred: (String) -> Unit,
    private val onPartialReceived: ((String) -> Unit)? = null,
    var onRequestSystemDialog: (() -> Unit)? = null
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var lastPartialTranscript: String = ""

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    init {
        mainHandler.post {
            initializeSpeechRecognizer()
        }
        initializeTts()
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun initializeSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceEngine)
                }
            }
        } catch (e: Exception) {
            speechRecognizer = null
        }
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.let { tts ->
                try {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts.setAudioAttributes(attrs)
                } catch (_: Exception) {}
                tts.setPitch(1.0f)
                tts.setSpeechRate(1.02f)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onStateChange(AssistantState.SPEAKING)
                    }

                    override fun onDone(utteranceId: String?) {
                        onStateChange(AssistantState.IDLE)
                    }

                    override fun onError(utteranceId: String?) {
                        onStateChange(AssistantState.IDLE)
                    }
                })
            }
        }
    }

    fun startListening(languageLocale: Locale = Locale.getDefault()) {
        stopSpeaking()
        lastPartialTranscript = ""
        vibratePulse(40)
        _isListening.value = true
        onStateChange(AssistantState.LISTENING)

        mainHandler.post {
            if (speechRecognizer == null) {
                initializeSpeechRecognizer()
            }

            if (speechRecognizer == null) {
                _isListening.value = false
                onStateChange(AssistantState.IDLE)
                onErrorOccurred("خدمة التعرف على الصوت المباشرة غير متوفرة على هذا الجهاز، سيتم فتح نافذة الصوت البديلة.")
                onRequestSystemDialog?.invoke()
                return@post
            }

            val langTag = if (languageLocale.language == "ar") "ar-SA" else "en-US"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(
                    "ar", "ar-SA", "ar-EG", "ar-AE", "ar-KW", "ar-QA", "ar-BH", "ar-OM",
                    "ar-YE", "ar-IQ", "ar-SY", "ar-JO", "ar-LB", "ar-PS", "ar-SD", "ar-DZ",
                    "ar-MA", "ar-TN", "ar-LY", "en", "en-US", "en-GB"
                ))
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 600L)
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _isListening.value = false
                onStateChange(AssistantState.IDLE)
                onErrorOccurred("تعذر تشغيل الميكروفون: ${e.localizedMessage}")
                onRequestSystemDialog?.invoke()
            }
        }
    }

    fun stopListening() {
        vibratePulse(25)
        _audioRms.value = 0f
        // Set processing so user sees immediate feedback that execution has started
        onStateChange(AssistantState.PROCESSING)

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                val fallback = lastPartialTranscript.trim()
                if (fallback.isNotBlank()) {
                    onCommandReceived(fallback)
                }
            }
        }
    }

    fun speak(text: String, isArabic: Boolean = true) {
        if (!isTtsInitialized || textToSpeech == null) return
        stopSpeaking()

        val cleanSpoken = sanitizeForNaturalHumanSpeech(text)
        if (cleanSpoken.isBlank()) return

        val locale = if (isArabic || containsArabic(cleanSpoken)) {
            val arLocale = Locale.forLanguageTag("ar")
            val available = textToSpeech?.isLanguageAvailable(arLocale)
            if (available != TextToSpeech.LANG_MISSING_DATA && available != TextToSpeech.LANG_NOT_SUPPORTED) {
                arLocale
            } else {
                Locale.getDefault()
            }
        } else {
            Locale.US
        }

        try {
            textToSpeech?.language = locale

            // Pick highest quality natural human voice
            try {
                val voices = textToSpeech?.voices
                val naturalVoice = voices?.firstOrNull { v ->
                    v.locale.language == locale.language && !v.isNetworkConnectionRequired
                } ?: voices?.firstOrNull { v -> v.locale.language == locale.language }

                if (naturalVoice != null) {
                    textToSpeech?.voice = naturalVoice
                }
            } catch (_: Exception) {}

            textToSpeech?.setSpeechRate(0.96f) // Natural, relaxed human cadence
            textToSpeech?.setPitch(1.0f)

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "cortex_voice_${System.currentTimeMillis()}")
            }
            textToSpeech?.speak(cleanSpoken, TextToSpeech.QUEUE_FLUSH, params, "cortex_voice_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sanitizeForNaturalHumanSpeech(raw: String): String {
        return raw
            // Remove markdown characters, asterisks, brackets, quotes
            .replace(Regex("[*`#_~<>{}\\[\\]\"'«»]"), " ")
            .replace("(", "، ")
            .replace(")", " ")
            // Remove emojis and symbols that cause robotic phonetic stuttering
            .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), " ")
            // Normalize punctuation for smooth breathing pauses
            .replace(Regex("[:;]+"), "، ")
            .replace(Regex("[.!?]{2,}"), ".")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun containsArabic(s: String): Boolean {
        for (c in s) {
            if (c in '\u0600'..'\u06FF' || c in '\u0750'..'\u077F') return true
        }
        return false
    }

    private fun vibratePulse(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        onStateChange(AssistantState.LISTENING)
    }

    override fun onBeginningOfSpeech() {
        lastPartialTranscript = ""
        onStateChange(AssistantState.LISTENING)
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize between 0.0 and 1.0 (typical rmsdB ranges from -2 to 10)
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioRms.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _audioRms.value = 0f
        onStateChange(AssistantState.PROCESSING)
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _audioRms.value = 0f

        val fallback = lastPartialTranscript.trim()
        lastPartialTranscript = ""
        if (fallback.isNotBlank()) {
            // Real words were spoken and captured while holding down! Execute immediately!
            onCommandReceived(fallback)
            return
        }

        onStateChange(AssistantState.IDLE)

        // Reset speech recognizer on client or busy error so subsequent attempts start fresh
        if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            initializeSpeechRecognizer()
        }

        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "لم أسمع أي كلام، استمر بالضغط على الزر وتحدث بوضوح ثم ارفع إصبعك، أو اضغط لفتح نافذة الصوت."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهى وقت الاستماع. استمر بالضغط على الزر وتحدث ثم ارفع إصبعك."
            SpeechRecognizer.ERROR_AUDIO -> "حدث خطأ في التقاط الصوت من الميكروفون."
            SpeechRecognizer.ERROR_CLIENT -> "الميكروفون جاهز، استمر بالضغط وتحدث بوضوح."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يرجى منح إذن الميكروفون لكي يستمع المساعد لصوتك."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "يرجى التحقق من اتصال الإنترنت لخدمة الصوت."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "الميكروفون مشغول في عملية أخرى، جرب مرة ثانية."
            SpeechRecognizer.ERROR_SERVER -> "خادم تحويل الصوت مؤقتاً غير متاح، جرب مجدداً."
            else -> "لم يتم التقاط الصوت، استمر بالضغط وتحدث ثم ارفع إصبعك."
        }
        onErrorOccurred(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _audioRms.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spokenText = matches?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: lastPartialTranscript.trim().takeIf { it.isNotBlank() }
        lastPartialTranscript = ""

        if (!spokenText.isNullOrBlank()) {
            onCommandReceived(spokenText)
        } else {
            onStateChange(AssistantState.IDLE)
            onErrorOccurred("لم أسمع أي كلمات، حاول التحدث بصوت أوضح.")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partial = matches[0].trim()
            if (partial.isNotBlank()) {
                lastPartialTranscript = partial
                onPartialReceived?.invoke(partial)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
    }
}
