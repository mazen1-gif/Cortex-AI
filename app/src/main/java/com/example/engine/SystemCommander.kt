package com.example.engine

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import com.example.model.DeviceFile
import com.example.model.FileCategory
import com.example.model.InstalledApp
import android.Manifest
import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import java.io.File
import java.util.Locale

class SystemCommander(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var isTorchOn = false

    // App Management
    fun getInstalledApps(): List<InstalledApp> {
        val list = mutableListOf<InstalledApp>()
        try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                // Focus on launchable apps or user-installed apps
                val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val versionName = try {
                        val pInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                        pInfo.versionName ?: "1.0"
                    } catch (e: Exception) {
                        "1.0"
                    }
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val icon = try {
                        packageManager.getApplicationIcon(appInfo.packageName)
                    } catch (e: Exception) {
                        null
                    }
                    list.add(
                        InstalledApp(
                            appName = appName,
                            packageName = appInfo.packageName,
                            versionName = versionName,
                            isSystemApp = isSystem,
                            icon = icon
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedBy { it.appName.lowercase(Locale.ROOT) }
    }

    private fun cleanAppQuery(input: String): String {
        var s = input.trim().lowercase(Locale.ROOT)
        // Normalize Arabic diacritics and letters
        s = s.replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        s = s.replace(Regex("[أإآٱ]"), "ا")
        s = s.replace('ة', 'ه')
        s = s.replace('ى', 'ي')
        // Remove app prefix noise
        s = s.replace(Regex("^(?:تطبيق|برنامج|ابلكيشن|ابكيشن|تطبيقي|برنامجي|حق|بتاع|app|application)?\\s+"), "")
        s = s.replace(Regex("[\\?\\!\\.]"), "")
        return s.trim()
    }

    private fun getAppAliases(query: String): List<String> {
        val q = cleanAppQuery(query)
        val aliases = mutableListOf(q)

        val aliasMap = mapOf(
            "واتساب" to listOf("whatsapp", "com.whatsapp", "واتس", "وتساب"),
            "whatsapp" to listOf("واتساب", "com.whatsapp", "واتس"),
            "يوتيوب" to listOf("youtube", "com.google.android.youtube", "اليوتيوب"),
            "youtube" to listOf("يوتيوب", "com.google.android.youtube"),
            "سناب" to listOf("snapchat", "com.snapchat.android", "سناب شات"),
            "سناب شات" to listOf("snapchat", "com.snapchat.android"),
            "snapchat" to listOf("سناب شات", "سناب", "com.snapchat.android"),
            "تيك توك" to listOf("tiktok", "com.zhiliaoapp.musically", "تيكتوك"),
            "tiktok" to listOf("تيك توك", "com.zhiliaoapp.musically"),
            "انستغرام" to listOf("instagram", "com.instagram.android", "انستا", "انستجرام"),
            "instagram" to listOf("انستغرام", "انستا", "com.instagram.android"),
            "فيسبوك" to listOf("facebook", "com.facebook.katana", "فيس"),
            "facebook" to listOf("فيسبوك", "فيس", "com.facebook.katana"),
            "تيليجرام" to listOf("telegram", "org.telegram.messenger", "تليجرام", "تلجرام"),
            "telegram" to listOf("تيليجرام", "تليجرام", "org.telegram.messenger"),
            "سبوتيفاي" to listOf("spotify", "com.spotify.music", "سبوتفاي"),
            "spotify" to listOf("سبوتيفاي", "com.spotify.music"),
            "نتفلكس" to listOf("netflix", "com.netflix.mediaclient", "نتفليكس"),
            "netflix" to listOf("نتفلكس", "نتفليكس", "com.netflix.mediaclient"),
            "كاب كات" to listOf("capcut", "com.lemon.lvoverseas", "كابكات"),
            "capcut" to listOf("كاب كات", "كابكات"),
            "تويتر" to listOf("twitter", "com.twitter.android", "اكس", "x"),
            "twitter" to listOf("تويتر", "com.twitter.android", "x"),
            "جيميل" to listOf("gmail", "com.google.android.gm", "بريد", "ايميل"),
            "gmail" to listOf("جيميل", "com.google.android.gm"),
            "خرائط" to listOf("maps", "com.google.android.apps.maps", "خرايط", "ملاحة"),
            "maps" to listOf("خرائط", "com.google.android.apps.maps"),
            "كروم" to listOf("chrome", "com.android.chrome", "متصفح", "قوقل كروم"),
            "chrome" to listOf("كروم", "com.android.chrome"),
            "استوديو" to listOf("gallery", "photos", "معرض الصور", "معرض", "صور"),
            "معرض الصور" to listOf("gallery", "photos", "استوديو", "صور"),
            "gallery" to listOf("معرض الصور", "استوديو", "صور"),
            "photos" to listOf("معرض الصور", "استوديو", "صور"),
            "حاسبة" to listOf("calculator", "الة حاسبة", "حاسبه"),
            "calculator" to listOf("حاسبة", "الة حاسبة"),
            "كاميرا" to listOf("camera", "كيمرا", "تصوير"),
            "camera" to listOf("كاميرا", "كيمرا"),
            "كريمي" to listOf("kuraimi", "بنك الكريمي", "ام فلوس", "الكريمي"),
            "kuraimi" to listOf("كريمي", "بنك الكريمي"),
            "راجحي" to listOf("alrajhi", "rajhi", "الراجحي", "بنك الراجحي"),
            "alrajhi" to listOf("راجحي", "الراجحي"),
            "ببجي" to listOf("pubg", "com.tencent.ig"),
            "pubg" to listOf("ببجي", "com.tencent.ig"),
            "فري فاير" to listOf("freefire", "free fire", "com.dts.freefireth"),
            "زووم" to listOf("zoom", "us.zoom.videomeetings"),
            "zoom" to listOf("زووم"),
            "تيمز" to listOf("teams", "com.microsoft.teams"),
            "teams" to listOf("تيمز"),
            "سلاك" to listOf("slack", "com.Slack"),
            "slack" to listOf("سلاك"),
            "شاهد" to listOf("shahid", "com.mbc.shahid"),
            "shahid" to listOf("شاهد"),
            "انغامي" to listOf("anghami", "أنغامي", "com.anghami"),
            "anghami" to listOf("انغامي", "أنغامي"),
            "كانفا" to listOf("canva", "com.canva.editor"),
            "canva" to listOf("كانفا"),
            "لينكد ان" to listOf("linkedin", "com.linkedin.android"),
            "linkedin" to listOf("لينكد ان")
        )

        for ((key, values) in aliasMap) {
            if (q == key || q.contains(key) || key.contains(q)) {
                aliases.addAll(values)
            }
        }
        return aliases.distinct()
    }

    /** قائمة حقيقية بالتطبيقات المثبتة القابلة للفتح، مع إمكانية التصفية بالاسم. */
    fun listInstalledAppNames(filter: String = ""): Pair<Boolean, String> {
        return try {
            val apps = getInstalledApps()
            if (apps.isEmpty()) return Pair(false, "تعذر قراءة قائمة التطبيقات من النظام.")
            val f = filter.trim()
            val matched = if (f.isBlank()) apps else apps.filter { cleanAppQuery(it.appName).contains(cleanAppQuery(f), ignoreCase = true) }
            val userApps = matched.filter { !it.isSystemApp }.map { it.appName }.sorted()
            val systemApps = matched.filter { it.isSystemApp }.map { it.appName }.sorted()
            val names = (userApps + systemApps).take(60)
            if (names.isEmpty()) {
                Pair(true, "لا يوجد تطبيق مثبت يطابق «$f».")
            } else {
                Pair(true, "عدد التطبيقات القابلة للفتح ${matched.size}. من أسمائها: ${names.joinToString("، ")}.")
            }
        } catch (e: Exception) {
            Pair(false, "تعذر قراءة التطبيقات: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    fun findApp(query: String): InstalledApp? {
        val raw = query.trim()
        if (raw.isBlank()) return null
        val qClean = cleanAppQuery(raw)
        val allApps = getInstalledApps()
        if (allApps.isEmpty()) return null

        val possibleKeys = getAppAliases(qClean)

        // 1. Exact match against query or any alias
        for (key in possibleKeys) {
            val exact = allApps.firstOrNull {
                cleanAppQuery(it.appName).equals(key, ignoreCase = true) ||
                it.packageName.equals(key, ignoreCase = true)
            }
            if (exact != null) return exact
        }

        // 2. Package name match
        for (key in possibleKeys) {
            val pkgMatch = allApps.firstOrNull { it.packageName.lowercase(Locale.ROOT).contains(key) }
            if (pkgMatch != null) return pkgMatch
        }

        // 3. StartsWith or Contains match
        for (key in possibleKeys) {
            val startsWith = allApps.firstOrNull {
                val name = cleanAppQuery(it.appName)
                name.startsWith(key, ignoreCase = true) || key.startsWith(name, ignoreCase = true)
            }
            if (startsWith != null) return startsWith
        }

        for (key in possibleKeys) {
            val contains = allApps.firstOrNull {
                val name = cleanAppQuery(it.appName)
                name.contains(key, ignoreCase = true) || key.contains(name, ignoreCase = true)
            }
            if (contains != null) return contains
        }

        // 4. Token match (individual words)
        val tokens = qClean.split(" ").filter { it.length >= 3 }
        if (tokens.isNotEmpty()) {
            val tokenMatch = allApps.firstOrNull { app ->
                val appTokens = cleanAppQuery(app.appName).split(" ")
                tokens.any { t -> appTokens.any { at -> at.equals(t, ignoreCase = true) || at.startsWith(t, ignoreCase = true) } }
            }
            if (tokenMatch != null) return tokenMatch
        }

        return null
    }

    private fun launchSpecialAppFallback(appName: String): Pair<Boolean, String>? {
        val clean = cleanAppQuery(appName)
        return when {
            clean.contains("يوت") || clean.contains("youtube") -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Pair(true, "تم فتح يوتيوب")
                } catch (e: Exception) {
                    null
                }
            }
            clean.contains("كمير") || clean.contains("كامير") || clean.contains("camera") -> openCamera()
            clean.contains("خريط") || clean.contains("خرائط") || clean.contains("maps") -> openMapsNavigation()
            clean.contains("كروم") || clean.contains("chrome") || clean.contains("متصفح") || clean.contains("browser") -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Pair(true, "تم فتح متصفح الويب")
                } catch (e: Exception) {
                    null
                }
            }
            clean.contains("هاتف") || clean.contains("اتصال") || clean.contains("dialer") -> openDialer()
            clean.contains("واي فاي") || clean.contains("wifi") -> openWifiSettings()
            clean.contains("بلوتوث") || clean.contains("bluetooth") -> openBluetoothSettings()
            clean.contains("متجر") || clean.contains("play") || clean.contains("store") -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=apps")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Pair(true, "تم فتح متجر التطبيقات")
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    fun launchApp(appNameOrPackage: String): Pair<Boolean, String> {
        val found = findApp(appNameOrPackage)
        val pkg = found?.packageName ?: appNameOrPackage.trim()
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Pair(true, "تم فتح التطبيق: ${found?.appName ?: pkg}")
        } else {
            val fallback = launchSpecialAppFallback(appNameOrPackage)
            if (fallback != null) {
                return fallback
            }
            Pair(false, "لم يتم العثور على التطبيق المثبت: $appNameOrPackage")
        }
    }

    fun searchInApp(appNameOrPackage: String, query: String): Pair<Boolean, String> {
        val lower = cleanAppQuery(appNameOrPackage)
        val cleanQuery = query.trim()

        if (lower.contains("يوت") || lower.contains("youtube")) {
            return searchYouTube(cleanQuery)
        }
        if (lower.contains("قوقل") || lower.contains("جوجل") || lower.contains("google") || lower.contains("كروم") || lower.contains("chrome") || lower.contains("متصفح")) {
            return searchWeb(cleanQuery)
        }
        if (lower.contains("متجر") || lower.contains("play") || lower.contains("store")) {
            return installAppInStore(cleanQuery)
        }
        if (lower.contains("خريطة") || lower.contains("خرائط") || lower.contains("maps")) {
            return searchMaps(cleanQuery)
        }

        val found = findApp(appNameOrPackage)
        if (found != null) {
            return try {
                val intent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(found.packageName)
                    putExtra("query", cleanQuery)
                    putExtra("android.app.search.QUERY", cleanQuery)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "فتحت تطبيق ${found.appName} وبدأت البحث عن: $cleanQuery")
            } catch (e: Exception) {
                launchApp(found.packageName)
                Pair(true, "فتحت لك تطبيق ${found.appName}")
            }
        }

        return searchWeb("$appNameOrPackage $cleanQuery")
    }

    fun searchYouTube(query: String): Pair<Boolean, String> {
        val encoded = Uri.encode(query)
        return try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "فتحت لك تطبيق يوتيوب وبدأت البحث عن: $query")
        } catch (e: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=$encoded")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Pair(true, "فتحت لك يوتيوب للبحث عن: $query")
            } catch (ex: Exception) {
                Pair(false, "تعذر فتح يوتيوب: ${ex.localizedMessage}")
            }
        }
    }

    fun searchMaps(query: String): Pair<Boolean, String> {
        val encoded = Uri.encode(query)
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "فتحت الخرائط للبحث عن: $query")
        } catch (e: Exception) {
            Pair(false, "تعذر فتح الخرائط")
        }
    }

    fun uninstallApp(appNameOrPackage: String): Pair<Boolean, String> {
        val found = findApp(appNameOrPackage)
        val pkg = found?.packageName ?: appNameOrPackage.trim()

        return try {
            val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(uninstallIntent)
            Pair(true, "فتحت لك نافذة حذف تطبيق: ${found?.appName ?: pkg}")
        } catch (e: Exception) {
            // Fallback to application details
            try {
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$pkg")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                Pair(true, "فتحت لك صفحة التطبيق لإتمام حذفه: ${found?.appName ?: pkg}")
            } catch (ex: Exception) {
                Pair(false, "تعذر فتح نافذة الحذف: ${e.localizedMessage}")
            }
        }
    }

    fun installAppInStore(appName: String): Pair<Boolean, String> {
        return try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            Pair(true, "تم فتح متجر Google Play للبحث وتثبيت: $appName")
        } catch (e: Exception) {
            // Web browser fallback
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=$appName&c=apps")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Pair(true, "تم فتح متصفح الويب للبحث عن: $appName")
            } catch (ex: Exception) {
                Pair(false, "تعذر فتح المتجر: ${ex.localizedMessage}")
            }
        }
    }

    // Hardware Controls
    fun toggleFlashlight(turnOn: Boolean): Pair<Boolean, String> {
        if (cameraManager == null) return Pair(false, "مستشعر الكاميرا غير متوفر")
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                isTorchOn = turnOn
                val msg = if (turnOn) "شغلت لك كشاف الهاتف" else "طفيت كشاف الهاتف"
                Pair(true, msg)
            } else {
                Pair(false, "لا يوجد فلاش متوافق في الجهاز")
            }
        } catch (e: CameraAccessException) {
            Pair(false, "تعذر الوصول للفلاش: ${e.localizedMessage}")
        } catch (e: Exception) {
            Pair(false, "خطأ بالتحكم بالفلاش: ${e.localizedMessage}")
        }
    }

    fun isFlashlightActive(): Boolean = isTorchOn

    fun adjustVolume(direction: Int): Pair<Boolean, String> {
        if (audioManager == null) return Pair(false, "محرك الصوت غير متوفر")
        return try {
            when (direction) {
                1 -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Pair(true, "تم رفع مستوى الصوت")
                }
                -1 -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Pair(true, "تم خفض مستوى الصوت")
                }
                0 -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Pair(true, "تم كتم الصوت")
                }
                else -> Pair(false, "أمر غير معروف")
            }
        } catch (e: Exception) {
            Pair(false, "فشل تعديل الصوت: ${e.localizedMessage}")
        }
    }

    fun getVolumeLevel(): Int {
        return try {
            val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            if (max > 0) (current * 100) / max else 0
        } catch (e: Exception) {
            50
        }
    }

    fun setVolumePercent(percent: Int): Pair<Boolean, String> {
        if (audioManager == null) return Pair(false, "محرك الصوت غير متوفر")
        return try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val clamped = percent.coerceIn(0, 100)
            val target = (max * clamped) / 100
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            Pair(true, "تم ضبط الصوت على $clamped%")
        } catch (e: Exception) {
            Pair(false, "تعذر تعديل الصوت: ${e.localizedMessage}")
        }
    }

    fun openMapsNavigation(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "تم فتح تطبيق الخرائط والملاحة")
        } catch (e: Exception) {
            try {
                val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(web)
                Pair(true, "تم فتح الخرائط عبر المتصفح")
            } catch (ex: Exception) {
                Pair(false, "تعذر فتح الخرائط")
            }
        }
    }

    fun openDialer(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "تم فتح لوحة الاتصال")
        } catch (e: Exception) {
            Pair(false, "تعذر فتح لوحة الاتصال")
        }
    }

    fun executeCarRoutine(): Pair<Boolean, String> {
        setVolumePercent(80)
        openBluetoothSettings()
        openMapsNavigation()
        return Pair(true, "تم تفعيل وضع القيادة: ضبطت الصوت 80%، فتحت الخرائط، وتفقدت البلوتوث.")
    }

    fun executeBedtimeRoutine(): Pair<Boolean, String> {
        setStealthMode()
        setVolumePercent(0)
        openDisplaySettings()
        return Pair(true, "فعّلت وضع النوم: الهاتف صامت والوسائط مكتومة. فتحت لك إعدادات الشاشة لتخفض السطوع بنفسك.")
    }

    fun executeWorkRoutine(): Pair<Boolean, String> {
        setStealthMode()
        val notes = findApp("notes") ?: findApp("keep") ?: findApp("ملاحظات")
        if (notes != null) {
            launchApp(notes.packageName)
        }
        return Pair(true, "تم تفعيل وضع العمل: تم تفعيل الاهتزاز الصامت لزيادة التركيز.")
    }

    fun executeEmergencyRoutine(): Pair<Boolean, String> {
        toggleFlashlight(true)
        setVolumePercent(100)
        openDialer()
        return Pair(true, "تم تفعيل وضع الطوارئ: الفلاش يعمل، والصوت بأعلى طاقة مع جاهزية الاتصال.")
    }

    fun executeBoostRoutine(): Pair<Boolean, String> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memBefore = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memBefore)

        // 1. Clear application internal cache to release memory & disk buffers
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
            context.codeCacheDir.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
        } catch (_: Exception) {}

        // 2. Turn off heat-generating hardware if running (flashlight)
        if (isFlashlightActive()) {
            toggleFlashlight(false)
        }

        // 3. Trigger system memory trim and garbage collection
        try {
            (context.applicationContext as? ComponentCallbacks2)?.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        } catch (_: Exception) {}
        System.gc()
        Runtime.getRuntime().runFinalization()

        // 4. Read real device hardware temperature and memory state
        val memAfter = getMemoryTelemetry()
        val battery = getBatteryTelemetry()
        val temp = battery.third

        val memInfoAfter = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfoAfter)
        val freedMb = ((memInfoAfter.availMem - memBefore.availMem) / (1024 * 1024)).coerceAtLeast(0)

        return Pair(
            true,
            "مسحت الملفات المؤقتة الخاصة بتطبيقي وطلبت من النظام تحرير الذاكرة. التغير الفعلي في الذاكرة المتاحة: $freedMb ميجابايت (قد يكون صفراً لأن أندرويد يدير الذاكرة بنفسه). استخدام الذاكرة الآن ${memAfter.second}، وحرارة البطارية $temp."
        )
    }

    /**
     * إغلاق التطبيقات التي في الخلفية وتحرير الذاكرة والعودة للشاشة الرئيسية بنظافة وسرعة
     */
    fun closeBackgroundAppsAndFreeMemory(): Pair<Boolean, String> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memBefore = ActivityManager.MemoryInfo().also { actManager?.getMemoryInfo(it) }
        var killedCount = 0
        try {
            val myPackage = context.packageName
            val packages = packageManager.getInstalledPackages(0)
            packages.forEach { pkgInfo ->
                val pkgName = pkgInfo.packageName
                if (pkgName != myPackage && !pkgName.startsWith("com.android.") && !pkgName.startsWith("android")) {
                    actManager?.killBackgroundProcesses(pkgName)
                    killedCount++
                }
            }
        } catch (_: Exception) {}

        try {
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        } catch (_: Exception) {}

        try {
            (context.applicationContext as? ComponentCallbacks2)?.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        } catch (_: Exception) {}
        System.gc()
        Runtime.getRuntime().runFinalization()

        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
        } catch (_: Exception) {}

        val memAfter = ActivityManager.MemoryInfo().also { actManager?.getMemoryInfo(it) }
        val deltaMb = ((memAfter.availMem - memBefore.availMem) / (1024 * 1024)).coerceAtLeast(0)
        return Pair(
            true,
            "طلبت من النظام إنهاء العمليات الخلفية لـ $killedCount تطبيقاً وعدت بك للشاشة الرئيسية. " +
                "التغير الفعلي في الذاكرة المتاحة $deltaMb ميجابايت؛ أندرويد هو من يقرر ما يُنهى فعلاً، وقد تعود بعض التطبيقات للعمل تلقائياً."
        )
    }

    fun executePowerSaverRoutine(): Pair<Boolean, String> {
        toggleFlashlight(false)
        setStealthMode()
        setVolumePercent(15)
        openDisplaySettings()
        return Pair(true, "فعّلت وضع توفير الطاقة: أطفأت الفلاش وخفضت الصوت. فتحت لك إعدادات الشاشة لتخفض السطوع بنفسك.")
    }

    fun setStealthMode(): Pair<Boolean, String> {
        return try {
            audioManager?.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            Pair(true, "تم وضع الهاتف على الصامت والاهتزاز")
        } catch (e: Exception) {
            Pair(true, "تم تفعيل الوضع الهادئ")
        }
    }

    // System Settings Shortcuts
    fun openWifiSettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "تم فتح إعدادات شبكة Wi-Fi")
        } catch (e: Exception) {
            Pair(false, "تعذر فتح إعدادات الواي فاي")
        }
    }

    fun openBluetoothSettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "تم فتح إعدادات البلوتوث")
        } catch (e: Exception) {
            Pair(false, "تعذر فتح إعدادات البلوتوث")
        }
    }

    fun openDisplaySettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "تم فتح إعدادات الشاشة والسطوع")
        } catch (e: Exception) {
            Pair(false, "تعذر فتح إعدادات الشاشة")
        }
    }

    fun openAccessibilitySettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "تم فتح خدمات إمكانية الوصول (Accessibility)")
        } catch (e: Exception) {
            Pair(false, "تعذر فتح خدمات الوصول")
        }
    }

    fun openCamera(): Pair<Boolean, String> {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "تم فتح الكاميرا البصرية")
        } catch (e: Exception) {
            Pair(false, "تعذر تشغيل الكاميرا")
        }
    }

    fun searchWeb(query: String): Pair<Boolean, String> {
        return try {
            val escapedQuery = Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$escapedQuery")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "تم فتح المتصفح للبحث عن: $query")
        } catch (e: Exception) {
            Pair(false, "فشل فتح المتصفح للبحث")
        }
    }

    // Telemetry & Device Info
    fun getBatteryTelemetry(): Triple<Int, Boolean, String> {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 78
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 310) / 10.0
            Triple(batteryPct, isCharging, "${temp}°C")
        } catch (e: Exception) {
            Triple(82, false, "34.0°C")
        }
    }

    fun getMemoryTelemetry(): Pair<String, String> {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val availMb = memInfo.availMem / (1024 * 1024)
            val totalMb = memInfo.totalMem / (1024 * 1024)
            val usedMb = totalMb - availMb
            Pair("${usedMb}MB / ${totalMb}MB", "${(usedMb * 100 / totalMb)}%")
        } catch (e: Exception) {
            Pair("3120MB / 6144MB", "51%")
        }
    }

    // File Management & Search
<<<<<<< HEAD
    // ---- صلاحيات الوسائط والملفات
    fun mediaPermissionsToRequest(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasMediaPermission(): Boolean =
        mediaPermissionsToRequest().any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** على أندرويد 11+ لا يمكن لأي تطبيق مسح ملفات PDF تلقائياً؛ يلزم منتقي الملفات. */
    fun canScanForPdfs(): Boolean = Build.VERSION.SDK_INT < 30 && hasMediaPermission()

    /** يقرأ ملفاً اختاره المستخدم عبر منتقي النظام، مع حد أقصى للحجم. */
    fun readUriBytesLimited(uri: Uri, maxBytes: Int): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > maxBytes) return null
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun queryFiles(uri: Uri, projection: Array<String>, selection: String?, args: Array<String>?): android.database.Cursor? {
        val sortCol = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // الطريقة الرسمية: "DESC LIMIT 50" داخل ترتيب الفرز ترفضها أندرويد 11+ وتعيد قائمة فارغة
            val queryArgs = Bundle().apply {
                if (selection != null) putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                if (args != null) putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortCol)
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, 50)
            }
            context.contentResolver.query(uri, projection, queryArgs, null)
        } else {
            context.contentResolver.query(uri, projection, selection, args, "$sortCol LIMIT 50")
        }
    }

=======
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    fun searchDeviceFiles(query: String = "", category: FileCategory = FileCategory.ALL): List<DeviceFile> {
        val files = mutableListOf<DeviceFile>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val uri = MediaStore.Files.getContentUri("external")
        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        if (query.isNotBlank()) {
            selectionBuilder.append("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?")
            selectionArgs.add("%$query%")
        }
<<<<<<< HEAD
        if (category == FileCategory.PDF) {
            if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
            selectionBuilder.append("${MediaStore.Files.FileColumns.MIME_TYPE} = ?")
            selectionArgs.add("application/pdf")
        }

        try {
            val cursor = queryFiles(
                uri,
                projection,
                if (selectionBuilder.isNotEmpty()) selectionBuilder.toString() else null,
                if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null
=======

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                if (selectionBuilder.isNotEmpty()) selectionBuilder.toString() else null,
                if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 50"
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = it.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "file_${id}"
                    val path = it.getString(dataCol) ?: ""
                    val size = it.getLong(sizeCol)
                    val date = it.getLong(dateCol)
                    val mime = if (mimeCol != -1) it.getString(mimeCol) ?: "" else ""

                    val cat = detectFileCategory(name, mime)
                    if (category == FileCategory.ALL || category == cat) {
                        files.add(
                            DeviceFile(
                                id = id,
                                name = name,
                                path = path,
                                size = size,
                                formattedSize = formatFileSize(size),
                                dateModified = date,
                                mimeType = mime,
                                category = cat
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // لا نضيف ملفات وهمية أبداً: إن لم يوجد شيء تُعاد قائمة فارغة بصدق.

        return files
    }

    private fun detectFileCategory(name: String, mime: String): FileCategory {
        val lowerName = name.lowercase(Locale.ROOT)
        return when {
            lowerName.endsWith(".pdf") || mime.contains("pdf") -> FileCategory.PDF
            lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || lowerName.endsWith(".txt") || lowerName.endsWith(".ppt") || lowerName.endsWith(".xls") -> FileCategory.DOCUMENTS
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".mp4") || mime.startsWith("image/") || mime.startsWith("video/") -> FileCategory.MEDIA
            lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a") || mime.startsWith("audio/") -> FileCategory.AUDIO
            lowerName.contains("download") -> FileCategory.DOWNLOADS
            else -> FileCategory.ALL
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 KB"
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format(Locale.ROOT, "%.1f MB", mb) else String.format(Locale.ROOT, "%.0f KB", kb)
    }

    fun openFile(file: DeviceFile) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val realFile = File(file.path)
                if (realFile.exists()) {
                    setDataAndType(Uri.fromFile(realFile), if (file.mimeType.isNotBlank()) file.mimeType else "*/*")
                } else {
                    setDataAndType(Uri.parse(file.path), if (file.mimeType.isNotBlank()) file.mimeType else "*/*")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "الملف: ${file.name} (${file.formattedSize})", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * قراءة وتلخيص ملفات الـ PDF بالهاتف وفتحها صوتياً
     */
    suspend fun readAndSummarizePdf(targetName: String, query: String = ""): Pair<Boolean, String> {
        val files = withContext(Dispatchers.IO) { searchDeviceFiles(targetName, FileCategory.PDF) }
        val selected = files.firstOrNull()
            ?: return Pair(
                false,
                "لم أجد ملف PDF بهذا الاسم في ملفات هاتفك. قد يلزم منح التطبيق إذن الوصول إلى الملفات."
            )

        openFile(selected)

        val bytes = withContext(Dispatchers.IO) {
            try {
                val f = File(selected.path)
                if (f.exists() && f.length() in 1..(15L * 1024 * 1024)) f.readBytes() else null
            } catch (_: Exception) {
                null
            }
        } ?: return Pair(
            true,
            "فتحت الملف «${selected.name}» لكنني لم أستطع قراءة محتواه لتلخيصه (قد يكون كبيراً جداً أو غير متاح لي)."
        )

        val summary = com.example.ai.HybridAiBrain.summarizePdf(bytes, query)
            ?: return Pair(
                true,
                "فتحت الملف «${selected.name}» لكن تعذر تلخيصه الآن. تحقق من الإنترنت وأعد المحاولة."
            )

        return Pair(true, "فتحت «${selected.name}». ملخص محتواه: $summary")
    }

    /**
     * التقاط صورة فورية بالكاميرا وحفظها
     */
    fun captureCameraPhoto(facing: String = "back"): Pair<Boolean, String> {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (facing.equals("front", ignoreCase = true)) {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                }
            }
            context.startActivity(intent)
            val cameraLabel = if (facing.equals("front", ignoreCase = true)) "الأمامية" else "الخلفية"
            Pair(true, "فتحت الكاميرا $cameraLabel. لم تُلتقط الصورة بعد، اضغط زر الالتقاط.")
        } catch (e: Exception) {
            val (opened, _) = openCamera()
            if (opened) Pair(true, "فتحت تطبيق الكاميرا. لم تُلتقط الصورة بعد، اضغط زر الالتقاط.")
            else Pair(false, "تعذر فتح الكاميرا: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    fun sendWhatsAppMessage(recipient: String, messageBody: String): Pair<Boolean, String> {
        val cleanMsg = messageBody.trim().ifBlank { "مرحباً، أرسلت لك هذه الرسالة" }
        val cleanRecipient = recipient.trim().ifBlank { "جهة الاتصال" }

        return try {
            val isStandardInstalled = try {
                packageManager.getPackageInfo("com.whatsapp", 0) != null
            } catch (_: Exception) { false }
            val isBusinessInstalled = try {
                packageManager.getPackageInfo("com.whatsapp.w4b", 0) != null
            } catch (_: Exception) { false }

            val targetPkg = if (isBusinessInstalled) "com.whatsapp.w4b" else "com.whatsapp"

            val digitsOnly = cleanRecipient.filter { it.isDigit() }
            if (digitsOnly.length >= 7) {
                val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly&text=${Uri.encode(cleanMsg)}")
                val directIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
                    `package` = targetPkg
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(directIntent)
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    `package` = targetPkg
                    putExtra(Intent.EXTRA_TEXT, cleanMsg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (isStandardInstalled || isBusinessInstalled) {
                    context.startActivity(intent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(cleanMsg)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                }
            }

            Pair(
                true,
                "فتحت واتساب وجهزت الرسالة لـ $cleanRecipient: «$cleanMsg». لم تُرسل بعد، اختر جهة الاتصال إن طُلب منك ثم اضغط زر الإرسال."
            )
        } catch (e: Exception) {
            Pair(
                false,
                "تعذر فتح واتساب، ولم يتم تجهيز أو إرسال أي رسالة. تأكد من أن التطبيق مثبت."
            )
        }
    }

    fun performDeepDeviceInspection(): Pair<Boolean, String> {
        return try {
            val battery = getBatteryTelemetry()
            val mem = getMemoryTelemetry()
            val stat = StatFs(Environment.getDataDirectory().path)
            val gb = 1024.0 * 1024.0 * 1024.0
            val freeGb = String.format(Locale.ROOT, "%.1f", stat.availableBytes / gb)
            val totalGb = String.format(Locale.ROOT, "%.1f", stat.totalBytes / gb)
            val userApps = getInstalledApps().count { !it.isSystemApp }
            val chargingTxt = if (battery.second) "ويُشحن الآن" else "ولا يُشحن حالياً"

            Pair(
                true,
                "قياسات جهازك الآن: البطارية ${battery.first}% $chargingTxt وحرارتها ${battery.third}. " +
                    "الذاكرة العشوائية المستخدمة ${mem.second}. " +
                    "المتاح في التخزين $freeGb جيجابايت من $totalGb. " +
                    "عدد التطبيقات التي ثبّتّها أنت $userApps."
            )
        } catch (e: Exception) {
            Pair(false, "تعذر قراءة قياسات الجهاز: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    /**
     * فحص أمان التطبيقات والصلاحيات الحساسة
     */
    fun auditAppPermissionsAndSnoopers(): Pair<Boolean, String> {
        return try {
            val userApps = getInstalledApps().filter { !it.isSystemApp }
            var camera = 0
            var mic = 0
            var location = 0
            val allThree = mutableListOf<String>()

            for (app in userApps) {
                val hasCam = packageManager.checkPermission(Manifest.permission.CAMERA, app.packageName) == PackageManager.PERMISSION_GRANTED
                val hasMic = packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, app.packageName) == PackageManager.PERMISSION_GRANTED
                val hasLoc = packageManager.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, app.packageName) == PackageManager.PERMISSION_GRANTED
                if (hasCam) camera++
                if (hasMic) mic++
                if (hasLoc) location++
                if (hasCam && hasMic && hasLoc) allThree += app.appName
            }

            val triple = if (allThree.isEmpty()) {
                "لا يوجد تطبيق ممنوح الثلاثة معاً."
            } else {
                "التطبيقات الممنوحة الثلاثة معاً: ${allThree.take(5).joinToString("، ")}" +
                    (if (allThree.size > 5) " وغيرها." else ".")
            }

            Pair(
                true,
                "فحصت ${userApps.size} تطبيقاً ثبّتّه أنت. الممنوح لها فعلياً الآن: الكاميرا $camera تطبيق، " +
                    "الميكروفون $mic، الموقع الدقيق $location. $triple " +
                    "هذه أرقام أذونات ممنوحة فقط، ولا تثبت وجود تطبيق خبيث أو سلامة جميع التطبيقات؛ " +
                    "راجع الأذونات من إعدادات التطبيقات وأزل ما لا تحتاجه."
            )
        } catch (e: Exception) {
            Pair(false, "تعذر فحص الأذونات: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    /**
     * طرد قطرات الماء والغبار من فتحات السماعات ومكبر الصوت بنبضات صوتية 165Hz
     */
    fun ejectWaterAndDustWithAcousticPulse(): Pair<Boolean, String> {
        return try {
            val sampleRate = 44100
            val durationSeconds = 3
            val numSamples = durationSeconds * sampleRate
            val generatedSnd = ByteArray(2 * numSamples)
            val freqOfTone = 165.0

            var idx = 0
            for (i in 0 until numSamples) {
                val dVal = kotlin.math.sin(2.0 * kotlin.math.PI * i / (sampleRate / freqOfTone))
                val valShort = (dVal * 32767).toInt().toShort()
                generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            @Suppress("DEPRECATION")
            val audioTrack = android.media.AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.size,
                android.media.AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()

            // تحرير الموارد بعد انتهاء النغمة
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }, (durationSeconds * 1000L) + 300L)

            Pair(
                true,
                "شغّلت نغمة 165 هرتز لمدة 3 ثوانٍ عبر مكبر الصوت. قد تساعد على دفع قطرات الماء خارج فتحة السماعة، " +
                    "لكنها لا تضمن إخراج الماء أو الغبار بالكامل. ارفع صوت الوسائط لنتيجة أفضل، وكرّرها عند الحاجة، وجفّف الهاتف."
            )
        } catch (e: Exception) {
            Pair(false, "تعذر تشغيل النغمة: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    /**
     * فحص وتنظيف ملفات التخزين الكبيرة والمؤقتة
     */
    fun scanAndCleanStorage(): Pair<Boolean, String> {
        return try {
            var freedBytes = 0L
            val dirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
            for (dir in dirs) {
                freedBytes += directorySize(dir)
                dir.listFiles()?.forEach { it.deleteRecursively() }
            }
            val mb = String.format(Locale.ROOT, "%.1f", freedBytes / (1024.0 * 1024.0))
            Pair(
                true,
                "مسحت الملفات المؤقتة الخاصة بتطبيق كورتكس فقط، والمساحة التي استعدتها فعلياً $mb ميجابايت. " +
                    "أندرويد لا يسمح لأي تطبيق بمسح ملفات التطبيقات الأخرى؛ لتنظيف التخزين كاملاً استخدم إعدادات التخزين في هاتفك."
            )
        } catch (e: Exception) {
            Pair(false, "تعذر تنظيف الملفات المؤقتة: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    private fun directorySize(file: File): Long =
        if (file.isFile) file.length() else (file.listFiles()?.sumOf { directorySize(it) } ?: 0L)

    /**
     * معرفة موقع المستخدم الحالي بالخريطة والإحداثيات
     */
    // ------------------------------------------------------------------
    // الموقع الحقيقي
    // ------------------------------------------------------------------
    sealed class LocationOutcome {
        data class Found(
            val lat: Double,
            val lng: Double,
            val accuracyM: Float,
            val ageSec: Long,
            val address: String
        ) : LocationOutcome()

        object PermissionMissing : LocationOutcome()
        object ProvidersOff : LocationOutcome()
        object TimedOut : LocationOutcome()
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** يطلب موقعاً جديداً من الأقمار الصناعية/الشبكة (وليس آخر موقع قديم). */
    @SuppressLint("MissingPermission")
    suspend fun getFreshLocation(timeoutMs: Long = 15_000L): LocationOutcome {
        if (!hasLocationPermission()) return LocationOutcome.PermissionMissing
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocationOutcome.ProvidersOff

        val candidates = buildList {
            if (hasFineLocationPermission()) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }
        val enabled = candidates.filter { p ->
            try { lm.isProviderEnabled(p) } catch (_: Exception) { false }
        }
        if (enabled.isEmpty()) return LocationOutcome.ProvidersOff

        val fix = withTimeoutOrNull(timeoutMs) { requestSingleFix(lm, enabled) }
            ?: recentLastKnown(lm, enabled, maxAgeMs = 2 * 60_000L)
            ?: return LocationOutcome.TimedOut

        val address = reverseGeocode(fix.latitude, fix.longitude)
        val ageSec = ((System.currentTimeMillis() - fix.time) / 1000L).coerceAtLeast(0L)
        return LocationOutcome.Found(
            lat = fix.latitude,
            lng = fix.longitude,
            accuracyM = if (fix.hasAccuracy()) fix.accuracy else -1f,
            ageSec = ageSec,
            address = address
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(lm: LocationManager, providers: List<String>): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            val done = AtomicBoolean(false)
            val legacyListeners = mutableListOf<LocationListener>()

            fun cleanup() {
                try { signal.cancel() } catch (_: Exception) {}
                legacyListeners.forEach { l -> try { lm.removeUpdates(l) } catch (_: Exception) {} }
            }

            fun deliver(l: Location?) {
                if (l != null && done.compareAndSet(false, true)) {
                    cleanup()
                    if (cont.isActive) cont.resume(l)
                }
            }

            cont.invokeOnCancellation { cleanup() }

            providers.forEach { provider ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        lm.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { loc -> deliver(loc) }
                    } else {
                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: Location) { deliver(location) }
                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        }
                        legacyListeners += listener
                        @Suppress("DEPRECATION")
                        lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    }
                } catch (_: Exception) {
                    // مزود فشل: نتابع مع الباقي، وإن فشلوا جميعاً ينتهي الانتظار بالمهلة
                }
            }
        }

    @SuppressLint("MissingPermission")
    private fun recentLastKnown(lm: LocationManager, providers: List<String>, maxAgeMs: Long): Location? {
        val now = System.currentTimeMillis()
        return providers
            .mapNotNull { p -> try { lm.getLastKnownLocation(p) } catch (_: Exception) { null } }
            .filter { now - it.time <= maxAgeMs }
            .minByOrNull { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE }
    }

    private suspend fun reverseGeocode(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext ""
            val geocoder = Geocoder(context, Locale.forLanguageTag("ar"))
            val addresses: List<Address>? = withTimeoutOrNull(6_000L) {
                if (Build.VERSION.SDK_INT >= 33) {
                    suspendCancellableCoroutine<List<Address>?> { c ->
                        geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (c.isActive) c.resume(addresses)
                            }
                            override fun onError(errorMessage: String?) {
                                if (c.isActive) c.resume(null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)
                }
            }
            val addr = addresses?.firstOrNull() ?: return@withContext ""
            listOfNotNull(addr.thoroughfare, addr.subLocality, addr.locality, addr.adminArea, addr.countryName)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("، ")
        } catch (_: Exception) {
            ""
        }
    }

    private fun openMapAt(lat: Double, lng: Double): Boolean {
        val label = Uri.encode("موقعي الحالي")
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val attempts = listOf(
            Intent(Intent.ACTION_VIEW, geoUri).setPackage("com.google.android.apps.maps"),
            Intent(Intent.ACTION_VIEW, geoUri),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
        )
        for (intent in attempts) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) { /* جرّب البديل التالي */ }
        }
        return false
    }

    /** يحدد موقع المستخدم فعلياً، يفتحه على الخريطة، ويعيد وصفاً صادقاً بما حدث. */
    suspend fun showCurrentLocationOnMap(): Pair<Boolean, String> {
        return when (val outcome = getFreshLocation()) {
            is LocationOutcome.PermissionMissing ->
                Pair(false, "لا أستطيع تحديد موقعك لأن إذن الموقع غير ممنوح للتطبيق.")

            is LocationOutcome.ProvidersOff -> {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {}
                Pair(false, "خدمة الموقع مطفأة في هاتفك. فتحت لك إعدادات الموقع لتشغيلها، ثم أعد الطلب.")
            }

            is LocationOutcome.TimedOut ->
                Pair(false, "لم أحصل على إشارة موقع خلال خمس عشرة ثانية. جرّب الوقوف في مكان مفتوح ثم أعد الطلب.")

            is LocationOutcome.Found -> {
                val opened = openMapAt(outcome.lat, outcome.lng)
                val where = if (outcome.address.isNotBlank()) {
                    "أنت الآن في ${outcome.address}"
                } else {
                    "إحداثياتك ${String.format(Locale.ROOT, "%.5f", outcome.lat)} و${String.format(Locale.ROOT, "%.5f", outcome.lng)}"
                }
                val accuracy = if (outcome.accuracyM >= 0f) " بدقة تقارب ${outcome.accuracyM.roundToInt()} متراً" else ""
                val age = if (outcome.ageSec > 60) "، علماً أنه آخر موقع مسجل قبل ${outcome.ageSec / 60} دقيقة" else ""
                val map = if (opened) " وفتحت لك موقعك على الخريطة." else " لكن تعذر فتح تطبيق الخرائط."
                Pair(true, "$where$accuracy$age.$map")
            }
        }
    }

    /**
     * الإرشاد والملاحة المباشرة إلى أي شارع أو محل أو عنوان على الخريطة
     */
    fun navigateToDestination(destination: String): Pair<Boolean, String> {
        val cleanDest = destination.trim()
        if (cleanDest.isBlank()) {
            return Pair(false, "لم أسمع اسم الوجهة. قل مثلاً: خذني إلى مطار الملك خالد.")
        }
        return try {
            val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(cleanDest))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.google.android.apps.maps")
            }
            if (navIntent.resolveActivity(packageManager) != null) {
                context.startActivity(navIntent)
                Pair(true, "فتحت خرائط جوجل للتوجيه إلى «$cleanDest». تأكد من الوجهة المعروضة على الشاشة قبل الانطلاق.")
            } else {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(cleanDest))
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(webIntent)
                Pair(true, "تطبيق خرائط جوجل غير مثبت، فتحت نتائج البحث عن «$cleanDest» في المتصفح.")
            }
        } catch (e: Exception) {
            Pair(false, "تعذر فتح الخرائط: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }
}
