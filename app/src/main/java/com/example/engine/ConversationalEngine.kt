package com.example.engine

import com.example.model.IntentResult
import com.example.model.ParsedAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Intelligent Conversational Engine that enables the assistant to understand
 * all natural human speech: time, dates, arithmetic calculations, general questions,
 * jokes, advice, feelings, and everyday conversational dialogue.
 */
object ConversationalEngine {

    /**
     * Checks if the normalized speech matches conversational intents,
     * time, math, knowledge, or general dialogue.
     * Returns an [IntentResult] if handled, or null if it should fallback to other parsers.
     */
    fun process(rawText: String, normalized: String): IntentResult? {
        val lower = normalized.trim()

        // 1. Time & Clock queries
        checkTime(rawText, lower)?.let { return it }

        // 2. Date & Calendar queries
        checkDate(rawText, lower)?.let { return it }

        // 3. Mathematical calculations
        checkCalculation(rawText, lower)?.let { return it }

        // 4. Greetings, pleasantries & well-being
        checkSocial(rawText, lower)?.let { return it }

        // 5. Jokes, fun & entertainment
        checkJokes(rawText, lower)?.let { return it }

        // 6. Wisdom & advice
        checkWisdom(rawText, lower)?.let { return it }

        // 7. Moods, feelings & everyday situations
        checkEmpathyAndMood(rawText, lower)?.let { return it }

        // 8. Encyclopedia & quick facts (Capitals, geography, science)
        checkQuickFacts(rawText, lower)?.let { return it }

        // 9. General Questions / Web Search intent
        checkQuestion(rawText, lower)?.let { return it }

        return null
    }

    private fun checkTime(raw: String, lower: String): IntentResult? {
        val timeKeywords = listOf(
            "كم الساعه", "الساعه كم", "كم الوقت", "الوقت الان", "ايش الوقت", "شو الوقت",
            "الساعه كم الحين", "الوقت الحين", "كم ساعه", "time", "what time is it", "current time"
        )
        if (timeKeywords.any { lower.contains(it) }) {
            val cal = java.util.Calendar.getInstance()
            val hour12 = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
            val minute = cal.get(java.util.Calendar.MINUTE)
            val isPm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.PM
            val amPmAr = if (isPm) "مساءً" else "صباحاً"
            val amPmEn = if (isPm) "PM" else "AM"
            val minFormatted = String.format(Locale.ROOT, "%02d", minute)

            val timeAr = "$hour12:$minFormatted $amPmAr"
            val timeEn = "$hour12:$minFormatted $amPmEn"

            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "الساعة الآن هي $timeAr.",
                feedbackMessageEn = "The time is currently $timeEn."
            )
        }
        return null
    }

    private fun checkDate(raw: String, lower: String): IntentResult? {
        val dateKeywords = listOf(
            "تاريخ اليوم", "اليوم ايش", "كم التاريخ", "تاريخ كم اليوم", "ايش اليوم",
            "شو اليوم", "اليوم شو", "تاريخ كم", "date", "what is today", "today date"
        )
        if (dateKeywords.any { lower.contains(it) }) {
            val date = Date()
            val dayNameFormatAr = SimpleDateFormat("EEEE", Locale.forLanguageTag("ar"))
            val fullDateFormatAr = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ar"))
            val fullDateFormatEn = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)

            val dayAr = dayNameFormatAr.format(date)
            val dateStrAr = fullDateFormatAr.format(date)
            val dateStrEn = fullDateFormatEn.format(date)

            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "اليوم هو $dayAr، الموافق $dateStrAr.",
                feedbackMessageEn = "Today is $dateStrEn."
            )
        }
        return null
    }

    private fun checkCalculation(raw: String, lower: String): IntentResult? {
        val mathPattern = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:زائد|زايد|\\+|ناقص|طرح|\\-|في|ضرب|\\*|×|تقسيم|قسمه|على|\\/)\\s*(\\d+(?:\\.\\d+)?)")
        val match = mathPattern.find(lower) ?: return null

        val num1 = match.groupValues[1].toDoubleOrNull() ?: return null
        val num2 = match.groupValues[2].toDoubleOrNull() ?: return null

        val matchedText = match.value
        val isAdd = matchedText.contains("زائد") || matchedText.contains("زايد") || matchedText.contains("+")
        val isSub = matchedText.contains("ناقص") || matchedText.contains("طرح") || matchedText.contains("-")
        val isMul = matchedText.contains("في") || matchedText.contains("ضرب") || matchedText.contains("*") || matchedText.contains("×")
        val isDiv = matchedText.contains("تقسيم") || matchedText.contains("قسمه") || matchedText.contains("على") || matchedText.contains("/")

        var result: Double? = null
        var opSymbol = ""
        when {
            isAdd -> { result = num1 + num2; opSymbol = "+" }
            isSub -> { result = num1 - num2; opSymbol = "-" }
            isMul -> { result = num1 * num2; opSymbol = "×" }
            isDiv -> {
                if (num2 != 0.0) {
                    result = num1 / num2
                    opSymbol = "÷"
                } else {
                    return IntentResult(
                        action = ParsedAction.TALK_ASSISTANT,
                        rawText = raw,
                        feedbackMessageAr = "لا يمكن القسمة على صفر يا غالي.",
                        feedbackMessageEn = "Cannot divide by zero."
                    )
                }
            }
        }

        if (result != null) {
            val formattedResult = if (result % 1.0 == 0.0) result.toLong().toString() else String.format(Locale.ROOT, "%.2f", result)
            val formattedNum1 = if (num1 % 1.0 == 0.0) num1.toLong().toString() else num1.toString()
            val formattedNum2 = if (num2 % 1.0 == 0.0) num2.toLong().toString() else num2.toString()

            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "ناتج $formattedNum1 $opSymbol $formattedNum2 يساوي $formattedResult.",
                feedbackMessageEn = "The result of $formattedNum1 $opSymbol $formattedNum2 is $formattedResult."
            )
        }
        return null
    }

    private fun checkSocial(raw: String, lower: String): IntentResult? {
        // Greetings
        if (matchesAny(lower, listOf("السلام عليكم", "وعليكم السلام", "سلام عليكم"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "وعليكم السلام ورحمة الله وبركاته! أهلاً بك وسهلاً، كيف أقدر أساعدك اليوم في هاتفك؟",
                feedbackMessageEn = "Peace be upon you! Welcome, how can I assist you on your phone today?"
            )
        }
        if (matchesAny(lower, listOf("صباح الخير", "صباح النور", "صباح الورد", "good morning"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "صباح النور والسرور والبركة! أتمنى لك يوماً رائعاً، آمرني وش تحب نسوي في الجهاز؟",
                feedbackMessageEn = "Good morning! Wishing you a wonderful day, what would you like me to do on your device?"
            )
        }
        if (matchesAny(lower, listOf("مساء الخير", "مساء النور", "مساء الورد", "good evening"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "مساء الخير والأنوار! يا هلا فيك، أنا في خدمتك متى ما احتجت أي أمر.",
                feedbackMessageEn = "Good evening! Welcome, I'm at your service whenever you need anything."
            )
        }
        if (matchesAny(lower, listOf("مرحبا", "اهلا", "اهلين", "يا هلا", "هلا والله", "حياك", "ارحب", "يا مرحب", "حياك الله", "يا ارحب", "هاي", "hello", "hi", "hey"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "يا هلا ومرحبا والمهلي ما يولي! أرحب يا غالي، أنا معك ومستعد لتنفيذ أي أمر تطلبه بصوتك فوراً.",
                feedbackMessageEn = "Hello and warmest welcome! I am here and ready to execute any command you speak immediately."
            )
        }

        // Asking how are you
        if (matchesAny(lower, listOf("كيف حالك", "شخبارك", "اخبارك", "كيفك", "عساك بخير", "طمني عنك", "علومك", "كيف صحتك", "كيف انت", "ايش اخبارك", "ايش مسوي", "شلونك", "how are you"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "بخير وعافية والحمد لله، نعم يا عزيزي آمرني! أنا حاضر وسعيد بخدمتك، هل تطلب مني شيء أنفذه لك في هاتفك الآن؟",
                feedbackMessageEn = "I'm doing great, at your service! What would you like me to do on your device?"
            )
        }

        // Gratitude & compliments
        if (matchesAny(lower, listOf("شكرا", "مشكور", "يعطيك العافيه", "الله يعافيك", "تسلم", "ما قصرت", "الله يسعدك", "شكرا جزيلا", "كفو", "بيض الله وجهك", "عشت", "قواك الله", "فديتك", "thank you", "thanks"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "العفو ووجهك أبيض يا غالي! تسلم وتعيش، هذا واجبي وأنا في خدمتك دائماً وأبداً متى ما احتجت أي شيء.",
                feedbackMessageEn = "You are most welcome! Always honored to be at your service, feel free to ask anytime."
            )
        }
        if (matchesAny(lower, listOf("احبك", "انت رائع", "ممتاز", "شاطر", "انت بطل", "ما شاء الله عليك", "يا ذيب", "والنعم", "i love you", "great job", "awesome"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "الله يسعدك ويحفظك وما عليك زود يا أصيل! هذا من كرمك وطيب أصلك، ويسعدني ويشرفني أكون مساعدك المفضل.",
                feedbackMessageEn = "Thank you so much! It's a true honor to be your assistant."
            )
        }

        // Farewell
        if (matchesAny(lower, listOf("مع السلامه", "باي", "في امان الله", "تصبح على خير", "وداعا", "اشوفك على خير", "يلا باي", "goodbye", "bye", "good night"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "في أمان الله وحفظه ورعايته! متى ما احتجتني اضغط وتكلم وأنا حاضر دائماً.",
                feedbackMessageEn = "Goodbye and take care! Whenever you need me, just hold and speak."
            )
        }

        // Identity & capabilities
        if (matchesAny(lower, listOf("مين انت", "من انت", "من تكون", "وش اسمك", "ما اسمك", "عرف عن نفسك", "من برمجك", "who are you"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "أنا رفيقك ومساعدك الشخصي في الهاتف. أساعدك على تسجيل مصاريفك ومتابعة محفظتك بصوتك، وإرسال رسائل واتساب، وفحص هاتفك وتبريده، وإدارة تطبيقاتك، والتحكم بالكشاف والصوت، والإجابة عن أي استفسار أو كتابة رسالة تطلبها مني. آمرني يا غالي!",
                feedbackMessageEn = "I am your personal voice companion. I track your expenses and wallet by voice, send WhatsApp messages, inspect device health, launch apps, manage flashlight and volume, and answer all your inquiries."
            )
        }
        if (matchesAny(lower, listOf("وش تقدر تسوي", "ماذا تستطيع ان تفعل", "ايش تسوي", "وش وظيفتك", "وش عندك", "what can you do"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "أقدر أنفذ لك مهام كثيرة بصوتك: تسجيل مصاريفك فوراً مثل «سجل صرفت 500 ريال تسوق» وقراءة مجموع ما صرفته، إرسال رسائل واتساب، فحص الهاتف فحصاً عميقاً، فتح وإدارة التطبيقات، تشغيل الكشاف، كتم ورفع الصوت، حفظ الملاحظات، والإجابة عن أي استشارة أو صياغة أي إيميل ورسالة ترغب بها.",
                feedbackMessageEn = "I can track your expenses ('Log 500 shopping') and read your totals, send WhatsApp messages, perform deep phone diagnostics, launch apps, control torch and volume, and assist you with anything you need."
            )
        }

        return null
    }

    private fun checkJokes(raw: String, lower: String): IntentResult? {
        val jokeTriggers = listOf("احكي نكته", "قول نكته", "نكته", "ضحكني", "وسع صدري", "joke", "tell a joke")
        if (jokeTriggers.any { lower.contains(it) }) {
            val jokes = listOf(
                "واحد كسلان راح للدكتور، قاله الدكتور: لازم تمشي كل يوم 5 كيلومتر. اتصل فيه بعد شهر قاله: دكتور أنا الحين على حدود الأردن شو أعمل؟",
                "واحد دخل محل أجهزة وسأل البائع: عندك تلفزيونات ملونة؟ قاله البائع: نعم! قاله: تمام، عطني واحد لونه أزرق!",
                "واحد سأل صاحبه: إيش الفرق بين الأسبوع والمطعم؟ قاله: الأسبوع فيه سبت والمطعم فيه شوك ومعالق!",
                "مبرمج طلب من زوجته تروح السوبرماركت وقالت له: هات علبة حليب، وإذا لقيت بيض جيب عشرة. راح السوبرماركت ولقى بيض، فرجع ومعه 10 علب حليب!"
            )
            val selectedJoke = jokes.random()
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "من عيوني، اسمع هذه:\n$selectedJoke",
                feedbackMessageEn = "Here is a light-hearted joke for you!"
            )
        }
        return null
    }

    private fun checkWisdom(raw: String, lower: String): IntentResult? {
        val wisdomTriggers = listOf("نصيحه", "حكمه", "عطني نصيحه", "حكمه اليوم", "نصيحه اليوم", "advice", "quote", "wisdom")
        if (wisdomTriggers.any { lower.contains(it) }) {
            val quotes = listOf(
                "لا تؤجل عمل اليوم إلى الغد، فالإنجاز الصغير اليوم يصنع نجاحاً كبيراً غداً.",
                "القوة الحقيقية ليست في عدم السقوط، بل في النهوض في كل مرة تسقط فيها.",
                "كن جميلاً ترى الوجود جميلاً، والتفاؤل هو أول خطوة نحو تحقيق أهدافك.",
                "خير الناس أنفعهم للناس، والكلمة الطيبة صدقة تفتح القلوب."
            )
            val selectedQuote = quotes.random()
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "تفضل هذه الحكمة القيمة:\n\"$selectedQuote\"",
                feedbackMessageEn = "Here is a piece of wisdom:\n\"$selectedQuote\""
            )
        }
        return null
    }

    private fun checkEmpathyAndMood(raw: String, lower: String): IntentResult? {
        // Boredom
        if (matchesAny(lower, listOf("طفشان", "زهقان", "ملل", "ابي اتسلى", "بورد", "ضيق صدري", "bored"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "روق يا غالي ولا تشيل هم! وش رأيك نشغل لك مقطع يوسع صدرك على يوتيوب، أو أقولك نكتة لطيفة؟ اطلب وأبشر.",
                feedbackMessageEn = "Cheer up! Would you like me to open YouTube or tell you a fun joke? Just ask."
            )
        }

        // Tired / Sleep
        if (matchesAny(lower, listOf("تعبان", "مرهق", "بنام", "نعسان", "ابي انام", "رايح انام", "tired", "sleepy"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "سلامتك وألف لا بأس عليك، ارتاح وخذ كفايتك من النوم. إذا تبي أكتم لك صوت الهاتف عشان ترتاح بدون إزعاج بس قولي اكتم الصوت.",
                feedbackMessageEn = "Take care and rest well. If you want me to silence your phone, just say mute sound."
            )
        }

        // Hunger / Food
        if (matchesAny(lower, listOf("جوعان", "ابي اكل", "مشتهي اكل", "مطعم", "وين اكل", "hungry", "food"))) {
            return IntentResult(
                action = ParsedAction.WEB_SEARCH,
                rawText = raw,
                targetParameter = "أفضل المطاعم القريبة",
                feedbackMessageAr = "عوافي مقدماً يا غالي! فتحت لك الآن بحثاً عن أقرب وأفضل المطاعم حولك لتختار ما يناسبك.",
                feedbackMessageEn = "Bon appetit! Searching for nearby top restaurants for you."
            )
        }

        // Weather
        if (matchesAny(lower, listOf("الطقس", "الجو", "درجه الحراره", "مطر", "حر", "برد", "weather", "temperature"))) {
            return IntentResult(
                action = ParsedAction.WEB_SEARCH,
                rawText = raw,
                targetParameter = "الطقس اليوم",
                feedbackMessageAr = "أبشر، فتحت لك حالة الطقس ودرجات الحرارة لليوم لتكون على اطلاع دائم.",
                feedbackMessageEn = "Opening the latest weather forecast for today."
            )
        }

        // Success / Celebration
        if (matchesAny(lower, listOf("نجحت", "فزت", "شريت سياره", "ترقيت", "مبروك", "مبسوط", "فرحان", "happy", "success"))) {
            return IntentResult(
                action = ParsedAction.TALK_ASSISTANT,
                rawText = raw,
                feedbackMessageAr = "ألف ألف مبروك! ما شاء الله تبارك الله، عسى أيامك كلها فرح وإنجازات وتوفيق تستاهل كل خير.",
                feedbackMessageEn = "Congratulations! Wishing you continued joy and success."
            )
        }

        return null
    }

    private fun checkQuickFacts(raw: String, lower: String): IntentResult? {
        val factsMap = mapOf(
            "عاصمه السعوديه" to "عاصمة المملكة العربية السعودية هي مدينة الرياض.",
            "عاصمه مصر" to "عاصمة جمهورية مصر العربية هي مدينة القاهرة.",
            "عاصمه الامارات" to "عاصمة دولة الإمارات العربية المتحدة هي أبوظبي.",
            "عاصمه قطر" to "عاصمة دولة قطر هي مدينة الدوحة.",
            "عاصمه الكويت" to "عاصمة دولة الكويت هي مدينة الكويت.",
            "عاصمه البحرين" to "عاصمة مملكة البحرين هي مدينة المنامة.",
            "عاصمه عمان" to "عاصمة سلطنة عُمان هي مدينة مسقط.",
            "عاصمه الاردن" to "عاصمة المملكة الأردنية الهاشمية هي مدينة عمّان.",
            "عاصمه العراق" to "عاصمة جمهورية العراق هي مدينة بغداد.",
            "عاصمه فرنسا" to "عاصمة فرنسا هي مدينة باريس.",
            "عاصمه بريطانيا" to "عاصمة بريطانيا هي مدينة لندن.",
            "عاصمه امريكا" to "عاصمة الولايات المتحدة الأمريكية هي واشنطن العاصمة.",
            "عاصمه اليابان" to "عاصمة اليابان هي مدينة طوكيو.",
            "عاصمه المانيا" to "عاصمة ألمانيا هي مدينة برلين.",
            "عاصمه ايطاليا" to "عاصمة إيطاليا هي مدينة روما.",
            "عاصمه تركيا" to "عاصمة تركيا هي مدينة أنقرة.",
            "اكبر قاره" to "أكبر قارة في العالم من حيث المساحة وعدد السكان هي قارة آسيا.",
            "اطول نهر" to "أطول نهر في العالم هو نهر النيل في قارة أفريقيا.",
            "اعلى قمه" to "أعلى قمة جبلية في العالم هي قمة إفرست في سلسلة جبال الهيمالايا بارتفاع 8848 متراً.",
            "سرعه الضوء" to "سرعة الضوء في الفراغ تبلغ حوالي 300 ألف كيلومتر في الثانية الواحدة.",
            "عدد سور القران" to "عدد سور القرآن الكريم هو 114 سورة كريمة.",
            "عدد كواكب المجموعه الشمسيه" to "عدد كواكب المجموعة الشمسية هو 8 كواكب تدور حول الشمس."
        )

        for ((key, value) in factsMap) {
            if (lower.contains(key)) {
                return IntentResult(
                    action = ParsedAction.TALK_ASSISTANT,
                    rawText = raw,
                    feedbackMessageAr = value,
                    feedbackMessageEn = value
                )
            }
        }
        return null
    }

    private fun checkQuestion(raw: String, lower: String): IntentResult? {
        val searchExplicit = listOf(
            "ابحث في قوقل", "ابحث في جوجل", "قوقل", "جوجل", "google", "ابحث بالنت", "ابحث في الانترنت",
            "ابحث في المتصفح", "بحث في قوقل", "بحث قوقل"
        )

        if (searchExplicit.any { lower.contains(it) }) {
            val query = raw.replace(Regex("ابحث في (قوقل|جوجل|النت|الانترنت|المتصفح)|بحث في قوقل|بحث قوقل|google", RegexOption.IGNORE_CASE), "").replace(Regex("[؟\\?]"), "").trim()
            return IntentResult(
                action = ParsedAction.WEB_SEARCH,
                rawText = raw,
                targetParameter = query.ifBlank { raw },
                feedbackMessageAr = "أبشر، جاري فتح بحث قوقل عن: ${query.ifBlank { raw }}",
                feedbackMessageEn = "Searching Google for: ${query.ifBlank { raw }}"
            )
        }
        return null
    }

    /**
     * Universal sentence comprehension for any spoken phrase that didn't match local triggers.
     * Hands over to the Hybrid AI Brain (Gemini via Firebase AI Logic) for deep conversational reasoning.
     */
    fun createUniversalConversationalFallback(rawText: String): IntentResult {
        return IntentResult(
            action = ParsedAction.TALK_ASSISTANT,
            rawText = rawText,
            feedbackMessageAr = "",
            feedbackMessageEn = ""
        )
    }

    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        return patterns.any { text.contains(it, ignoreCase = true) }
    }
}
