package com.example.ai

import android.util.Log
import com.example.model.ParsedAction
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Hybrid AI Brain (العقل الهجين)
 * Combines ultra-fast local hardware/app operations with frontier cloud AI (Gemini 3.8 Flash via Firebase AI Logic)
 * for open-ended knowledge, drafting, consultation, and natural conversational dialogue.
 */
object HybridAiBrain {

    private const val TAG = "HybridAiBrain"

    private const val SYSTEM_PROMPT = """
أنت رفيق ومساعد شخصي إنساني حقيقي، حكيم وودود وأصيل جداً.
تتحدث مع المستخدم كإنسان حقيقي: صديق ومستشار مخلص ومحترف، يفهم مشاعره ولهجته وسياقه، ولا يتكلم بنبرة روبوتية أو مصطنعة إطلاقاً.
قاعدة صارمة ومطلقة: لا تذكر إطلاقاً كلمات مثل "ذكاء اصطناعي" أو "الذكي" أو "Gemini" أو "نموذج لغوي" أو "برمجي" أو "خوارزمية" أو أي إشارات تقنية توحي بأنك ذكاء اصطناعي.
إذا سألك أي سؤال فكري، علمي، ثقافي، أو استشارة شخصية، أو طلب صياغة رسالة، إيميل، أو نقاشاً عميقاً:
- أجب بفصاحة وبلاغة عربية عميقة وطبيعية تأسر السامع وتريحه.
- تحدث بطريقة بشرية تلقائية ودافئة ومناسبة للاستماع الصوتي المباشر.
- لا تستخدم جداول أو رموزاً معقدة أو علامات نجمية أو تنسيقات برمجية.
- اختم دائماً بنبرة خدومة دافئة: "هل تطلب مني شيء آخر يا غالي؟" أو "أنا بخدمتك دائماً، هل تأمرني بأي شيء يا عزيزي؟".
"""

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(SYSTEM_PROMPT) }
        )
    }

    /**
     * Determines whether the parsed user intent represents a purely local device control action
     * that must run on-device in milliseconds without network.
     */
    fun isLocalDeviceAction(action: ParsedAction): Boolean {
        return when (action) {
            ParsedAction.FLASHLIGHT_ON,
            ParsedAction.FLASHLIGHT_OFF,
            ParsedAction.VOLUME_UP,
            ParsedAction.VOLUME_DOWN,
            ParsedAction.VOLUME_MUTE,
            ParsedAction.OPEN_APP,
            ParsedAction.UNINSTALL_APP,
            ParsedAction.INSTALL_APP,
            ParsedAction.OPEN_APP_SEARCH,
            ParsedAction.SEARCH_FILE,
            ParsedAction.OPEN_WIFI,
            ParsedAction.OPEN_BLUETOOTH,
            ParsedAction.OPEN_CAMERA,
            ParsedAction.BATTERY_STATUS,
            ParsedAction.BATTERY_SENTINEL,
            ParsedAction.STEALTH_MODE,
            ParsedAction.ROUTINE_CAR,
            ParsedAction.ROUTINE_BEDTIME,
            ParsedAction.ROUTINE_WORK,
            ParsedAction.ROUTINE_MORNING,
            ParsedAction.ROUTINE_EMERGENCY,
            ParsedAction.ROUTINE_BOOST,
            ParsedAction.ROUTINE_POWER_SAVER,
            ParsedAction.ROUTINE_ANTI_THEFT,
            ParsedAction.DISARM_ANTI_THEFT,
            ParsedAction.SAVE_NOTE,
            ParsedAction.READ_NOTES,
            ParsedAction.SEND_WHATSAPP,
            ParsedAction.DEEP_DEVICE_SCAN,
            ParsedAction.ADD_EXPENSE,
            ParsedAction.QUERY_EXPENSES,
            ParsedAction.CLOSE_BACKGROUND_APPS,
            ParsedAction.VIEW_INTRUDERS,
            ParsedAction.READ_PDF,
            ParsedAction.CAPTURE_PHOTO,
            ParsedAction.AUDIT_SECURITY,
            ParsedAction.WATER_EJECT,
            ParsedAction.CLEAN_STORAGE,
            ParsedAction.NAVIGATE_TO,
            ParsedAction.CURRENT_LOCATION,
            ParsedAction.REPLY_MESSAGE,
            ParsedAction.CANCEL_REPLY -> true

            ParsedAction.WEB_SEARCH,
            ParsedAction.CONVERSATION_CHAT,
            ParsedAction.TALK_ASSISTANT,
            ParsedAction.UNKNOWN -> false
        }
    }

    /**
     * Checks if the user request is an explicit web browser search (e.g., "ابحث في قوقل عن كذا")
     */
    fun isExplicitWebSearch(rawText: String): Boolean {
        val lower = rawText.lowercase().trim()
        val webTriggers = listOf(
            "ابحث في قوقل", "ابحث في جوجل", "جوجل", "قوقل", "سوي بحث في قوقل", "دور في قوقل",
            "ابحث بالمتصفح", "افتح قوقل", "google search", "search google"
        )
        return webTriggers.any { lower.contains(it) }
    }

    /**
     * Generates an intelligent, human-like AI response via Firebase AI Logic and Gemini 3.8 Flash.
     */
    suspend fun generateAnswer(prompt: String, deviceContext: String = ""): String {
        return try {
            val fullPrompt = if (deviceContext.isNotBlank()) {
                "سياق حالة الجهاز الحالية للمستخدم: $deviceContext\nسؤال أو طلب المستخدم: $prompt"
            } else {
                prompt
            }

            val response = withTimeout(40_000L) {
                generativeModel.generateContent(fullPrompt)
            }

            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                cleanOutputForSpeech(text)
            } else {
                "أهلاً بك يا عزيزي، فهمت طلبك وسأظل بخدمتك دائماً. هل تطلب مني شيء آخر يا غالي؟"
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini generation timed out", e)
            "عذراً يا عزيزي، استغرق التفكير وقتاً أطول من المعتاد بسبب بطء الاتصال، يمكنك إعادة سؤالك وسأجيبك فوراً. هل تأمرني بشيء آخر؟"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic generation failed", e)
            "أهلاً بك يا غالي! أنا أستمع إليك وأفهم كل ما تطلبه مني. هل تطلب مني فتح أي تطبيق، أو فحص هاتفك، أو تفعيل الكشاف، أو كتابة رسالة واتساب؟ أنا بخدمتك دائماً."
        }
    }

    /**
     * Cleans markdown formatting symbols like asterisks and backticks so TTS reads smoothly.
     */
    private fun cleanOutputForSpeech(text: String): String {
        var clean = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // remove bold markdown
            .replace(Regex("\\*(.*?)\\*"), "$1")     // remove italic markdown
            .replace(Regex("`{1,3}.*?`{1,3}"), "")  // remove inline code fences
            .replace(Regex("#+\\s*"), "")           // remove headings
            .replace(Regex("[-*]\\s+"), "، ")      // bullets to commas
            .trim()

        if (!clean.contains("هل تطلب") && !clean.contains("هل تأمر") && !clean.contains("شيء آخر")) {
            clean += " هل تطلب مني شيء آخر يا غالي؟"
        }
        return clean
    }
}
