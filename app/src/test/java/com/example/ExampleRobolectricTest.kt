package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Cortex", appName)
  }

  @Test
  fun `verify voice expense tracking parsing`() {
    val res1 = com.example.engine.IntentParser.parse("سجل صرفت 500 ريال تسوق")
    assertEquals(com.example.model.ParsedAction.ADD_EXPENSE, res1.action)
    assertEquals("500.0", res1.targetParameter)
    assertEquals(com.example.model.ExpenseCategory.SHOPPING.name, res1.secondaryParameter)

    val res2 = com.example.engine.IntentParser.parse("حط 100 ريال بنزين")
    assertEquals(com.example.model.ParsedAction.ADD_EXPENSE, res2.action)
    assertEquals("100.0", res2.targetParameter)
    assertEquals(com.example.model.ExpenseCategory.FUEL.name, res2.secondaryParameter)

    val res3 = com.example.engine.IntentParser.parse("كم صرفت اليوم؟")
    assertEquals(com.example.model.ParsedAction.QUERY_EXPENSES, res3.action)
    assertEquals("today", res3.targetParameter)

    val res4 = com.example.engine.IntentParser.parse("احسب لي مصاريف الأسبوع")
    assertEquals(com.example.model.ParsedAction.QUERY_EXPENSES, res4.action)
    assertEquals("week", res4.targetParameter)

    val res5 = com.example.engine.IntentParser.parse("اغلق التطبيقات التي في الخلفية")
    assertEquals(com.example.model.ParsedAction.CLOSE_BACKGROUND_APPS, res5.action)

    val res6 = com.example.engine.IntentParser.parse("وريني صور المتلصصين")
    assertEquals(com.example.model.ParsedAction.VIEW_INTRUDERS, res6.action)

    val res7 = com.example.engine.IntentParser.parse("افتح ملف الـ PDF واقرأ لي ملخصه")
    assertEquals(com.example.model.ParsedAction.READ_PDF, res7.action)

    val res8 = com.example.engine.IntentParser.parse("التقط لي صورة بالكاميرا")
    assertEquals(com.example.model.ParsedAction.CAPTURE_PHOTO, res8.action)

    val res9 = com.example.engine.IntentParser.parse("افحص أمان التطبيقات")
    assertEquals(com.example.model.ParsedAction.AUDIT_SECURITY, res9.action)

    val res10 = com.example.engine.IntentParser.parse("طرد الماء من السماعة")
    assertEquals(com.example.model.ParsedAction.WATER_EJECT, res10.action)

    val res11 = com.example.engine.IntentParser.parse("نظف التخزين ومسح المهملات")
    assertEquals(com.example.model.ParsedAction.CLEAN_STORAGE, res11.action)

    val res12 = com.example.engine.IntentParser.parse("وين مكاني الحين في الخريطة")
    assertEquals(com.example.model.ParsedAction.CURRENT_LOCATION, res12.action)

    val res13 = com.example.engine.IntentParser.parse("اشتيك تدلنا وين شارع الزيري محل كذا")
    assertEquals(com.example.model.ParsedAction.NAVIGATE_TO, res13.action)
    assert(res13.targetParameter.contains("شارع الزيري") || res13.targetParameter.contains("محل كذا"))
  }

  @Test
  fun `verify ExpenseManager calculations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.wallet.ExpenseManager.getInstance(context)

    // Clear existing
    manager.expenses.value.forEach { manager.deleteExpense(it.id) }
    manager.setMonthlyBudget(5000.0)

    manager.addExpense(500.0, com.example.model.ExpenseCategory.SHOPPING, "تسوق")
    manager.addExpense(100.0, com.example.model.ExpenseCategory.FUEL, "بنزين")

    assertEquals(600.0, manager.getTodayTotal(), 0.01)
    assertEquals(4400.0, manager.getRemainingBudget(), 0.01)

    val summary = manager.getSpeechSummary("today")
    assert(summary.contains("600"))
    assert(summary.contains("4400"))
  }
}
