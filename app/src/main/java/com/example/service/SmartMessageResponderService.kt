package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Smart Notification Listener Service that reads incoming messages aloud
 * and sends voice-dictated replies back via official Android RemoteInput APIs.
 */
class SmartMessageResponderService : NotificationListenerService(), TextToSpeech.OnInitListener {

    data class ActiveMessageSession(
        val sender: String,
        val messageText: String,
        val packageName: String,
        val replyAction: Notification.Action,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        const val PREFS_NAME = "cortex_smart_responder_prefs"
        const val KEY_VOICE_READING_ENABLED = "voice_reading_enabled"
        const val KEY_AUTO_REPLY_DRIVING = "auto_reply_driving"
        const val KEY_AUTO_REPLY_WORK = "auto_reply_work"
        const val KEY_DRIVING_MESSAGE = "custom_driving_message"
        const val KEY_WORK_MESSAGE = "custom_work_message"

        const val DEFAULT_DRIVING_MSG = "أنا أقود السيارة حالياً، سأعاود التواصل معك لاحقاً."
        const val DEFAULT_WORK_MSG = "أنا في اجتماع عمل حالياً، سأتواصل معك فور الانتهاء."

        @Volatile
        var activeSession: ActiveMessageSession? = null

        @Volatile
        var isDrivingModeActive = false

        @Volatile
        var isWorkModeActive = false

        private var instance: SmartMessageResponderService? = null

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val myComponent = ComponentName(context, SmartMessageResponderService::class.java).flattenToString()
            return enabledListeners != null && enabledListeners.contains(myComponent)
        }

        fun sendReply(context: Context, replyText: String): Pair<Boolean, String> {
            val session = activeSession
            if (session == null) {
                return Pair(false, "لا توجد رسالة واردة نشطة للرد عليها حالياً.")
            }

            return try {
                val action = session.replyAction
                val remoteInputs = action.remoteInputs ?: return Pair(false, "هذا التطبيق لا يدعم ميزة الرد المباشر.")
                val remoteInput = remoteInputs.firstOrNull() ?: return Pair(false, "تعذر تجهيز حقل الرد.")

                val replyIntent = Intent()
                val bundle = Bundle()
                bundle.putCharSequence(remoteInput.resultKey, replyText)
                RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, bundle)

                action.actionIntent.send(context, 0, replyIntent)
                activeSession = null
                Pair(true, "تم إرسال ردك بنجاح إلى ${session.sender}: \"$replyText\"")
            } catch (e: Exception) {
                Pair(false, "تعذر إرسال الرد: ${e.localizedMessage}")
            }
        }
    }

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onDestroy() {
        instance = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.let { tts ->
                val ar = Locale.forLanguageTag("ar")
                if (tts.isLanguageAvailable(ar) != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = ar
                }
                tts.setPitch(0.98f)
                tts.setSpeechRate(1.02f)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isVoiceEnabled = prefs.getBoolean(KEY_VOICE_READING_ENABLED, false)
        if (!isVoiceEnabled) return

        // Ignore our own notifications
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Skip ongoing persistent notifications (media players, calls, timers)
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        // Target messaging apps
        val messagingPackages = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.thunderdog.challegram",
            "com.google.android.apps.messaging",
            "com.facebook.orca",
            "com.instagram.android",
            "com.viber.voip",
            "org.thoughtcrime.securesms",
            "com.android.mms"
        )

        val isMessagingApp = messagingPackages.contains(sbn.packageName) ||
                notification.category == Notification.CATEGORY_MESSAGE

        if (!isMessagingApp) return

        // Extract title (sender) and text (message)
        val title = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getString(Notification.EXTRA_TEXT)
            ?: ""

        if (title.isBlank() || text.isBlank()) return

        // Find reply Action with RemoteInput
        val actions = notification.actions ?: return
        val replyAction = actions.firstOrNull { it.remoteInputs?.isNotEmpty() == true } ?: return

        // Save active session
        activeSession = ActiveMessageSession(
            sender = title.trim(),
            messageText = text.trim(),
            packageName = sbn.packageName,
            replyAction = replyAction
        )

        // Check auto-reply during Driving Mode
        val autoReplyDriving = prefs.getBoolean(KEY_AUTO_REPLY_DRIVING, true)
        if (isDrivingModeActive && autoReplyDriving) {
            val drivingMsg = prefs.getString(KEY_DRIVING_MESSAGE, DEFAULT_DRIVING_MSG) ?: DEFAULT_DRIVING_MSG
            sendReply(this, drivingMsg)
            speak("وصلتك رسالة من $title ورديت عليه تلقائياً بوضع القيادة.")
            return
        }

        // Check auto-reply during Work Mode
        val autoReplyWork = prefs.getBoolean(KEY_AUTO_REPLY_WORK, false)
        if (isWorkModeActive && autoReplyWork) {
            val workMsg = prefs.getString(KEY_WORK_MESSAGE, DEFAULT_WORK_MSG) ?: DEFAULT_WORK_MSG
            sendReply(this, workMsg)
            speak("وصلتك رسالة من $title ورديت عليه تلقائياً بوضع العمل.")
            return
        }

        // Normal Voice Announcement & Voice Prompt
        val announcement = "رسالة جديدة من $title: \"$text\". هل تود الرد؟"
        speak(announcement)
    }

    private fun speak(message: String) {
        if (isTtsReady) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "smart_msg_${System.currentTimeMillis()}")
        }
    }
}
