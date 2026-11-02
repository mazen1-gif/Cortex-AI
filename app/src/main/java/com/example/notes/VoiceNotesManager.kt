package com.example.notes

import android.content.Context
import android.content.SharedPreferences
import com.example.model.NoteType
import com.example.model.VoiceNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مذكرات الأفكار والمهام الفورية بالصوت (Voice Memos & Action Tasks)
 * حفظ وتنظيم الملاحظات والأفكار والمهام الصوتية مع التاريخ والوقت تلقائياً.
 * قراءتها كاملة بصوت المساعد عند الطلب.
 */
class VoiceNotesManager(private val context: Context) {

    companion object {
        const val PREFS_NAME = "cortex_voice_notes_prefs"
        const val KEY_NOTES_JSON = "saved_voice_notes_json"

        @Volatile
        private var INSTANCE: VoiceNotesManager? = null

        fun getInstance(context: Context): VoiceNotesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VoiceNotesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _notes = MutableStateFlow<List<VoiceNote>>(emptyList())
    val notes: StateFlow<List<VoiceNote>> = _notes.asStateFlow()

    init {
        loadNotes()
    }

    private fun loadNotes() {
        val jsonStr = prefs.getString(KEY_NOTES_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            // Seed a helpful welcome note
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val initial = listOf(
                VoiceNote(
                    text = "الاجتماع غداً الساعة 4 عصراً لمناقشة خطة العمل",
                    type = NoteType.TASK,
                    timestamp = now,
                    formattedDate = dateFormat.format(Date(now)),
                    formattedTime = timeFormat.format(Date(now))
                ),
                VoiceNote(
                    text = "فكرة شراء بطارية جديدة وحافظة حماية للهاتف",
                    type = NoteType.IDEA,
                    timestamp = now - 3600000,
                    formattedDate = dateFormat.format(Date(now - 3600000)),
                    formattedTime = timeFormat.format(Date(now - 3600000))
                )
            )
            _notes.value = initial
            saveNotesToDisk(initial)
            return
        }

        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<VoiceNote>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val text = obj.optString("text", "")
                val typeStr = obj.optString("type", NoteType.NOTE.name)
                val type = try { NoteType.valueOf(typeStr) } catch (_: Exception) { NoteType.NOTE }
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val date = obj.optString("date", "")
                val time = obj.optString("time", "")

                list.add(
                    VoiceNote(
                        id = if (id.isNotEmpty()) id else java.util.UUID.randomUUID().toString(),
                        text = text,
                        type = type,
                        timestamp = timestamp,
                        formattedDate = date,
                        formattedTime = time
                    )
                )
            }
            _notes.value = list
        } catch (_: Exception) {
            _notes.value = emptyList()
        }
    }

    private fun saveNotesToDisk(list: List<VoiceNote>) {
        try {
            val jsonArray = JSONArray()
            for (note in list) {
                val obj = JSONObject().apply {
                    put("id", note.id)
                    put("text", note.text)
                    put("type", note.type.name)
                    put("timestamp", note.timestamp)
                    put("date", note.formattedDate)
                    put("time", note.formattedTime)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_NOTES_JSON, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    /**
     * حفظ ملاحظة أو فكرة جديدة فوراً مع التاريخ والوقت
     */
    fun addNote(text: String, type: NoteType = NoteType.NOTE): VoiceNote {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        val newNote = VoiceNote(
            text = text.trim(),
            type = type,
            timestamp = now,
            formattedDate = dateFormat.format(Date(now)),
            formattedTime = timeFormat.format(Date(now))
        )

        val updated = listOf(newNote) + _notes.value
        _notes.value = updated
        saveNotesToDisk(updated)
        return newNote
    }

    /**
     * حذف ملاحظة
     */
    fun deleteNote(id: String) {
        val updated = _notes.value.filter { it.id != id }
        _notes.value = updated
        saveNotesToDisk(updated)
    }

    /**
     * إعداد الملخص الصوتي الكامل لقراءة الملاحظات بصوت طبيعي
     */
    fun getSpeechSummary(todayOnly: Boolean = false): String {
        val currentNotes = _notes.value
        if (currentNotes.isEmpty()) {
            return "ليس لديك أي ملاحظات أو أفكار مسجلة حتى الآن."
        }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        val targetNotes = if (todayOnly) {
            currentNotes.filter { it.formattedDate == todayStr }
        } else {
            currentNotes
        }

        if (targetNotes.isEmpty()) {
            return if (todayOnly) "لم تسجل أي ملاحظات جديدة اليوم، ولكن لديك ${currentNotes.size} ملاحظات مسجلة سابقاً."
            else "ليس لديك أي ملاحظات مسجلة."
        }

        val sb = java.lang.StringBuilder()
        sb.append("لديك ${targetNotes.size} ")
        sb.append(if (todayOnly) "ملاحظات مسجلة اليوم: " else "ملاحظات مسجلة: ")

        targetNotes.take(6).forEachIndexed { index, note ->
            val prefix = when (index) {
                0 -> "الأولى: "
                1 -> "، الثانية: "
                2 -> "، الثالثة: "
                3 -> "، الرابعة: "
                4 -> "، الخامسة: "
                else -> "، وأخيراً: "
            }
            val typeLabel = when (note.type) {
                NoteType.IDEA -> "فكرة "
                NoteType.TASK -> "مهمة "
                NoteType.NOTE -> "ملاحظة "
            }
            sb.append("$prefix$typeLabel${note.text}")
        }

        if (targetNotes.size > 6) {
            sb.append("، وهناك ${targetNotes.size - 6} ملاحظات إضافية يمكنك استعراضها في قائمة الملاحظات.")
        }

        return sb.toString()
    }
}
