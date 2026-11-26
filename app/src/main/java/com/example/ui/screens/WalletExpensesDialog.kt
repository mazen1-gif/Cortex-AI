package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ExpenseCategory
import com.example.model.VoiceExpense
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.HologramTextMuted
import com.example.ui.theme.HologramTextPrimary
import com.example.ui.theme.HologramTextSecondary
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WalletExpensesDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val isArabic by viewModel.isArabicVoice.collectAsState()

    val todayTotal = remember(expenses) { viewModel.expenseManager.getTodayTotal() }
    val weekTotal = remember(expenses) { viewModel.expenseManager.getWeekTotal() }
    val monthTotal = remember(expenses) { viewModel.expenseManager.getMonthTotal() }
    val remainingBudget = remember(expenses, monthlyBudget) { viewModel.expenseManager.getRemainingBudget() }

    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(18.dp))
                .background(CyberDarkBg)
                .border(1.dp, MatrixGreen.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(16.dp)
                .testTag("wallet_expenses_dialog")
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MatrixGreen.copy(alpha = 0.15f))
                                .border(1.dp, MatrixGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "المحفظة",
                                tint = MatrixGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "محفظتي وسجل المصاريف" else "WALLET & EXPENSES",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 13.sp,
                                color = MatrixGreen
                            )
                            Text(
                                text = if (isArabic) "تسجيل فوري بصوتك وحسابات دقيقة" else "Instant voice expense tracking",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_wallet_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = HologramTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Row (Today, Week, Month)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMetricCard(
                        title = if (isArabic) "اليوم" else "Today",
                        amount = todayTotal,
                        accent = MatrixGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = if (isArabic) "هذا الأسبوع" else "This Week",
                        amount = weekTotal,
                        accent = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = if (isArabic) "هذا الشهر" else "This Month",
                        amount = monthTotal,
                        accent = NeonYellow,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly Budget Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isArabic) "الميزانية الشهرية:" else "Monthly Budget:",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 11.sp,
                                    color = HologramTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${monthlyBudget.toLong()} ريال",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NeonCyan
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberSurfaceVariant)
                                    .clickable { showBudgetDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل الميزانية",
                                    tint = HologramTextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) "تعديل" else "Edit",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 10.sp,
                                    color = HologramTextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val progress = if (monthlyBudget > 0) (monthTotal / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
                        val progressColor = when {
                            progress >= 0.9f -> NeonMagenta
                            progress >= 0.7f -> NeonYellow
                            else -> MatrixGreen
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = CyberSurfaceBorder,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isArabic) "المتبقي: ${remainingBudget.toLong()} ريال" else "Remaining: ${remainingBudget.toLong()} SAR",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (remainingBudget > 0) MatrixGreen else NeonMagenta
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% من الميزانية",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = HologramTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row (Voice Read + Manual Add)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.processVoiceCommand("كم صرفت اليوم")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MatrixGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("read_expenses_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "قراءة المصاريف",
                            tint = MatrixGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "اسمع ملخص اليوم" else "Read Today",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MatrixGreen
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("add_expense_manual_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة مصروف",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "إضافة مصروف" else "Add Expense",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Voice Prompt Guide Hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
<<<<<<< HEAD
                        text = if (isArabic) "يمكنك القول في أي وقت: «سجل صرفت 500 ريال تسوق» أو «حط 100 بنزين» أو «كم صرفت اليوم؟»"
                        else "Say anytime: 'Record 500 shopping' or 'Put 100 fuel' or 'How much spent today?'",
=======
                        text = if (isArabic) "💡 يمكنك القول في أي وقت: «سجل صرفت 500 ريال تسوق» أو «حط 100 بنزين» أو «كم صرفت اليوم؟»"
                        else "💡 Say anytime: 'Record 500 shopping' or 'Put 100 fuel' or 'How much spent today?'",
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 11.sp,
                        color = HologramTextSecondary,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transactions List
                Text(
                    text = if (isArabic) "سجل العمليات والمشتريات (${expenses.size}):" else "TRANSACTIONS (${expenses.size}):",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = HologramTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (expenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "💰", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "لا توجد مصاريف مسجلة حتى الآن" else "No expenses logged yet",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 12.sp,
                                color = HologramTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "اضغط المايكروفون وقل: «سجل صرفت 50 ريال مطعم»" else "Hold mic and say: 'Log 50 dinner'",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = MatrixGreen
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(expenses, key = { it.id }) { expense ->
                            ExpenseItemRow(
                                expense = expense,
                                isArabic = isArabic,
                                onDelete = { viewModel.deleteExpenseDirect(expense.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            isArabic = isArabic,
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, category, desc ->
                viewModel.addExpenseDirect(amount, category, desc)
                showAddDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        EditBudgetDialog(
            currentBudget = monthlyBudget,
            isArabic = isArabic,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { newBudget ->
                viewModel.setBudgetDirect(newBudget)
                showBudgetDialog = false
            }
        )
    }
}

@Composable
fun SummaryMetricCard(
    title: String,
    amount: Double,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                fontFamily = RajdhaniFontFamily,
                fontSize = 11.sp,
                color = HologramTextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (amount % 1.0 == 0.0) "${amount.toLong()}" else String.format(java.util.Locale.ROOT, "%.1f", amount),
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = accent
            )
            Text(
                text = "ريال",
                fontFamily = RajdhaniFontFamily,
                fontSize = 10.sp,
                color = HologramTextSecondary
            )
        }
    }
}

@Composable
fun ExpenseItemRow(
    expense: VoiceExpense,
    isArabic: Boolean,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = expense.category.emoji, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = expense.description.ifBlank { if (isArabic) expense.category.labelAr else expense.category.labelEn },
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = HologramTextPrimary
                    )
                    Text(
                        text = "${expense.formattedDate} • ${expense.formattedTime}",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 10.sp,
                        color = HologramTextMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "- ${if (expense.amount % 1.0 == 0.0) expense.amount.toLong() else expense.amount} ${expense.currency}",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NeonYellow
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف العملية",
                        tint = HologramTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, ExpenseCategory, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.GENERAL) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDarkBg)
                .border(1.dp, NeonCyan, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = if (isArabic) "إضافة مصروف جديد" else "ADD NEW EXPENSE",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 13.sp,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isArabic) "المبلغ (ريال)" else "Amount (SAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isArabic) "الفئة:" else "Category:",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = HologramTextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ExpenseCategory.values()) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MatrixGreen.copy(alpha = 0.25f) else CyberSurface)
                                .border(1.dp, if (isSelected) MatrixGreen else CyberSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${cat.emoji} ${if (isArabic) cat.labelAr else cat.labelEn}",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = if (isSelected) MatrixGreen else HologramTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text(if (isArabic) "الوصف (اختياري، مثلاً: بقالة الحي)" else "Description (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface)
                    ) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = HologramTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt, selectedCategory, descText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen),
                        modifier = Modifier.testTag("confirm_add_expense_button")
                    ) {
                        Text(if (isArabic) "حفظ المصروف" else "Save", color = CyberDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditBudgetDialog(
    currentBudget: Double,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget.toLong().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDarkBg)
                .border(1.dp, NeonYellow, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = if (isArabic) "تحديد الميزانية الشهرية" else "SET MONTHLY BUDGET",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 13.sp,
                    color = NeonYellow
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text(if (isArabic) "الميزانية الشهرية (ريال)" else "Budget (SAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonYellow,
                        unfocusedBorderColor = CyberSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface)
                    ) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = HologramTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newB = budgetText.toDoubleOrNull() ?: currentBudget
                            if (newB > 0) {
                                onConfirm(newB)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow)
                    ) {
                        Text(if (isArabic) "تحديث" else "Update", color = CyberDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
