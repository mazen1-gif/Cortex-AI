package com.example.ai

import android.util.Log
import com.example.model.ParsedAction
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * العقل الهجين (Hybrid AI Brain)
 *
 * - الأوامر المحلية السريعة تبقى على الجهاز كما هي.
 * - أي طلب آخر يذهب إلى Gemini عبر Firebase AI Logic مع "أدوات" حقيقية (Function Calling):
 *   النموذج يقرر أي أداة يستدعي، والتطبيق ينفذها فعلياً ويعيد النتيجة الحقيقية (نجاح/فشل + رسالة)،
 *   ثم يصوغ النموذج الرد الصوتي بناءً على ما حدث فعلاً وليس على ما يُفترض أن يحدث.
 */
object HybridAiBrain {

    private const val TAG = "HybridAiBrain"

    // اسم النموذج الأساسي. إن لم يكن متاحاً في مشروعك يُجرَّب النموذج الاحتياطي تلقائياً.
    private const val PRIMARY_MODEL = "gemini-3.8-flash"
    private const val FALLBACK_MODEL = "gemini-2.5-flash"
    private const val REQUEST_TIMEOUT_MS = 40_000L
    private const val MAX_TOOL_ROUNDS = 3

    @Volatile
    private var activeModelName = PRIMARY_MODEL

    /** نتيجة تنفيذ أداة على الجهاز. */
    data class ToolResult(val success: Boolean, val message: String)

    /** نتيجة جولة الوكيل الكاملة. */
    data class AgentOutcome(
        val text: String,
        val executedAnyTool: Boolean,
        val failed: Boolean
    )

    private val SYSTEM_PROMPT = """
أنت "كورتكس"، مساعد ذكاء اصطناعي يمني أصيل، ذكي، وسريع البديهة. تتحدث باللهجة الصنعانية البيضاء (المفهومة للجميع) بأسلوب بشري راقٍ ودافئ، وكأنك صديق وفي للمستخدم.

قواعد الشخصية والأسلوب (إلزامية):
1. اللهجة: استخدم كلمات مثل "أبشر"، "من عيوني"، "حيا بيك"، "تم يا ذيب"، "ما يهمك"، "عدّي"، "أمرك مطاع". اجعل الجمل قصيرة ومريحة للنطق الصوتي.
2. الكفاءة: عندما يطلب المستخدم فعلاً (فتح تطبيق، رسالة، موقع...) نفذ الأداة فوراً بلا كثرة كلام.
3. الصدق: لا تقل إن الفعل تم إلا إذا نجحت الأداة فعلاً. إذا فشلت، قل "والله ما سبرت" أو "اعتذر منك حصلت مشكلة" مع توضيح السبب بلهجة محلية.
4. الرد المباشر: ابدأ بالنتيجة فوراً. لا تكرر كلام المستخدم ولا ترحب في كل جملة. كن مبتكراً في ردودك، لا تكن آلياً جافاً.
5. الثقافة المحلية: افهم السياق اليمني (المواقع، التطبيقات المشهورة، التوقيت).
6. الذكاء البشري: إذا سألك "من أنت؟" قل "أنا كورتكس، خويك ومساعدك اليماني الذكي في هذا التلفون".
7. التعامل مع الأدوات: اتبع نفس القواعد السابقة في استخدام الأدوات بدقة، لكن بصيغة رد صنعانية فخمة.
"""

    // ---------------------------------------------------------------------
    // الأدوات (تُنفَّذ فعلياً في MainViewModel.executeToolCall)
    // ---------------------------------------------------------------------
    private fun str(desc: String) = Schema.string(desc)

    private val TOOLS: List<Tool> by lazy {
        listOf(
            Tool.functionDeclarations(
                listOf(
                    FunctionDeclaration(
                        "open_app",
                        "يفتح تطبيقاً مثبتاً على هاتف المستخدم.",
                        mapOf("app_name" to str("اسم التطبيق كما نطقه المستخدم"))
                    ),
                    FunctionDeclaration(
                        "search_in_app",
                        "يفتح تطبيقاً (مثل يوتيوب أو الخرائط) ويبحث فيه عن عبارة.",
                        mapOf(
                            "app_name" to str("اسم التطبيق"),
                            "query" to str("عبارة البحث")
                        )
                    ),
                    FunctionDeclaration(
                        "navigate_to",
                        "يفتح خرائط جوجل ويبدأ التوجيه إلى وجهة (شارع، محل، مدينة، عنوان).",
                        mapOf("destination" to str("اسم الوجهة أو عنوانها"))
                    ),
                    FunctionDeclaration(
                        "show_my_location",
                        "يحدد موقع المستخدم الحالي فعلياً عبر GPS، ويفتحه على الخريطة، ويعيد العنوان.",
                        emptyMap()
                    ),
                    FunctionDeclaration(
                        "web_search",
                        "يفتح المتصفح للبحث في جوجل عن عبارة. يُستخدم فقط عندما يطلب المستخدم البحث على الإنترنت.",
                        mapOf("query" to str("عبارة البحث"))
                    ),
                    FunctionDeclaration(
                        "send_whatsapp",
                        "يجهز رسالة واتساب جاهزة للإرسال. لا يرسلها: يبقى على المستخدم الضغط على زر الإرسال.",
                        mapOf(
                            "recipient" to str("رقم الهاتف بالصيغة الدولية، أو اسم جهة الاتصال إن لم يتوفر الرقم"),
                            "message" to str("نص الرسالة")
                        ),
                        optionalParameters = listOf("recipient")
                    ),
                    FunctionDeclaration(
                        "set_flashlight",
                        "يشغل كشاف الهاتف أو يطفئه.",
                        mapOf("on" to Schema.boolean("true للتشغيل و false للإطفاء"))
                    ),
                    FunctionDeclaration(
                        "adjust_volume",
                        "يغير مستوى صوت الوسائط في الهاتف.",
                        mapOf(
                            "direction" to Schema.enumeration(
                                listOf("up", "down", "mute"),
                                "up للرفع، down للخفض، mute للكتم"
                            )
                        )
                    ),
                    FunctionDeclaration(
                        "open_wifi_settings",
                        "يفتح شاشة إعدادات الواي فاي. لا يشغله ولا يطفئه بنفسه.",
                        emptyMap()
                    ),
                    FunctionDeclaration(
                        "open_bluetooth_settings",
                        "يفتح شاشة إعدادات البلوتوث. لا يشغله ولا يطفئه بنفسه.",
                        emptyMap()
                    ),
                    FunctionDeclaration(
                        "list_installed_apps",
                        "يعيد أسماء التطبيقات المثبتة فعلاً على الهاتف. استدعه قبل open_app عندما لا تعرف الاسم الدقيق (مثل: تطبيق البنك، تطبيق التوصيل).",
                        mapOf("filter" to str("جزء من اسم التطبيق للتصفية، اختياري")),
                        optionalParameters = listOf("filter")
                    ),
                    FunctionDeclaration(
                        "read_notifications",
                        "يقرأ الإشعارات الظاهرة الآن في شريط إشعارات الهاتف (الأحدث أولاً). للإشعار الأخير استخدم count=1.",
                        mapOf(
                            "count" to Schema.integer("عدد الإشعارات المطلوبة من 1 إلى 10، والافتراضي 5"),
                            "app_name" to str("اسم تطبيق لتصفية الإشعارات، اختياري")
                        ),
                        optionalParameters = listOf("count", "app_name")
                    ),
                    FunctionDeclaration(
                        "search_files",
                        "يفتح تبويب الملفات ويبحث عن ملفات الصور والفيديو والصوت بالاسم. لا يقرأ مستندات PDF وWord تلقائياً على أندرويد 11 وما بعده.",
                        mapOf("query" to str("جزء من اسم الملف")),
                        optionalParameters = listOf("query")
                    ),
                    FunctionDeclaration(
                        "summarize_pdf",
                        "يفتح منتقي ملفات النظام ليختار المستخدم ملف PDF، ثم يلخصه أو يجيب عن سؤال عنه.",
                        mapOf("question" to str("سؤال محدد عن الملف، اختياري")),
                        optionalParameters = listOf("question")
                    ),
                    FunctionDeclaration(
                        "read_screen",
                        "يقرأ النصوص الظاهرة الآن على شاشة الهاتف داخل أي تطبيق (يتطلب تفعيل خدمة إمكانية الوصول). لا يقرأ كلمات المرور.",
                        emptyMap()
                    ),
                    FunctionDeclaration(
                        "tap_screen_text",
                        "يضغط على عنصر ظاهر على الشاشة مكتوب عليه هذا النص. يطلب تأكيد المستخدم قبل التنفيذ. استدعِ read_screen أولاً لمعرفة النصوص الفعلية.",
                        mapOf("label" to str("النص الظاهر على الزر أو العنصر كما هو على الشاشة"))
                    ),
                    FunctionDeclaration(
                        "type_text",
                        "يكتب نصاً في الحقل النشط حالياً على الشاشة. يطلب تأكيد المستخدم قبل التنفيذ ولا يكتب في حقول كلمات المرور.",
                        mapOf("text" to str("النص المراد كتابته"))
                    ),
                    FunctionDeclaration(
                        "press_system_button",
                        "يضغط زر نظام: رجوع أو الرئيسية أو التطبيقات الأخيرة أو شريط الإشعارات أو الإعدادات السريعة.",
                        mapOf(
                            "button" to Schema.enumeration(
                                listOf("back", "home", "recents", "notifications", "quick_settings"),
                                "الزر المطلوب"
                            )
                        )
                    ),
                    FunctionDeclaration(
                        "scroll_screen",
                        "يمرر المحتوى الظاهر على الشاشة للأسفل أو للأعلى.",
                        mapOf(
                            "direction" to Schema.enumeration(listOf("down", "up"), "down لرؤية المزيد، up للرجوع")
                        )
                    ),
                    FunctionDeclaration(
                        "battery_status",
                        "يقرأ نسبة البطارية وحالة الشحن وحرارتها الآن.",
                        emptyMap()
                    ),
                    FunctionDeclaration(
                        "save_note",
                        "يحفظ ملاحظة نصية للمستخدم داخل التطبيق.",
                        mapOf("text" to str("نص الملاحظة"))
                    ),
                    FunctionDeclaration(
                        "uninstall_app",
                        "يفتح نافذة النظام لحذف تطبيق. الحذف لا يتم إلا بعد تأكيد المستخدم في تلك النافذة.",
                        mapOf("app_name" to str("اسم التطبيق"))
                    ),
                    FunctionDeclaration(
                        "install_app",
                        "يفتح متجر Google Play للبحث عن تطبيق. التثبيت يتم بيد المستخدم.",
                        mapOf("app_name" to str("اسم التطبيق"))
                    ),
                    FunctionDeclaration(
                        "device_report",
                        "يقرأ قياسات حقيقية للجهاز: البطارية والذاكرة والتخزين وعدد التطبيقات.",
                        emptyMap()
                    ),
                    FunctionDeclaration(
                        "permissions_audit",
                        "يحصي التطبيقات المثبتة التي مُنحت أذونات الكاميرا والميكروفون والموقع فعلياً.",
                        emptyMap()
                    ),
                    FunctionDeclaration(
                        "clean_app_cache",
                        "يمسح الملفات المؤقتة الخاصة بتطبيق كورتكس فقط ويعيد المساحة المستعادة فعلياً.",
                        emptyMap()
                    )
                )
            )
        )
    }

    // أدوات نتيجتها نهائية للمستخدم: عند نجاحها نُنطق رسالة التنفيذ الحقيقية فوراً
    // بدون جولة إضافية مع النموذج (أسرع وأدق). الفشل يعود للنموذج ليحاول الإصلاح.
    private val TERMINAL_TOOLS = setOf(
        "open_app", "search_in_app", "navigate_to", "show_my_location", "web_search",
        "send_whatsapp", "set_flashlight", "adjust_volume", "open_wifi_settings",
        "open_bluetooth_settings", "read_notifications", "battery_status", "save_note",
        "uninstall_app", "install_app", "search_files", "summarize_pdf",
        "tap_screen_text", "type_text", "press_system_button", "scroll_screen", "device_report", "permissions_audit", "clean_app_cache"
    )

    private fun buildModel(modelName: String, withTools: Boolean = true): GenerativeModel =
        Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig { temperature = 0.3f },
            systemInstruction = content { text(SYSTEM_PROMPT) },
            tools = if (withTools) TOOLS else null
        )

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
     * يشغّل الوكيل: يرسل طلب المستخدم، وينفذ الأدوات التي يطلبها النموذج فعلياً (حتى 3 جولات)،
     * ثم يعيد الرد النهائي المبني على نتائج التنفيذ الحقيقية.
     *
     * إن فشل الاتصال قبل تنفيذ أي أداة يُجرَّب النموذج الاحتياطي مرة واحدة.
     * وإن فشل بعد تنفيذ أداة، يُعاد وصف ما نُفِّذ فعلاً بصدق.
     */
    suspend fun runAgent(
        userText: String,
        deviceContext: String,
        executeTool: suspend (name: String, args: Map<String, String>) -> ToolResult
    ): AgentOutcome {
        val prompt = if (deviceContext.isNotBlank()) {
            "سياق الجهاز الآن: $deviceContext\nطلب المستخدم: $userText"
        } else userText

        val executed = mutableListOf<String>()

        try {
            return agentLoop(activeModelName, prompt, executeTool, executed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Agent timed out", e)
            return failureOutcome(executed, "استغرق الاتصال وقتاً أطول من المعتاد. أعد الطلب بعد قليل.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Agent failed on model $activeModelName", e)
            if (activeModelName != FALLBACK_MODEL && executed.isEmpty()) {
                try {
                    val outcome = agentLoop(FALLBACK_MODEL, prompt, executeTool, executed)
                    activeModelName = FALLBACK_MODEL
                    return outcome
                } catch (e2: TimeoutCancellationException) {
                    Log.e(TAG, "Fallback model timed out", e2)
                } catch (e2: CancellationException) {
                    throw e2
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback model failed", e2)
                }
            }
            return failureOutcome(executed, "تعذر الاتصال بخدمة الفهم الذكي الآن. تحقق من الإنترنت وأعد المحاولة.")
        }
    }

    private suspend fun agentLoop(
        modelName: String,
        prompt: String,
        executeTool: suspend (String, Map<String, String>) -> ToolResult,
        executed: MutableList<String>
    ): AgentOutcome {
        val chat = buildModel(modelName).startChat()
        var response = withTimeout(REQUEST_TIMEOUT_MS) { chat.sendMessage(prompt) }

        var rounds = 0
        while (rounds < MAX_TOOL_ROUNDS) {
            val calls = response.functionCalls
            if (calls.isEmpty()) break

            val parts = ArrayList<FunctionResponsePart>()
            val roundResults = ArrayList<Pair<String, ToolResult>>()
            for (call in calls) {
                val args = call.args.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() }
                val result = try {
                    executeTool(call.name, args)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ToolResult(false, "حدث خطأ أثناء التنفيذ: ${e.localizedMessage ?: "غير معروف"}")
                }
                if (result.message.isNotBlank()) executed += result.message
                roundResults += call.name to result
                parts += FunctionResponsePart(
                    call.name,
                    JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(result.success),
                            "message" to JsonPrimitive(result.message)
                        )
                    )
                )
            }

            if (roundResults.all { it.second.success && it.first in TERMINAL_TOOLS }) {
                return AgentOutcome(
                    roundResults.joinToString("، ") { it.second.message },
                    executedAnyTool = true,
                    failed = false
                )
            }

            response = withTimeout(REQUEST_TIMEOUT_MS) {
                chat.sendMessage(content("function") { parts.forEach { part(it) } })
            }
            rounds++
        }

        val text = response.text?.trim().orEmpty()
        val finalText = when {
            text.isNotBlank() -> cleanOutputForSpeech(text)
            executed.isNotEmpty() -> executed.joinToString("، ")
            else -> "لم أفهم طلبك بشكل كافٍ. هل تعيده بصياغة أخرى؟"
        }
        return AgentOutcome(finalText, executed.isNotEmpty(), failed = false)
    }

    private fun failureOutcome(executed: List<String>, reason: String): AgentOutcome =
        if (executed.isNotEmpty()) {
            // نُخبر المستخدم بما حدث فعلاً قبل الانقطاع
            AgentOutcome(executed.joinToString("، "), executedAnyTool = true, failed = false)
        } else {
            AgentOutcome(reason, executedAnyTool = false, failed = true)
        }

    /**
     * تلخيص حقيقي لملف PDF عبر Gemini (يُرسل محتوى الملف للخدمة). يعيد null عند الفشل.
     */
    suspend fun summarizePdf(pdfBytes: ByteArray, userQuery: String = ""): String? {
        return try {
            val instruction = if (userQuery.isBlank()) {
                "لخّص هذا الملف بالعربية في خمس جمل كحد أقصى بأسلوب مناسب للنطق الصوتي. اعتمد على محتوى الملف فقط ولا تضف معلومات غير موجودة فيه."
            } else {
                "$userQuery\nأجب بالعربية بإيجاز بأسلوب مناسب للنطق الصوتي، واعتمد على محتوى الملف فقط، وإن لم تجد الجواب فيه فقل ذلك."
            }
            val response = withTimeout(60_000L) {
                buildModel(activeModelName, withTools = false).generateContent(
                    content {
                        inlineData(pdfBytes, "application/pdf")
                        text(instruction)
                    }
                )
            }
            response.text?.trim()?.takeIf { it.isNotBlank() }?.let { cleanOutputForSpeech(it) }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "summarizePdf failed", e)
            null
        }
    }

    /**
     * ردّ نصي بسيط بلا أدوات (للاستخدامات القديمة). الأخطاء تُذكر بصدق.
     */
    suspend fun generateAnswer(prompt: String, deviceContext: String = ""): String {
        return try {
            val fullPrompt = if (deviceContext.isNotBlank()) {
                "سياق الجهاز الآن: $deviceContext\nطلب المستخدم: $prompt"
            } else prompt
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                buildModel(activeModelName, withTools = false).generateContent(fullPrompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) cleanOutputForSpeech(text)
            else "لم يصلني رد من الخدمة هذه المرة، أعد الطلب من فضلك."
        } catch (e: TimeoutCancellationException) {
            "استغرق الاتصال وقتاً أطول من المعتاد، أعد الطلب بعد قليل."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateAnswer failed", e)
            "تعذر الاتصال بالخدمة الآن. تحقق من الإنترنت وأعد المحاولة."
        }
    }

    /** يزيل رموز التنسيق حتى يُنطق الرد بسلاسة. */
    private fun cleanOutputForSpeech(text: String): String =
        text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}.*?`{1,3}"), "")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "، ")
            .trim()
}
