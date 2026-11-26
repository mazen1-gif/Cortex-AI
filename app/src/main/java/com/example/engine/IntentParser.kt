package com.example.engine

import com.example.model.IntentResult
import com.example.model.ParsedAction
import java.util.Locale

object IntentParser {

    /**
     * Normalizes Arabic text by removing diacritics, unifying hamzas, taa marbuta,
     * and common phonetic variations.
     */
    fun normalize(input: String): String {
        var s = input.trim()
        // Remove Arabic diacritics (tashkeel: fathah, dammah, kasrah, sukun, shaddah, tanween)
        s = s.replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        // Normalize Alefs: أ, إ, آ, ٱ -> ا
        s = s.replace(Regex("[أإآٱ]"), "ا")
        // Normalize Taa Marbuta: ة -> ه
        s = s.replace('ة', 'ه')
        // Normalize Yaa: ى -> ي
        s = s.replace('ى', 'ي')
        // Remove punctuation marks
        s = s.replace(Regex("[،,\\?!\\.\\-_\"'«»]"), " ")
        // Remove duplicate spaces
        s = s.replace(Regex("\\s+"), " ")
        return s.lowercase(Locale.ROOT)
    }

    /**
     * Parses input which may be a single command or a compound sentence
     * containing multiple commands separated by "و" or "ثم" (e.g. "شغل الكشاف واكتم الصوت").
     */
    fun parse(input: String): IntentResult {
        val trimmed = input.trim()
        val normalized = normalize(trimmed)

        // Check for compound command with explicit delimiters
        val explicitDelimiters = listOf(" ثم ", " و ", " وبعدها ", " وبعدين ", " and ")
        for (delimiter in explicitDelimiters) {
            if (trimmed.contains(delimiter, ignoreCase = true)) {
                val parts = trimmed.split(delimiter).map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    val sub1 = parseSingleClause(parts[0])
                    val sub2 = parseSingleClause(parts[1])
                    if (isRealAction(sub1) && isRealAction(sub2)) {
                        return IntentResult(
                            action = sub1.action,
                            rawText = trimmed,
                            targetParameter = sub1.targetParameter,
                            feedbackMessageAr = "${sub1.feedbackMessageAr} وأيضاً ${sub2.feedbackMessageAr}",
                            feedbackMessageEn = "${sub1.feedbackMessageEn} and also ${sub2.feedbackMessageEn}",
                            subActions = listOf(sub1, sub2)
                        )
                    }
                }
            }
        }

        // Check for Arabic attached conjunction " وا..." (e.g., "شغل الكشاف واكتم الصوت")
        val attachedWawRegex = Regex("\\s+و(?=[\\u0600-\\u06FF])")
        val wawMatch = attachedWawRegex.find(trimmed)
        if (wawMatch != null) {
            val part1 = trimmed.substring(0, wawMatch.range.first).trim()
            val part2 = trimmed.substring(wawMatch.range.last + 1).trim()
            if (part1.isNotBlank() && part2.isNotBlank()) {
                val sub1 = parseSingleClause(part1)
                val sub2 = parseSingleClause(part2)
                if (isRealAction(sub1) && isRealAction(sub2)) {
                    return IntentResult(
                        action = sub1.action,
                        rawText = trimmed,
                        targetParameter = sub1.targetParameter,
                        feedbackMessageAr = "${sub1.feedbackMessageAr} وأيضاً ${sub2.feedbackMessageAr}",
                        feedbackMessageEn = "${sub1.feedbackMessageEn} and also ${sub2.feedbackMessageEn}",
                        subActions = listOf(sub1, sub2)
                    )
                }
            }
        }

        return parseSingleClause(trimmed)
    }

    private fun isRealAction(res: IntentResult): Boolean {
        if (res.action == ParsedAction.UNKNOWN) return false
        if (res.action in listOf(ParsedAction.OPEN_APP, ParsedAction.UNINSTALL_APP, ParsedAction.INSTALL_APP, ParsedAction.SEARCH_FILE)) {
            return res.targetParameter.isNotBlank()
        }
        if (res.action == ParsedAction.TALK_ASSISTANT) {
            return res.feedbackMessageAr.isNotBlank()
        }
        return true
    }

    private fun parseSingleClause(text: String): IntentResult {
        val lower = normalize(text)

        // -1. SMART VOICE MESSAGE REPLY & CANCEL (الرد الصوتي وتجاهل الرسائل)
        val isExplicitCancel = lower == "لا" || lower == "لا لا" || lower == "لا شكرا" ||
                matchesAny(lower, listOf("لا ترد", "لا ترسل", "لا تجاوب", "تجاهل", "الغاء", "إلغاء", "الغاء الرد", "ما يحتاج", "cancel reply", "ignore"))
        if (isExplicitCancel) {
            return IntentResult(
                action = ParsedAction.CANCEL_REPLY,
                rawText = text,
                feedbackMessageAr = "تم، تم تجاهل الرسالة ولم يتم إرسال أي رد.",
                feedbackMessageEn = "Ignored message, no reply sent."
            )
        }

        val replyRegex = Regex(
            "^(?:نعم|ايوه|ايه|اجل)?\\s*(?:قل له|قول له|رد عليه|رد|اكتب له|ارسل له|جاوب له|بلغ له|reply|tell him|tell them)\\s*(?:بـ|بان|ان|:\\s*)?\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val replyMatch = replyRegex.find(lower)
        if (replyMatch != null && replyMatch.groupValues[1].isNotBlank()) {
            val replyBody = replyMatch.groupValues[1].trim()
            if (replyBody.isNotBlank()) {
                return IntentResult(
                    action = ParsedAction.REPLY_MESSAGE,
                    rawText = text,
                    targetParameter = replyBody,
                    feedbackMessageAr = "أبشر، جاري إرسال ردك: \"$replyBody\"",
                    feedbackMessageEn = "Sure, sending your reply: \"$replyBody\""
                )
            }
        }

        // 0-EXPENSE. VOICE EXPENSE TRACKER (مسجل المصاريف والمحفظة الصوتية الفورية)
        // A) Querying expenses & budget: "كم صرفت اليوم", "احسب لي مصاريف الاسبوع", "كم باقي من الميزانية"
        val isExpenseQuery = ((lower.contains("صرفت") || lower.contains("مصاريف") || lower.contains("مصروف") || lower.contains("الميزانيه") || lower.contains("المحفظه")) &&
                matchesAny(lower, listOf("كم", "احسب", "ايش", "شو", "مجموع", "باقي", "متبقي", "تقرير", "فحص", "وريني", "اعرض"))) ||
                matchesAny(lower, listOf(
                    "كم صرفت", "مصاريف اليوم", "مصاريف الاسبوع", "مصاريف الشهر", "مجموع المصاريف", "حساب المصاريف", "وضع المحفظه"
                ))
        if (isExpenseQuery) {
            val period = when {
                lower.contains("اسبوع") || lower.contains("أسبوع") -> "week"
                lower.contains("شهر") -> "month"
                else -> "today"
            }
            return IntentResult(
                action = ParsedAction.QUERY_EXPENSES,
                rawText = text,
                targetParameter = period,
                feedbackMessageAr = "أبشر، جاري مراجعة حساباتك ومصاريفك...",
                feedbackMessageEn = "Calculating your expenses summary..."
            )
        }

        // B) Logging/Adding expense: "سجل صرفت 500 ريال تسوق", "حط 100 ريال بنزين", "سجل 50 ريال مطعم", "صرفت 200 ريال بقالة"
        val isExpenseAction = matchesAny(lower, listOf(
            "سجل صرفت", "صرفت", "حط", "سجلت", "سجل مصروف", "دفعت", "سجل دفعت", "حطيت", "اشتريت", "شريت", "خصم", "اضف مصروف"
        ))
        val numberRegex = Regex("(\\d+(?:\\.\\d+)?)")
        val numMatch = numberRegex.find(lower)

        if (isExpenseAction && numMatch != null) {
            val amountStr = numMatch.value
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount > 0.0) {
                val category = when {
                    matchesAny(lower, listOf("بنزين", "وقود", "محطة", "محطه", "بترول", "ديزل", "fuel", "gas")) -> com.example.model.ExpenseCategory.FUEL
                    matchesAny(lower, listOf("تسوق", "ملابس", "سوق", "شوبينق", "shopping", "مول", "ثياب", "احذيه", "اجهزة", "اجهزه")) -> com.example.model.ExpenseCategory.SHOPPING
                    matchesAny(lower, listOf("مطعم", "مطاعم", "اكل", "طعام", "غدا", "عشا", "فطور", "وجبه", "كافيه", "قهوه", "كوفي", "food", "dinner", "lunch", "cafe")) -> com.example.model.ExpenseCategory.FOOD
                    matchesAny(lower, listOf("فاتوره", "فواتير", "كهرباء", "ماء", "نت", "انترنت", "شحن", "اشتراك", "رصيد", "bills")) -> com.example.model.ExpenseCategory.BILLS
                    matchesAny(lower, listOf("بقاله", "سوبرماركت", "تموينات", "اغراض", "خضار", "فواكه", "لحم", "دجاج", "grocery")) -> com.example.model.ExpenseCategory.GROCERY
                    matchesAny(lower, listOf("صيدليه", "علاج", "دواء", "مستشفى", "طبيب", "صحه", "health", "pharmacy")) -> com.example.model.ExpenseCategory.HEALTH
                    matchesAny(lower, listOf("ترفيه", "العاب", "سينما", "لعبه", "تذكره", "رحلة", "نزهه", "entertainment")) -> com.example.model.ExpenseCategory.ENTERTAINMENT
                    else -> com.example.model.ExpenseCategory.GENERAL
                }

                val cleanDesc = lower.replace(Regex("سجل|صرفت|حط|دفعت|ريال|سجلت|مصروف|اليوم|امس|شريت|اشتريت|اضف|$amountStr"), "").trim()
                val desc = if (cleanDesc.isNotBlank()) cleanDesc else category.labelAr

                return IntentResult(
                    action = ParsedAction.ADD_EXPENSE,
                    rawText = text,
                    targetParameter = amount.toString(),
                    secondaryParameter = category.name,
                    feedbackMessageAr = "أبشر يا غالي، سجلت لك $amountStr ريال في ${category.labelAr}.",
                    feedbackMessageEn = "Recorded $amountStr for ${category.labelEn}."
                )
            }
        }

        // 0-A. WHATSAPP VOICE MESSAGING ("اشتيك تفتح لي الواتس وتعمل رسالة لـ فلان وقله كذا" أو "ارسل واتساب لـ فلان: كذا")
        if (matchesAny(lower, listOf("واتس", "واتساب", "وتساب", "whatsapp")) &&
            matchesAny(lower, listOf("رسالة", "رساله", "ارسل", "راسل", "اكتب", "قله", "قوله", "وقله", "وقل له", "وقول له", "مسج", "send", "message"))) {

            var recipient = ""
            var msgBody = ""

            val waRegex1 = Regex(
                "(?:اشتيك|ابي|ودي|ابغى|ممكن|بالله)?\\s*(?:تفتح|افتح|ارسل|راسل|اكتب|سوي|اعمل|ابعث)?\\s*(?:لي)?\\s*(?:في|على)?\\s*(?:الواتس|الواتساب|واتساب|واتس|whatsapp)\\s*(?:وتعمل|وتكتب|وترسل)?\\s*(?:لي)?\\s*(?:رسالة|رساله)?\\s*(?:لـ|الى|ل|حق)?\\s*([\\p{L}\\p{N}_]+)\\s*(?:وقله|وقل له|وقول له|رسالة|واكتب له|قله|قوله|مضمونها)?\\s*[:\\-]?\\s*(.*)",
                RegexOption.IGNORE_CASE
            )
            val m1 = waRegex1.find(lower)
            if (m1 != null && m1.groupValues[1].isNotBlank()) {
                val candidateRec = m1.groupValues[1].trim()
                if (!candidateRec.contains("الواتس") && !candidateRec.contains("رسال")) {
                    recipient = candidateRec
                    msgBody = m1.groupValues[2].trim()
                }
            }

            if (recipient.isBlank()) {
                val waRegex2 = Regex(
                    "(?:ارسل|راسل|اكتب)\\s*(?:رسالة|رساله)?\\s*(?:واتساب|واتس|وتساب)?\\s*(?:لـ|الى|ل)?\\s*([\\p{L}\\p{N}_]+)\\s*[:\\-]?(.*)",
                    RegexOption.IGNORE_CASE
                )
                val m2 = waRegex2.find(lower)
                if (m2 != null && m2.groupValues[1].isNotBlank()) {
                    val candidateRec = m2.groupValues[1].trim()
                    if (!candidateRec.contains("الواتس") && !candidateRec.contains("رسال")) {
                        recipient = candidateRec
                        msgBody = m2.groupValues[2].trim()
                    }
                }
            }

            if (recipient.isNotBlank() || msgBody.isNotBlank()) {
                val finalRec = if (recipient.isNotBlank()) recipient else "جهة الاتصال"
                val finalMsg = if (msgBody.isNotBlank()) msgBody else text
                return IntentResult(
                    action = ParsedAction.SEND_WHATSAPP,
                    rawText = text,
                    targetParameter = finalRec,
                    secondaryParameter = finalMsg,
                    feedbackMessageAr = "نعم يا عزيزي، أنا بخدمتك وفي عيوني! جاري فتح الواتساب وتجهيز رسالتك لـ $finalRec: \"$finalMsg\". هل تطلب مني شيء آخر يا غالي؟",
                    feedbackMessageEn = "Sure, opening WhatsApp and composing your message for $finalRec: \"$finalMsg\". Anything else?"
                )
            }
        }

        // 0-B. DEEP DEVICE & SYSTEM INSPECTION ("افحص لي التلفون", "فحص عميق للهاتف", "ايش داخل تلفوني", "فحص شامل")
        if (matchesAny(lower, listOf(
            "افحص لي التلفون", "افحص التلفون", "افحص لي الهاتف", "افحص الهاتف", "افحص الجوال", "فحص عميق",
            "ايش داخل هاتفي", "ايش داخل جوالي", "ايش داخل تلفوني", "فحص شامل", "فحص النظام", "شيك على التلفون",
            "تفقد الجهاز", "افحص كل شيء", "فحص كامل", "حالة الجهاز", "تفقد التلفون", "deep scan", "inspect device", "phone inspection"
        ))) {
            return IntentResult(
                action = ParsedAction.DEEP_DEVICE_SCAN,
                rawText = text,
                feedbackMessageAr = "أبشر يا غالي، جاري فحص كافة جوانب الهاتف بعمق وتفقد التطبيقات والذاكرة والبطارية والتخزين...",
                feedbackMessageEn = "Conducting a deep inspection of device hardware, apps, storage, and battery..."
            )
        }

        // 0. SMART ROUTINES & MACROS (سيناريوهات الأوامر المتسلسلة بكلمة واحدة واللهجات)
        // A) CAR / DRIVING ROUTINE ("أنا في السيارة", "في الموتر", "ركبت الموتر", "سوق", "انا اسوق", "drive mode")
        if (matchesAny(lower, listOf(
            "انا في السياره", "انا بالسياره", "انا في سيارتي", "ركبت السياره", "وضع القياده", "وضع السياره",
            "سواقه", "بالسياره", "سوق", "رايح بالسياره", "في الموتر", "ركبت الموتر", "اسوق", "انا اسوق",
            "drive mode", "car mode", "im in the car", "im driving"
        ))) {
            return IntentResult(
                action = ParsedAction.ROUTINE_CAR,
                rawText = text,
                feedbackMessageAr = "أهلاً بك! جهزت لك وضع القيادة: فتحت خرائط الملاحة، رفعت الصوت لـ 80%، وتفقدت البلوتوث. قيادة آمنة وموفقة يا غالي!",
                feedbackMessageEn = "Driving mode activated: Navigation opened, volume at 80%, Bluetooth ready. Safe travels!"
            )
        }

        // B) SLEEP / BEDTIME ROUTINE ("وقت النوم", "اشتي ارقد", "وقت الرقاد", "بدخل ارقد", "راقد", "تصبح على خير", "sleep mode")
        if (matchesAny(lower, listOf(
            "وقت النوم", "تصبح على خير", "وضع النوم", "بدخل انام", "بروح انام", "حان وقت النوم", "سانام",
            "اشتي ارقد", "وقت الرقاد", "بدخل ارقد", "بروح ارقد", "راقد", "ابي انام", "بنام", "نعسان", "تصبح ع خير",
            "sleep mode", "bedtime", "good night", "going to sleep"
        ))) {
            return IntentResult(
                action = ParsedAction.ROUTINE_BEDTIME,
                rawText = text,
                feedbackMessageAr = "تم تفعيل وضع النوم: خفضت إضاءة الشاشة، كتمت الأصوات وخليت الهاتف صامت لتنعم بالهدوء. نوم الهنا والعوافي وتصبح على ألف خير!",
                feedbackMessageEn = "Sleep mode activated: Phone silenced, media muted and display dimmed. Sweet dreams!"
            )
        }

        // C) WORK / FOCUS ROUTINE ("أنا في العمل", "معي شغل", "عندي دوام", "وقت الدوام", "work mode")
        if (matchesAny(lower, listOf(
            "انا في العمل", "انا بالدوام", "عندي اجتماع", "وضع العمل", "بدأ الدوام", "وقت التركيز",
            "معي شغل", "عندي دوام", "وقت الدوام", "في الدوام", "معي عمل",
            "وضع الاجتماع", "اجتماع مهم", "work mode", "meeting mode", "focus mode"
        ))) {
            return IntentResult(
                action = ParsedAction.ROUTINE_WORK,
                rawText = text,
                feedbackMessageAr = "فعلت لك وضع العمل والتركيز: كتمت رنين الهاتف على الاهتزاز، وفتحت لك المفكرة. بالتوفيق في عملك وإنجازاتك!",
                feedbackMessageEn = "Work mode enabled: Phone set to vibrate and notes ready. Have a productive session!"
            )
        }

        // D) MORNING ROUTINE ("صباح الخير", "ابدأ يومي", "بداية اليوم", "صحيت", "good morning")
        if (matchesAny(lower, listOf(
            "صباح الخير", "ابدأ يومي", "بداية اليوم", "صحيت من النوم", "صباح النور", "good morning", "start my day"
        ))) {
            return IntentResult(
                action = ParsedAction.ROUTINE_MORNING,
                rawText = text,
                feedbackMessageAr = "صباح النور والسرور والبركة! ضبطت لك الصوت على مستوى مناسب وتفقدت لك حالة النظام. أتمنى لك يوماً جميلاً وموفقاً!",
                feedbackMessageEn = "Good morning! System status refreshed and ready for an amazing day."
            )
        }

        // E) EMERGENCY ROUTINE ("وضع الطوارئ", "فزعة", "طوارئ", "emergency")
        if (matchesAny(lower, listOf(
            "وضع الطوارئ", "حاله طوارئ", "طوارئ", "استعداد طوارئ", "فزعه", "الفزعه", "يا غاره الله", "emergency mode", "emergency"
        ))) {
            return IntentResult(
                action = ParsedAction.ROUTINE_EMERGENCY,
                rawText = text,
                feedbackMessageAr = "تم تفعيل وضع الطوارئ فوراً: شغلت كشاف الفلاش، رفعت الصوت لأقصى طاقة، وجهزت لوحة الاتصال.",
                feedbackMessageEn = "Emergency mode active: Flashlight on, max volume, and dialer ready."
            )
        }

        // F) BOOST / PHONE CLEAN ROUTINE ("نظف الهاتف", "برد الهاتف", "سبر التلفون", "برد الجوال", "صيانة سريعة", "boost")
        if (matchesAny(lower, listOf(
            "نظف الهاتف", "تبريد الهاتف", "برد الهاتف", "برد لي الهاتف", "سوي تبريد", "صيانه سريعه", "تفريغ الرام", "تسريع الهاتف", "سرع الجوال",
            "سبر التلفون", "سبر الجوال", "برد الجوال", "برد التلفون", "تبريد الجوال", "تبريد التلفون", "نظف الجوال", "نظف تلفوني",
            "نظف لي الهاتف", "فضي الرام", "نظف الذاكره", "تسريع الجوال", "سرع التلفون", "تنظيف الهاتف", "تنظيف الجهاز", "boost", "clean phone"
        ))) {
            return IntentResult(
                action = ParsedAction.ROUTINE_BOOST,
                rawText = text,
                feedbackMessageAr = "أبشر يا غالي، تم تبريد الهاتف وتحرير الذاكرة العشوائية، المعالج ونظام هاتفك يعملان الآن بأعلى كفاءة وسرعة. هل تطلب مني شيء آخر؟",
                feedbackMessageEn = "Phone optimized: RAM cleared and performance boosted to max efficiency. Anything else?"
            )
        }

        // G) POWER SAVER ROUTINE ("وفر الطاقة", "وفر الصارفة", "وفر البتري", "انقذ البطارية", "power saver")
        if (matchesAny(lower, listOf(
            "وفر الطاقه", "وضع التوفير", "توفير الطاقه", "وضع توفير الطاقه", "انقذ البطاريه", "حفظ الطاقه السريع",
            "وفر الصارفه", "وفر البتري", "انقذ البتري", "حافظ على البتري", "توفير البتري",
            "فعل وضع التوفير", "وضع الحفظ", "حفظ الطاقه", "وفر البطاريه", "power saver", "battery saver", "save battery", "low power mode"
        ))) {
            return IntentResult(
                action = ParsedAction.ROUTINE_POWER_SAVER,
                rawText = text,
                feedbackMessageAr = "تم تفعيل وضع توفير الطاقة الفائق: أطفأت كشاف الفلاش، خفضت مستوى الصوت، وخفضت سطوع الشاشة لحماية بطاريتك.",
                feedbackMessageEn = "Power saver mode activated: Flashlight off, volume minimized, and display dimmed."
            )
        }

        // H) BATTERY SENTINEL ("الحارس الأمني", "حارس البطارية", "حماية البطارية", "battery sentinel")
        if (matchesAny(lower, listOf(
            "حارس البطاريه", "الحارس الامني", "حمايه البطاريه", "حارس الطاقه", "تنبيه الشحن", "تنبيه البطاريه",
            "فحص الشاحن", "حارس البطارية والامان", "battery sentinel", "battery guard"
        ))) {
            return IntentResult(
                action = ParsedAction.BATTERY_SENTINEL,
                rawText = text,
                feedbackMessageAr = "حارس البطارية الأمني في خدمتك: يراقب الشاحن وينبهك صوتياً بعبارة طبيعية عند اكتمال 100% لفصل الشاحن فوراً، كما ينبهك بإنذار صوتي عندما تصبح البطارية أقل من 20% لتوصيل الشاحن.",
                feedbackMessageEn = "Battery Sentinel is active: Alerts at 100% full charge to unplug and warns when battery is below 20% to plug in."
            )
        }

        // I) ANTI-THEFT & CHARGER GUARD ("حارس السرقة", "حماية الشاحن", "إنذار السرقة", "anti-theft")
        if (matchesAny(lower, listOf(
            "حارس السرقه", "حامي الشاحن", "حمايه الشاحن", "وضع السرقه", "فعل حارس السرقه", "شغل حارس السرقه",
            "انذار السرقه", "قفل الشاحن", "حمايه الجوال من السرقه", "حمايه التلفون من السرقه", "وضع الحمايه من السرقه",
            "فعل الانذار", "حارس السرقة وفصل الشاحن", "anti theft", "charger alarm", "theft alarm", "protect charger", "arm anti theft"
        ))) {
            return IntentResult(
                action = ParsedAction.ROUTINE_ANTI_THEFT,
                rawText = text,
                feedbackMessageAr = "أبشر، جاري بدء العد التنازلي لتفعيل حارس السرقة وفصل الشاحن... ضع هاتفك بثبات على الطاولة.",
                feedbackMessageEn = "Anti-Theft arming countdown initiated. Keep your device steady on the table."
            )
        }

        // J) DISARM ALARM ("أوقف الإنذار", "إلغاء الإنذار", "disarm alarm")
        if (matchesAny(lower, listOf(
            "وقف الانذار", "اطفئ الانذار", "اطفي الانذار", "الغاء الانذار", "اسكت الصفاره", "وقف الصفاره", "الغاء حارس السرقه",
            "disarm", "disarm alarm", "stop alarm", "turn off alarm"
        ))) {
            return IntentResult(
                action = ParsedAction.DISARM_ANTI_THEFT,
                rawText = text,
                feedbackMessageAr = "لإيقاف الإنذار الأمني، يرجى إدخال رمز الأمان (PIN) أو لمس بصمة الإصبع.",
                feedbackMessageEn = "Please enter your security PIN or scan fingerprint to disarm."
            )
        }

        // K) CLOSE BACKGROUND APPS ("أغلق التطبيقات التي في الخلفية", "أبعد التطبيقات", "سكر البرامج")
        if (matchesAny(lower, listOf(
            "اغلق التطبيقات التي في الخلفيه", "اغلق تطبيقات الخلفيه", "سكر التطبيقات بالخلفيه", "سكر تطبيقات الخلفيه",
            "ابعد التطبيقات", "ابعد البرامج", "قفل التطبيقات المفتوحه", "قفل البرامج المفتوحه", "ابعد البرامج المفتوحه",
            "اغلق البرامج بالخلفيه", "سكر البرامج", "قفل البرامج", "ابعد التطبيقات التي في الخلفيه", "اغلق الخلفيه",
            "سكر الخلفيه", "قفل الخلفيه", "ابعدها", "ابعد التطبيقات", "سكر كل البرامج", "قفل كل التطبيقات",
            "اغلق كل التطبيقات", "اغلق التطبيقات المفتوحه", "سكر البرامج اللي شغاله", "اغلق البرامج الشغاله",
            "close background apps", "kill background apps", "close apps in background", "dismiss apps", "clear background"
        ))) {
            return IntentResult(
                action = ParsedAction.CLOSE_BACKGROUND_APPS,
                rawText = text,
                feedbackMessageAr = "أبشر يا غالي، أغلقت التطبيقات التي تعمل في الخلفية ونظفت الذاكرة عشان هاتفك يرجع خفيف وسريع. هل تطلب مني شيء آخر؟",
                feedbackMessageEn = "Closed background apps and cleared device memory. Anything else I can do for you?"
            )
        }

        // L) INTRUDER SILENT SELFIE / SECURITY LOG ("من حاول يفتح تلفوني", "صور المتلصصين", "سجل الحماية")
        if (matchesAny(lower, listOf(
            "من حاول يفتح تلفوني", "مين حاول يفتح جوالي", "وريني صور المتلصصين", "من مسك جوالي", "من مسك تلفوني",
            "صور المتلصصين", "سجل الحمايه", "سجل المتلصصين", "مين لمس الهاتف", "من لمس جوالي", "كشف المتلصصين",
            "متلصصين", "صور المتطفلين", "سجل التطفل", "سجل الحماية", "صور السرقه", "مين حاول يفتح الهاتف",
            "intruder selfie", "show intruders", "who touched my phone", "intruder logs"
        ))) {
            return IntentResult(
                action = ParsedAction.VIEW_INTRUDERS,
                rawText = text,
                feedbackMessageAr = "أهلاً بك، فتحت لك سجل الحماية وصور المتلصصين الذين حاولوا لمس هاتفك أو إدخال الرمز السري.",
                feedbackMessageEn = "Opened intruder protection logs and silent selfie captures."
            )
        }

        // M) READ & SUMMARIZE PDF ("افتح ملف الـ PDF واقرأ لي إياه", "لخص لي ملف PDF", "اقرأ ملف العمل")
        if (matchesAny(lower, listOf(
            "ملف pdf", "ملف الـ pdf", "ملف ال pdf", "اقرا ملف", "اقرا لي ملف", "لخص ملف", "لخص لي ملف",
            "افتح ملف pdf", "اقرا pdf", "افتح pdf", "قراءة pdf", "read pdf", "open pdf", "summarize pdf"
        ))) {
            val fileNameClean = text.replace(Regex("(?i)افتح|اقرأ|اقرا|لي|ملف|الـ|ال|pdf|لخص|وعطني|ملخص"), "").trim()
            return IntentResult(
                action = ParsedAction.READ_PDF,
                rawText = text,
                targetParameter = fileNameClean,
                feedbackMessageAr = "أبشر يا غالي، جاري فتح ملف الـ PDF وقراءة محتواه وتلخيصه لك فوراً...",
                feedbackMessageEn = "Opening PDF file and generating a speech summary..."
            )
        }

        // N) CAPTURE CAMERA PHOTO ("التقط لي صورة", "صور لي صورة بالكاميرا", "صور سيلفي")
        if (matchesAny(lower, listOf(
            "التقط صوره", "التقط لي صوره", "صور صوره", "صور لي صوره", "التقط صوره بالكاميرا",
            "صور بالكاميرا", "صورني", "التقط صوره بالكاميرا الاماميه", "صور سيلفي", "التقط سيلفي",
            "take photo", "capture photo", "snap photo", "take a picture", "take selfie"
        ))) {
            val isFront = lower.contains("اماميه") || lower.contains("أمامية") || lower.contains("سيلفي") || lower.contains("selfie")
            val facing = if (isFront) "front" else "back"
            return IntentResult(
                action = ParsedAction.CAPTURE_PHOTO,
                rawText = text,
                targetParameter = facing,
                feedbackMessageAr = if (isFront) "أبشر، جهزت لك الكاميرا الأمامية والتقطت لك الصورة فوراً." else "أبشر، جهزت لك الكاميرا والتقطت لك الصورة فوراً.",
                feedbackMessageEn = "Captured photo successfully."
            )
        }

        // O) AUDIT PERMISSIONS & SNOOPERS ("افحص أمان التطبيقات", "كاشف التلصص", "افحص الصلاحيات")
        if (matchesAny(lower, listOf(
            "افحص امان التطبيقات", "افحص التطبيقات المشبوهه", "كاشف التلصص", "فحص الصلاحيات",
            "صلاحيات التطبيقات", "تطبيقات مشبوهه", "فحص الامان", "امان الهاتف", "audit security", "app permissions"
        ))) {
            return IntentResult(
                action = ParsedAction.AUDIT_SECURITY,
                rawText = text,
                feedbackMessageAr = "أبشر يا غالي، جاري فحص أمان وصلاحيات التطبيقات والتأكد من عدم وجود أي تطبيق متلصص...",
                feedbackMessageEn = "Auditing app permissions and security state..."
            )
        }

        // P) WATER & DUST EJECTOR ("طرد الماء", "نظف السماعة من الماء", "طرد الغبار", "موجات تنظيف السماعة")
        if (matchesAny(lower, listOf(
            "طرد الماء", "نظف السماعه من الماء", "نظف السماعات من الماء", "طرد الغبار",
            "تنظيف السماعه", "تنظيف السماعات", "موجات طرد الماء", "طلع الماء من السماعه",
            "سماعات الهاتف غبار", "water eject", "clean speaker", "eject dust"
        ))) {
            return IntentResult(
                action = ParsedAction.WATER_EJECT,
                rawText = text,
                feedbackMessageAr = "أبشر يا غالي، جاري إطلاق الموجات الصوتية 165Hz لطرد الماء وتنظيف السماعة...",
                feedbackMessageEn = "Playing 165Hz acoustic waves to eject water and dust from speakers..."
            )
        }

        // Q) CLEAN STORAGE & TRASH ("نظف التخزين", "افحص الملفات الكبيرة", "توفير مساحة", "مسح المهملات")
        if (matchesAny(lower, listOf(
            "نظف التخزين", "نظف الذاكره", "فحص التخزين", "افحص الملفات الكبيره", "توفير مساحه",
            "مسح المهملات", "تنظيف المهملات", "فض لي مساحه", "clean storage", "free space"
        ))) {
            return IntentResult(
                action = ParsedAction.CLEAN_STORAGE,
                rawText = text,
                feedbackMessageAr = "أبشر يا غالي، جاري فحص التخزين ومسح الملفات المؤقتة لتوفير المساحة...",
                feedbackMessageEn = "Scanning and clearing temporary files..."
            )
        }

        // R) CURRENT GPS LOCATION ("وين مكاني الحين", "وين انا", "حدد موقعي", "اين مكاني")
        if (matchesAny(lower, listOf(
            "وين مكاني الحين", "وين انا", "حدد موقعي", "وين مكاني", "اين موقعي",
            "موقعي الحالي", "وين موقعي بالخريطه", "احداثيات موقعي", "where am i", "my location", "current location"
        ))) {
            return IntentResult(
                action = ParsedAction.CURRENT_LOCATION,
                rawText = text,
                feedbackMessageAr = "أبشر يا غالي، جاري تحديد موقعك الجغرافي وقراءة عنوانك وإحداثياتك بالخريطة...",
                feedbackMessageEn = "Locating your current GPS coordinates..."
            )
        }

        // S) MAP & GPS NAVIGATION ("اشتيك تدلنا وين شارع...", "دلنا على...", "طريق إلى...", "كيف أروح لـ...")
        if (matchesAny(lower, listOf("تدلنا", "دلنا", "دلني", "كيف اروح", "وجهني", "ودنا الى", "طريق الى", "طريق لـ", "وين شارع", "وين محل", "navigate to", "directions to"))) {
            var dest = text.replace(Regex("(?i)^(?:اشتيك|اشتي|ودّي|ابي|ابغى|ممكن|بالله|تكفى)?\\s*(?:تدلنا|تدلني|توجهنا|توجهني|ودنا|ودني|دلنا|دلني|كيف اروح|طريق|وجهني|افتح الخريطه على|افتح الخريطة على|navigate to|directions to|take me to)\\s*(?:وين|على|الى|لـ|إلى)?"), "").trim()
            if (dest.isBlank()) dest = text.replace(Regex("(?i)^(?:اشتيك تدلنا|دلنا على|وين)\\s*"), "").trim()

            return IntentResult(
                action = ParsedAction.NAVIGATE_TO,
                rawText = text,
                targetParameter = dest,
                feedbackMessageAr = "أبشر يا غالي، جاري فتح الخريطة وتشغيل الملاحة المباشرة إلى «$dest»...",
                feedbackMessageEn = "Opening live GPS navigation to $dest..."
            )
        }

        // K) READ NOTES & VOICE MEMOS ("ايش عندي ملاحظات؟", "ماذا سجلت اليوم؟", "اقرأ ملاحظاتي")
        if (matchesAny(lower, listOf(
            "ايش عندي ملاحظات", "ايش الملاحظات", "ماذا سجلت اليوم", "ماذا سجلت", "اقرا ملاحظاتي", "اقرا لي ملاحظاتي",
            "اقرا الملاحظات", "اقرا المذكرات", "شو مسجل عندي", "ايش عندي مهام", "ايش الافكار", "وريني الملاحظات",
            "عرض الملاحظات", "ملاحظاتي", "قائمة الملاحظات", "read my notes", "read notes", "what are my notes",
            "what did i record today", "my tasks", "my memos", "show notes", "show my notes"
        ))) {
            return IntentResult(
                action = ParsedAction.READ_NOTES,
                rawText = text,
                feedbackMessageAr = "أبشر، جاري مراجعة وقراءة ملاحظاتك وأفكارك المسجلة...",
                feedbackMessageEn = "Checking and reading your saved voice notes..."
            )
        }

        // L) SAVE VOICE NOTE, IDEA, OR TASK ("سجل ملاحظة: ...", "احفظ لي فكرة ...", "سجل مهمة ...")
        val noteSaveRegex = Regex(
            "^(?:من فضلك|لو سمحت|ممكن|بالله|تكفى)?\\s*(?:سجل|اكتب|احفظ|دون|ضيف|حط|نزل|save|record|add|note|remember)\\s*(?:لي)?\\s*(?:في المذكرة|في المفكرة|في النوتة)?\\s*(?:ملاحظة|ملاحظه|فكرة|فكره|مهمة|مهمه|نوتة|نوته|تذكير|note|idea|task|memo)?\\s*[:\\-]?\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val noteMatch = noteSaveRegex.find(lower)
        if (noteMatch != null && noteMatch.groupValues[1].isNotBlank()) {
            val content = noteMatch.groupValues[1].trim()
            if (content.length >= 3 && !content.startsWith("تطبيق") && !content.startsWith("برنامج") && !content.startsWith("كشاف") && !content.startsWith("واي فاي") && !content.startsWith("بلوتوث")) {
                val isIdea = lower.contains("فكرة") || lower.contains("فكره") || lower.contains("idea")
                val isTask = lower.contains("مهمة") || lower.contains("مهمه") || lower.contains("task") || lower.contains("ذكرني") || lower.contains("اجتماع")
                val type = if (isIdea) "IDEA" else if (isTask) "TASK" else "NOTE"
                val typeNameAr = if (isIdea) "الفكرة" else if (isTask) "المهمة" else "الملاحظة"

                return IntentResult(
                    action = ParsedAction.SAVE_NOTE,
                    rawText = text,
                    targetParameter = content,
                    secondaryParameter = type,
                    feedbackMessageAr = "تم، حفظت لك $typeNameAr: \"$content\" مع توثيق التاريخ والوقت.",
                    feedbackMessageEn = "Saved: \"$content\" with date and time."
                )
            }
        }

        // 1. UNINSTALL / DELETE APP ("احذف تطبيق", "اقلع تطبيق", "قلع برنامج", "امسح برنامج", "طير تطبيق", "فرمت", "uninstall")
        val uninstallRegex = Regex(
            "^(?:من فضلك|لو سمحت|ممكن|بالله|تكفى)?\\s*(?:احذف|امسح|طير|ازل|شيل|اقلع|قلع|فرمت|حرد|اطرد|تخلص من|حذف|ازاله|مسح|ديليت|حيد|فسخ|قيم|uninstall|delete|remove)\\s*(?:لي|من فضلك)?\\s*(?:تطبيقي|برنامجي|تطبيق|برنامج|application|app)?\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val uninstallMatch = uninstallRegex.find(lower)
        if (uninstallMatch != null && uninstallMatch.groupValues[1].isNotBlank()) {
            val appTarget = cleanTargetName(uninstallMatch.groupValues[1])
            if (appTarget.isNotBlank()) {
                val canonicalApp = mapToCanonicalApp(appTarget)
                return IntentResult(
                    action = ParsedAction.UNINSTALL_APP,
                    rawText = text,
                    targetParameter = canonicalApp,
                    feedbackMessageAr = "أبشر، فتحت لك تأكيد حذف تطبيق $canonicalApp.",
                    feedbackMessageEn = "Sure, opening the uninstall dialog for $canonicalApp."
                )
            }
        }

        // 2. INSTALL / DOWNLOAD FROM STORE ("نزل تطبيق", "ثبت برنامج", "حمل", "install", "download")
        val installRegex = Regex(
            "^(?:من فضلك|لو سمحت|ممكن|بالله|تكفى)?\\s*(?:نزل|ثبت|تحميل|تثبيت|حمل|ابحث في المتجر عن|ابحث بالمتجر عن|install|download|get)\\s*(?:لي)?\\s*(?:تطبيق|برنامج|app)?\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val installMatch = installRegex.find(lower)
        if (installMatch != null && installMatch.groupValues[1].isNotBlank()) {
            val appTarget = cleanTargetName(installMatch.groupValues[1])
            if (appTarget.isNotBlank()) {
                val canonicalApp = mapToCanonicalApp(appTarget)
                return IntentResult(
                    action = ParsedAction.INSTALL_APP,
                    rawText = text,
                    targetParameter = canonicalApp,
                    feedbackMessageAr = "أبشر، فتحت لك متجر التطبيقات لتثبيت: $canonicalApp.",
                    feedbackMessageEn = "Opening the App Store to install $canonicalApp."
                )
            }
        }

        // 3. IN-APP DEEP SEARCH (YouTube, Maps, Google Play, etc.)
        // A) YouTube In-App Search: e.g. "افتح اليوتيوب وابحث لي عن سورة الكهف", "شغل في اليوتيوب اغاني يمنية", "ابحث في اليوتيوب عن كذا"
        val ytDirectSearchRegex = Regex(
            "^(?:من فضلك|لو سمحت|ممكن|بالله|تكفى)?\\s*(?:شغل في اليوتيوب|شغل باليوتيوب|شغل في يوتيوب|شغل باليوتيوب|ابحث في اليوتيوب عن|ابحث باليوتيوب عن|ابحث في يوتيوب عن|ابحث باليوتيوب عن|دور في اليوتيوب عن|دور باليوتيوب عن)\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val ytDirectMatch = ytDirectSearchRegex.find(lower)
        if (ytDirectMatch != null && ytDirectMatch.groupValues[1].isNotBlank()) {
            val query = ytDirectMatch.groupValues[1].trim()
            return IntentResult(
                action = ParsedAction.OPEN_APP_SEARCH,
                rawText = text,
                targetParameter = "يوتيوب",
                secondaryParameter = query,
                feedbackMessageAr = "أبشر، فتحت لك يوتيوب وبحثت لك عن: $query.",
                feedbackMessageEn = "Opening YouTube and searching for: $query."
            )
        }

        val ytSearchRegex = Regex(
            "^(?:من فضلك|لو سمحت|ممكن|بالله|تكفى)?\\s*(?:افتح|فك|شغل|ادخل على)?\\s*(?:يوتيوب|اليوتيوب|youtube)\\s*(?:و|ثم)?\\s*(?:ابحث|دور|شغل|طلع|search)?\\s*(?:لي)?\\s*(?:فيه|فيها)?\\s*(?:عن|في)?\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val ytMatch = ytSearchRegex.find(lower)
        if (ytMatch != null && ytMatch.groupValues[1].isNotBlank()) {
            val query = ytMatch.groupValues[1].trim()
            if (query.isNotBlank() && query != "يوتيوب" && query != "اليوتيوب") {
                return IntentResult(
                    action = ParsedAction.OPEN_APP_SEARCH,
                    rawText = text,
                    targetParameter = "يوتيوب",
                    secondaryParameter = query,
                    feedbackMessageAr = "أبشر، فتحت لك يوتيوب وبحثت لك عن: $query.",
                    feedbackMessageEn = "Opening YouTube and searching for: $query."
                )
            }
        }

        // B) Maps In-App Search: e.g. "افتح الخرائط وابحث عن مطعم قريب", "ابحث في الخرائط عن محطة بنزين"
        val mapsSearchRegex = Regex(
            "^(?:من فضلك|لو سمحت|ممكن|بالله)?\\s*(?:افتح|فك)?\\s*(?:الخرائط|خرائط|خريطه|maps)\\s*(?:و|ثم)?\\s*(?:ابحث|دور)?\\s*(?:لي)?\\s*(?:عن|فيه عن)?\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val mapsMatch = mapsSearchRegex.find(lower)
        if (mapsMatch != null && mapsMatch.groupValues[1].isNotBlank()) {
            val query = mapsMatch.groupValues[1].trim()
            if (query.isNotBlank() && query != "خرائط" && query != "الخرائط") {
                return IntentResult(
                    action = ParsedAction.OPEN_APP_SEARCH,
                    rawText = text,
                    targetParameter = "خرائط",
                    secondaryParameter = query,
                    feedbackMessageAr = "أبشر، فتحت لك الخرائط لتحديد موقع: $query.",
                    feedbackMessageEn = "Opening Maps to find: $query."
                )
            }
        }

        // C) Generic App Search: "افتح تطبيق [X] وابحث لي عن [Y]" or "فك تطبيق [X] ودور لي عن [Y]"
        val genericAppSearchRegex = Regex(
            "^(?:من فضلك|لو سمحت|ممكن|بالله|تكفى)?\\s*(?:افتح|فك|شغل|ادخل على)\\s*(?:تطبيق|برنامج)?\\s*([a-zA-Z\\u0600-\\u06FF0-9]+)\\s*(?:و|ثم)?\\s*(?:ابحث|دور|طلع)\\s*(?:لي)?\\s*(?:عن|فيه عن|في)?\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val genericMatch = genericAppSearchRegex.find(lower)
        if (genericMatch != null && genericMatch.groupValues[1].isNotBlank() && genericMatch.groupValues[2].isNotBlank()) {
            val targetApp = mapToCanonicalApp(cleanTargetName(genericMatch.groupValues[1]))
            val query = genericMatch.groupValues[2].trim()
            if (targetApp.isNotBlank() && query.isNotBlank() && !isControlKeyword(targetApp)) {
                return IntentResult(
                    action = ParsedAction.OPEN_APP_SEARCH,
                    rawText = text,
                    targetParameter = targetApp,
                    secondaryParameter = query,
                    feedbackMessageAr = "أبشر، فتحت لك $targetApp وبحثت لك عن: $query.",
                    feedbackMessageEn = "Opening $targetApp and searching for: $query."
                )
            }
        }

        // 4. SEARCH FILE / FIND ("ابحث عن ملف", "دور لي على ملف", "ملف pdf", "search file")
        val fileRegex = Regex(
            "(?:ابحث عن ملف|دور لي على ملف|ابحث لي عن ملف|جد لي ملف|ابحث في الملفات عن|افتح ملف|search file|find file|locate file)\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val fileMatch = fileRegex.find(lower)
        if (fileMatch != null && fileMatch.groupValues[1].isNotBlank()) {
            val fileTarget = cleanTargetName(fileMatch.groupValues[1])
            return IntentResult(
                action = ParsedAction.SEARCH_FILE,
                rawText = text,
                targetParameter = fileTarget,
                feedbackMessageAr = "بحثت لك في ملفاتك وطلعت لك نتائج: $fileTarget.",
                feedbackMessageEn = "Searching your files for $fileTarget."
            )
        }
        if (lower.contains("ملف pdf") || lower.contains("ملفات pdf") || lower.contains(".pdf")) {
            val query = lower.replace("ملفات pdf", "").replace("ملف pdf", "").trim()
            return IntentResult(
                action = ParsedAction.SEARCH_FILE,
                rawText = text,
                targetParameter = if (query.isNotBlank()) query else "pdf",
                feedbackMessageAr = "جمعت لك كل مستندات الـ PDF الموجودة بجهازك.",
                feedbackMessageEn = "Here are all the PDF documents found on your device."
            )
        }

        // 5. FLASHLIGHT CONTROLS ("شغل الفلاش", "فك السراج", "ولع السراج", "طفي السراج", "اخمد السراج", "شغل الليت", "شعل الضو")
        if (matchesAny(lower, listOf(
            "شغل الفلاش", "ولع الفلاش", "افتح الكشاف", "شغل الكشاف", "ولع الكشاف", "ضوء الفلاش",
            "شغل الضوء", "فلاش", "كشاف", "شغل الليت", "ولع النور", "شعل الضو", "ضو الفلاش",
            "شغل السراج", "فك السراج", "ولع السراج", "شعل السراج", "افتح السراج", "فك الفلاش", "فك الليت", "فك الضو",
            "ولع الليت", "افتح الضوء", "شعل الفلاش", "سراج", "flashlight on", "turn on flashlight", "torch on"
        ))) {
            if (!matchesAny(lower, listOf("طفي", "اطفئ", "قفل", "سكر", "طف", "اخمد", "off"))) {
                return IntentResult(
                    action = ParsedAction.FLASHLIGHT_ON,
                    rawText = text,
                    feedbackMessageAr = "أبشر، شغلت لك كشاف الهاتف.",
                    feedbackMessageEn = "Turned on your flashlight."
                )
            }
        }
        if (matchesAny(lower, listOf(
            "طفي الفلاش", "اطفئ الفلاش", "طفي الكشاف", "اطفئ الكشاف", "قفل الكشاف", "سكر الكشاف",
            "طفي السراج", "اخمد السراج", "سكر السراج", "اطفي السراج", "طفي الليت", "طفي الضو", "سكر الضو",
            "طفي الضوء", "طفي النور", "قفل النور", "قفل الفلاش", "طف الكشاف", "طف الفلاش", "اخمد",
            "flashlight off", "turn off flashlight", "torch off"
        ))) {
            return IntentResult(
                action = ParsedAction.FLASHLIGHT_OFF,
                rawText = text,
                feedbackMessageAr = "تم، طفيت كشاف الهاتف.",
                feedbackMessageEn = "Turned off your flashlight."
            )
        }

        // 6. VOLUME CONTROLS ("ارفع الصوت", "علي الصوت", "زيد حسه", "قصر حسه", "قصر للصوت", "اكتم الصوت", "صامت")
        if (matchesAny(lower, listOf(
            "ارفع الصوت", "علي الصوت", "صوت عالي", "اقصي صوت", "زيد الصوت", "كبر الصوت", "قوي الصوت", "علي لي الصوت",
            "زيد حسه", "ارفع حسه", "علي حسه", "قوي حسه", "طول على الصوت", "طول الصوت",
            "volume up", "increase volume", "louder"
        ))) {
            return IntentResult(
                action = ParsedAction.VOLUME_UP,
                rawText = text,
                feedbackMessageAr = "رفعت لك صوت الهاتف.",
                feedbackMessageEn = "Turned up the volume."
            )
        }
        if (matchesAny(lower, listOf(
            "اخفض الصوت", "وطي الصوت", "صوت واطي", "قصر الصوت", "نزل الصوت", "نقص الصوت", "نصي الصوت", "صغر الصوت", "وطي لي الصوت",
            "قصر حسه", "قصر للصوت", "قصر صوته", "وطي حسه", "قصر على الصوت",
            "volume down", "decrease volume", "lower volume"
        ))) {
            return IntentResult(
                action = ParsedAction.VOLUME_DOWN,
                rawText = text,
                feedbackMessageAr = "خفضت لك صوت الهاتف.",
                feedbackMessageEn = "Lowered the volume."
            )
        }
        if (matchesAny(lower, listOf(
            "اكتم الصوت", "صامت", "كتم", "الوضع الصامت", "بدون صوت", "ميوت", "سكت الصوت", "سد الصوت",
            "اخمد حسه", "سد حسه", "اكتم حسه", "mute", "silence", "be quiet"
        ))) {
            return IntentResult(
                action = ParsedAction.VOLUME_MUTE,
                rawText = text,
                feedbackMessageAr = "تم كتم الصوت ووضع الهاتف على الصامت.",
                feedbackMessageEn = "Muted the phone sound."
            )
        }

        // 7. STEALTH MODE / QUIET ("وضع التخفي", "الوضع الهادئ", "stealth mode")
        if (matchesAny(lower, listOf("وضع التخفي", "الوضع الهادئ", "وضع صامت", "وضع الراحة", "stealth mode", "quiet mode"))) {
            return IntentResult(
                action = ParsedAction.STEALTH_MODE,
                rawText = text,
                feedbackMessageAr = "تم تفعيل الوضع الهادئ، كتمت الأصوات وخليت الهاتف على الاهتزاز.",
                feedbackMessageEn = "Quiet mode enabled, phone silenced with vibration."
            )
        }

        // 8. WI-FI AND BLUETOOTH ("افتح الواي فاي", "شغل الواي فاي", "wifi", "bluetooth")
        if (matchesAny(lower, listOf("واي فاي", "الواي فاي", "شبكه النت", "شبكه وايفاي", "وايفاي", "wifi", "open wifi", "wi-fi"))) {
            return IntentResult(
                action = ParsedAction.OPEN_WIFI,
                rawText = text,
                feedbackMessageAr = "أبشر، فتحت لك إعدادات الواي فاي لتختار الشبكة.",
                feedbackMessageEn = "Opened your Wi-Fi network settings."
            )
        }
        if (matchesAny(lower, listOf("بلوتوث", "البلوتوث", "bluetooth", "open bluetooth"))) {
            return IntentResult(
                action = ParsedAction.OPEN_BLUETOOTH,
                rawText = text,
                feedbackMessageAr = "فتحت لك إعدادات البلوتوث لربط أجهزتك.",
                feedbackMessageEn = "Opened Bluetooth settings for pairing."
            )
        }

        // 9. BATTERY / TELEMETRY ("كم الصارفة", "كم باقي في البتري", "كم شحن البتري", "كم البطارية", "جم باقي", "شكد البطارية")
        if (matchesAny(lower, listOf(
            "كم البطاريه", "نسبه البطاريه", "شحن البطاريه", "حاله الطاقه", "حراره الجهاز", "فحص الجهاز", "فحص النظام", "تقرير النظام",
            "كم الصارفه", "كم الصارفة", "شحن الصارفه", "كم باقي في البتري", "كم شحن البتري", "كيف البتري", "كم البتري", "صارفه",
            "البطاريه كام", "كام في الميه", "جم باقي بالبطاريه", "جم الشحن", "جم باقي", "شكد البطاريه", "شكد شحن",
            "قديش البطاريه", "اديش باقي شحن", "شو وضع البطاريه", "شحال باقي فالبطاريه", "قداش لاشارج",
            "battery", "battery status", "how much battery", "battery percentage"
        ))) {
            return IntentResult(
                action = ParsedAction.BATTERY_STATUS,
                rawText = text,
                feedbackMessageAr = "فحصت لك البطارية والنظام،",
                feedbackMessageEn = "Checking your battery and system,"
            )
        }

        // 10. CAMERA ("افتح الكاميرا", "شغل الكاميرا", "صور", "open camera", "take photo")
        if (matchesAny(lower, listOf("افتح الكاميرا", "شغل الكاميرا", "افتح الكيمرا", "شغل الكيمرا", "تصوير", "التقط صوره", "open camera", "camera", "take photo"))) {
            return IntentResult(
                action = ParsedAction.OPEN_CAMERA,
                rawText = text,
                feedbackMessageAr = "أبشر، فتحت لك الكاميرا لتلتقط الصور.",
                feedbackMessageEn = "Opened the camera for you."
            )
        }

        // 11. OPEN APP ("افتح تطبيق", "اشتيك تفتح", "فك تطبيق", "فك لي", "سبر لي", "ادخل على", "طلع لي", "open youtube", "launch app")
        val openAppRegex = Regex(
            "^(?:من فضلك|لو سمحت|ممكن|بالله|ودني|تكفى|اشتيك|اشتي|ابي|ابغى|ودي|اريد|عايز|بدي)?\\s*(?:تفتح|افتح|فك|سبر لي|ادخل على|طلع لي|شغل|انتقل الي|انتقل الى|تشغيل|افتحلي|فك لي|شغل لي|هاتلي|ودني لـ|ودني الى|حل|open|launch|start|run)\\s*(?:لي)?\\s*(?:في|على)?\\s*(?:تطبيقي|برنامجي|تطبيق|برنامج|app|application)?\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val openMatch = openAppRegex.find(lower)
        if (openMatch != null && openMatch.groupValues[1].isNotBlank()) {
            val rawTarget = cleanTargetName(openMatch.groupValues[1])
            if (rawTarget.isNotBlank() && !isControlKeyword(rawTarget)) {
                val canonicalApp = mapToCanonicalApp(rawTarget)
                return IntentResult(
                    action = ParsedAction.OPEN_APP,
                    rawText = text,
                    targetParameter = canonicalApp,
                    feedbackMessageAr = "أبشر يا غالي، جاري فتح $canonicalApp لك الآن. هل تطلب مني شيء آخر؟",
                    feedbackMessageEn = "Opening $canonicalApp for you right now. Anything else?"
                )
            }
        }

        // 11. WEB SEARCH ("ابحث في قوقل عن", "ابحث في جوجل عن", "ابحث في الانترنت", "search web for", "google")
        val searchRegex = Regex(
            "(?:ابحث في قوقل عن|ابحث في جوجل عن|ابحث في الانترنت عن|ابحث بالمتصفح عن|ابحث عن|دور لي عن|search web for|google|search for)\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val searchMatch = searchRegex.find(lower)
        if (searchMatch != null && searchMatch.groupValues[1].isNotBlank()) {
            val query = searchMatch.groupValues[1].trim()
            return IntentResult(
                action = ParsedAction.WEB_SEARCH,
                rawText = text,
                targetParameter = query,
                feedbackMessageAr = "أبشر، جاري البحث في قوقل عن: $query.",
                feedbackMessageEn = "Searching Google for: $query."
            )
        }

        // 12. Check ConversationalEngine for all natural speech:
        // (Time, Date, Math calculations, Greetings, Social pleasantries, Jokes, Advice, Feelings, Quick facts, Questions)
        ConversationalEngine.process(text, lower)?.let { return it }

        // 13. Direct App Names without prefixes (e.g. "واتساب", "يوتيوب", "تيك توك", "chrome")
        val directAppNames = listOf(
            "واتساب", "وتساب", "واتس", "whatsapp",
            "يوتيوب", "اليوتيوب", "youtube",
            "تليجرام", "تيليجرام", "تلجرام", "telegram",
            "فيسبوك", "فيس", "facebook",
            "انستغرام", "انستجرام", "انستا", "instagram",
            "سناب شات", "سناب", "snapchat",
            "تيك توك", "تيكتوك", "tiktok",
            "كروم", "قوقل كروم", "chrome",
            "خرائط", "الخرائط", "maps",
            "معرض الصور", "معرض صور", "استوديو", "gallery",
            "حاسبه", "حاسبة", "calculator"
        )
        for (appName in directAppNames) {
            if (lower.contains(appName)) {
                val canonical = mapToCanonicalApp(appName)
                return IntentResult(
                    action = ParsedAction.OPEN_APP,
                    rawText = text,
                    targetParameter = canonical,
                    feedbackMessageAr = "أبشر، جاري فتح تطبيق $canonical لك الآن.",
                    feedbackMessageEn = "Opening $canonical for you now."
                )
            }
        }

        // 14. Universal Speech Comprehension:
        // Understands any sentence, thought, observation, or conversational speech
        // with authentic human-to-human Arabic engagement.
        return ConversationalEngine.createUniversalConversationalFallback(text)
    }

    private fun cleanTargetName(input: String): String {
        return input.replace(Regex("^(تطبيقي|برنامجي|تطبيق|برنامج|ملف|لي|application|app|the)?\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[\\?\\!\\.]"), "")
            .trim()
    }

    private fun mapToCanonicalApp(name: String): String {
        val n = normalize(name)
        return when {
            n.contains("وتس") || n.contains("واتس") || n.contains("whatsapp") -> "واتساب"
            n.contains("يوت") || n.contains("youtube") -> "يوتيوب"
            n.contains("سناب") || n.contains("snapchat") -> "سناب شات"
            n.contains("تيك") || n.contains("tiktok") -> "تيك توك"
            n.contains("انست") || n.contains("انستا") || n.contains("instagram") -> "انستغرام"
            n.contains("فيس") || n.contains("facebook") -> "فيسبوك"
            n.contains("تلجرام") || n.contains("تليجرام") || n.contains("تيليجرام") || n.contains("telegram") -> "تليجرام"
            n.contains("كروم") || n.contains("chrome") -> "كروم"
            n.contains("خريطه") || n.contains("خرائط") || n.contains("maps") -> "خرائط"
            n.contains("معرض") || n.contains("استوديو") || n.contains("صور") || n.contains("gallery") -> "معرض الصور"
            n.contains("حاسب") || n.contains("calculator") -> "آلة حاسبة"
            n.contains("كمير") || n.contains("كامير") || n.contains("camera") -> "الكاميرا"
            else -> name.trim()
        }
    }

    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        return patterns.any { text.contains(it, ignoreCase = true) }
    }

    private fun isControlKeyword(target: String): Boolean {
        val keywords = listOf("الفلاش", "الكشاف", "الواي فاي", "البلوتوث", "الصوت", "البطاريه", "البطارية", "الكاميرا", "الكيمرا", "wifi", "bluetooth", "flashlight", "volume")
        return keywords.any { target.equals(it, ignoreCase = true) }
    }
}
