package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * خدمة إمكانية الوصول الاختيارية: تتيح لكورتكس قراءة نص الشاشة والضغط والكتابة داخل أي تطبيق
 * بأمر صوتي من المستخدم فقط، وبعد تأكيده للأفعال التي تغيّر شيئاً.
 *
 * ضمانات أمان مدمجة:
 * - لا تقرأ ولا تكتب في حقول كلمات المرور أبداً.
 * - لا تنفذ ضغطاً أو كتابة داخل شاشات النظام الحساسة (الإعدادات، الأذونات، تثبيت/حذف التطبيقات)،
 *   حتى لا يستطيع المساعد منح نفسه صلاحيات أو تعطيل حمايات الهاتف.
 * - لا تتجسس في الخلفية: لا تعالج أحداث الشاشة، وتعمل فقط عند استدعاء المستخدم لها.
 */
class CortexAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: CortexAccessibilityService? = null

        private val PROTECTED_PACKAGES = setOf(
            "com.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.samsung.android.permissioncontroller",
            "com.samsung.android.packageinstaller"
        )

        fun isServiceEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val component = ComponentName(context, CortexAccessibilityService::class.java)
            val full = component.flattenToString()
            val short = component.flattenToShortString()
            return enabled.split(':').any { it.equals(full, true) || it.equals(short, true) }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // لا نعالج الأحداث عمداً: الخدمة تعمل عند الطلب فقط.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun foregroundPackage(): String? = rootInActiveWindow?.packageName?.toString()

    fun isProtectedForeground(): Boolean = foregroundPackage() in PROTECTED_PACKAGES

    /** نصوص الشاشة الظاهرة الآن، بدون حقول كلمات المرور. */
    fun readScreenText(maxItems: Int = 60): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val out = LinkedHashSet<String>()

        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || out.size >= maxItems || depth > 30) return
            if (node.isVisibleToUser && !node.isPassword) {
                val t = (node.text ?: node.contentDescription)?.toString()?.trim().orEmpty()
                if (t.isNotBlank()) out.add(t.take(120))
            }
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
        }

        walk(root, 0)
        return out.toList()
    }

    private fun visibleMatches(label: String): List<AccessibilityNodeInfo> {
        val root = rootInActiveWindow ?: return emptyList()
        return root.findAccessibilityNodeInfosByText(label)
            .filter { it.isVisibleToUser && !it.isPassword }
    }

    private fun clickableTarget(label: String): AccessibilityNodeInfo? {
        val candidates = visibleMatches(label)
        val wanted = label.trim()
        val exact = candidates.firstOrNull {
            (it.text ?: it.contentDescription)?.toString()?.trim().equals(wanted, ignoreCase = true)
        }
        var cur: AccessibilityNodeInfo? = exact ?: candidates.firstOrNull()
        var hops = 0
        while (cur != null && hops < 6) {
            if (cur.isClickable && cur.isEnabled) return cur
            cur = cur.parent
            hops++
        }
        return null
    }

    /** عدد العناصر الظاهرة المطابقة لهذا النص والقابلة للضغط (مباشرة أو عبر أبيها). */
    fun matchCount(label: String): Int =
        if (clickableTarget(label) == null) 0 else visibleMatches(label).size.coerceAtLeast(1)

    fun clickByText(label: String): Boolean =
        clickableTarget(label)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    fun hasEditableFocus(): Boolean {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return node.isEditable && !node.isPassword
    }

    /** يضيف النص إلى الحقل المحدد حالياً. يرفض حقول كلمات المرور. */
    fun typeIntoFocused(text: String): Boolean {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        if (!node.isEditable || node.isPassword) return false
        val showingHint = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && node.isShowingHintText
        val existing = if (showingHint) "" else node.text?.toString().orEmpty()
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, existing + text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun pressSystemButton(name: String): Boolean = when (name.lowercase()) {
        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        "quick_settings" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        else -> false
    }

    private fun findScrollable(node: AccessibilityNodeInfo?, depth: Int = 0): AccessibilityNodeInfo? {
        if (node == null || depth > 30) return null
        if (node.isScrollable && node.isVisibleToUser) return node
        for (i in 0 until node.childCount) {
            findScrollable(node.getChild(i), depth + 1)?.let { return it }
        }
        return null
    }

    fun scroll(down: Boolean): Boolean {
        val node = findScrollable(rootInActiveWindow) ?: return false
        return node.performAction(
            if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        )
    }
}
