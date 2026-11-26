package com.example.model

import android.graphics.drawable.Drawable

enum class AssistantState(val labelAr: String, val labelEn: String) {
    IDLE("جاهز ومستعد لمساعدتك", "Ready to help you"),
    LISTENING("أسمعك الآن، تفضل...", "Listening to you..."),
    PROCESSING("ثواني، جاري فهم طلبك...", "Thinking..."),
    SPEAKING("أتحدث معك الآن...", "Speaking..."),
    EXECUTING("جاري تنفيذ طلبك الآن...", "Working on your request..."),
    ERROR("عذراً، تعذر إكمال الطلب", "Couldn't complete request")
}

enum class LogLevel {
    INFO,
    INTENT,
    EXEC,
    SUCCESS,
    WARN,
    ERROR
}

data class TerminalLog(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val icon: Drawable? = null
)

enum class FileCategory(val labelAr: String, val labelEn: String) {
    ALL("الكل", "ALL"),
    PDF("ملفات PDF", "PDF"),
    DOCUMENTS("مستندات", "DOCS"),
    MEDIA("صور وفيديو", "MEDIA"),
    AUDIO("صوتيات", "AUDIO"),
    DOWNLOADS("التنزيلات", "DOWNLOADS")
}

data class DeviceFile(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val formattedSize: String,
    val dateModified: Long,
    val mimeType: String,
    val category: FileCategory
)

data class CommandPrompt(
    val categoryAr: String,
    val categoryEn: String,
    val commandAr: String,
    val commandEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val iconName: String
)

enum class NoteType(val labelAr: String, val labelEn: String, val emoji: String) {
    NOTE("ملاحظة", "Note", "📝"),
    IDEA("فكرة", "Idea", "💡"),
    TASK("مهمة", "Task", "⚡")
}

data class VoiceNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val type: NoteType = NoteType.NOTE,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String,
    val formattedTime: String
)

enum class ParsedAction {
    OPEN_APP,
    OPEN_APP_SEARCH,
    UNINSTALL_APP,
    INSTALL_APP,
    SEARCH_FILE,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    OPEN_WIFI,
    OPEN_BLUETOOTH,
    BATTERY_STATUS,
    STEALTH_MODE,
    OPEN_CAMERA,
    WEB_SEARCH,
    TALK_ASSISTANT,
    ROUTINE_CAR,
    ROUTINE_BEDTIME,
    ROUTINE_WORK,
    ROUTINE_MORNING,
    ROUTINE_EMERGENCY,
    ROUTINE_BOOST,
    ROUTINE_POWER_SAVER,
    BATTERY_SENTINEL,
    REPLY_MESSAGE,
    CANCEL_REPLY,
    ROUTINE_ANTI_THEFT,
    DISARM_ANTI_THEFT,
    SAVE_NOTE,
    READ_NOTES,
    SEND_WHATSAPP,
    DEEP_DEVICE_SCAN,
    ADD_EXPENSE,
    QUERY_EXPENSES,
    CLOSE_BACKGROUND_APPS,
    VIEW_INTRUDERS,
    READ_PDF,
    CAPTURE_PHOTO,
    AUDIT_SECURITY,
    WATER_EJECT,
    CLEAN_STORAGE,
    NAVIGATE_TO,
    CURRENT_LOCATION,
    CONVERSATION_CHAT,
    UNKNOWN
}

enum class ExpenseCategory(val labelAr: String, val labelEn: String, val emoji: String) {
    SHOPPING("تسوق", "Shopping", "🛍️"),
    FUEL("بنزين ووقود", "Fuel", "⛽"),
    FOOD("طعام ومطاعم", "Food & Dining", "🍽️"),
    BILLS("فواتير والتزامات", "Bills", "📄"),
    GROCERY("بقالة وتموينات", "Grocery", "🛒"),
    HEALTH("صحة وعلاج", "Health", "💊"),
    ENTERTAINMENT("ترفيه", "Entertainment", "🎮"),
    GENERAL("مصروف عام", "General", "💵")
}

data class VoiceExpense(
    val id: String = java.util.UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String = "ريال",
    val category: ExpenseCategory = ExpenseCategory.GENERAL,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String,
    val formattedTime: String
)

data class IntentResult(
    val action: ParsedAction,
    val rawText: String,
    val targetParameter: String = "",
    val secondaryParameter: String = "",
    val feedbackMessageAr: String,
    val feedbackMessageEn: String,
    val confidence: Float = 0.95f,
    val subActions: List<IntentResult> = emptyList()
)

data class CyberRoutine(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val triggerPhrase: String,
    val alternativeTriggers: List<String>,
    val iconName: String,
    val stepsAr: List<String>,
    val actionType: ParsedAction
)

data class IntruderRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val reasonAr: String,
    val photoPath: String,
    val formattedDate: String,
    val formattedTime: String
)
