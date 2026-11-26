package com.example.wallet

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ExpenseCategory
import com.example.model.VoiceExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Intelligent Expense & Wallet Manager (مسجل المصاريف والمحفظة الصوتية الفورية)
 * Stores transactions securely on device with sub-second voice readback and aggregation.
 */
class ExpenseManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "cortex_wallet_prefs"
        private const val KEY_MONTHLY_BUDGET = "key_monthly_budget"
        private const val FILE_NAME = "expenses_vault.json"

        @Volatile
        private var instance: ExpenseManager? = null

        fun getInstance(context: Context): ExpenseManager {
            return instance ?: synchronized(this) {
                instance ?: ExpenseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _expenses = MutableStateFlow<List<VoiceExpense>>(emptyList())
    val expenses: StateFlow<List<VoiceExpense>> = _expenses.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(prefs.getFloat(KEY_MONTHLY_BUDGET, 5000.0f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    init {
        loadExpenses()
    }

    @Synchronized
    private fun loadExpenses() {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            _expenses.value = emptyList()
            return
        }

        try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<VoiceExpense>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val catStr = obj.optString("category", ExpenseCategory.GENERAL.name)
                val cat = try {
                    ExpenseCategory.valueOf(catStr)
                } catch (_: Exception) {
                    ExpenseCategory.GENERAL
                }

                list.add(
                    VoiceExpense(
                        id = obj.getString("id"),
                        amount = obj.getDouble("amount"),
                        currency = obj.optString("currency", "ريال"),
                        category = cat,
                        description = obj.optString("description", ""),
                        timestamp = obj.getLong("timestamp"),
                        formattedDate = obj.optString("formattedDate", ""),
                        formattedTime = obj.optString("formattedTime", "")
                    )
                )
            }
            _expenses.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            _expenses.value = emptyList()
        }
    }

    @Synchronized
    private fun saveExpenses() {
        try {
            val jsonArray = JSONArray()
            for (expense in _expenses.value) {
                val obj = JSONObject().apply {
                    put("id", expense.id)
                    put("amount", expense.amount)
                    put("currency", expense.currency)
                    put("category", expense.category.name)
                    put("description", expense.description)
                    put("timestamp", expense.timestamp)
                    put("formattedDate", expense.formattedDate)
                    put("formattedTime", expense.formattedTime)
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addExpense(
        amount: Double,
        category: ExpenseCategory,
        description: String,
        currency: String = "ريال"
    ): VoiceExpense {
        val now = Date()
        val item = VoiceExpense(
            amount = amount,
            currency = currency,
            category = category,
            description = description.ifBlank { category.labelAr },
            timestamp = now.time,
            formattedDate = dateFormat.format(now),
            formattedTime = timeFormat.format(now)
        )

        val updated = mutableListOf(item)
        updated.addAll(_expenses.value)
        _expenses.value = updated
        saveExpenses()
        return item
    }

    fun deleteExpense(id: String): Boolean {
        val current = _expenses.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            _expenses.value = current
            saveExpenses()
        }
        return removed
    }

    fun setMonthlyBudget(newBudget: Double) {
        _monthlyBudget.value = newBudget
        prefs.edit().putFloat(KEY_MONTHLY_BUDGET, newBudget.toFloat()).apply()
    }

    fun getTodayTotal(): Double {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return _expenses.value
            .filter { it.timestamp >= startOfToday }
            .sumOf { it.amount }
    }

    fun getWeekTotal(): Double {
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return _expenses.value
            .filter { it.timestamp >= startOfWeek }
            .sumOf { it.amount }
    }

    fun getMonthTotal(): Double {
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return _expenses.value
            .filter { it.timestamp >= startOfMonth }
            .sumOf { it.amount }
    }

    fun getRemainingBudget(): Double {
        val remaining = _monthlyBudget.value - getMonthTotal()
        return if (remaining < 0) 0.0 else remaining
    }

    fun getTodayExpensesList(): List<VoiceExpense> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return _expenses.value.filter { it.timestamp >= startOfToday }
    }

    /**
     * Formats an authentic, human-like voice readback for expenses.
     */
    fun getSpeechSummary(period: String = "today"): String {
        return when (period.lowercase()) {
            "week", "الاسبوع", "أسبوع" -> {
                val total = getWeekTotal()
                val totalStr = formatAmount(total)
                val remaining = getRemainingBudget()
                val remainingStr = formatAmount(remaining)
                if (total <= 0.0) {
                    "لم تسجل أي مصاريف خلال هذا الأسبوع حتى الآن، والمتبقي من ميزانيتك الشهرية هو $remainingStr ريال. هل تطلب مني شيء آخر يا غالي؟"
                } else {
                    "مجموع ما صرفته خلال هذا الأسبوع هو $totalStr ريال. والمتبقي من ميزانيتك لهذا الشهر هو $remainingStr ريال. هل تطلب مني شيء آخر يا غالي؟"
                }
            }
            "month", "الشهر", "شهر" -> {
                val total = getMonthTotal()
                val totalStr = formatAmount(total)
                val remaining = getRemainingBudget()
                val remainingStr = formatAmount(remaining)
                if (total <= 0.0) {
                    "لم تسجل أي مصاريف في هذا الشهر بعد، ميزانيتك كاملة ومقدارها $remainingStr ريال. هل تطلب مني شيء آخر يا غالي؟"
                } else {
                    "مجموع مصاريفك لهذا الشهر هو $totalStr ريال، والمتبقي من ميزانيتك المحددة هو $remainingStr ريال. هل تطلب مني شيء آخر يا غالي؟"
                }
            }
            else -> { // Today
                val todayItems = getTodayExpensesList()
                val total = getTodayTotal()
                val totalStr = formatAmount(total)
                val remaining = getRemainingBudget()
                val remainingStr = formatAmount(remaining)

                if (todayItems.isEmpty() || total <= 0.0) {
                    "لم تسجل أي مصروف اليوم حتى الآن يا غالي، والمتبقي من ميزانيتك لهذا الشهر هو $remainingStr ريال. هل تطلب مني تسجيل أي مبلغ الآن؟"
                } else {
                    // Group breakdown
                    val breakdown = todayItems.groupBy { it.category }
                        .entries
                        .joinToString("، و") { entry ->
                            "${formatAmount(entry.value.sumOf { it.amount })} ريال ${entry.key.labelAr}"
                        }
                    "مجموع ما صرفته اليوم هو $totalStr ريال ($breakdown)، والمتبقي من ميزانيتك لهذا الشهر هو $remainingStr ريال. هل تطلب مني شيء آخر يا غالي؟"
                }
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.1f", amount)
        }
    }
}
