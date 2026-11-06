package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.theme.ChiliRed
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.GoldenCrisp
import com.example.ui.theme.OrangePrimary
import java.util.Locale

@Composable
fun AnalyticsView(viewModel: MainViewModel) {
    val summary = remember(viewModel.orders, viewModel.hiddenCosts) {
        viewModel.repository.calculateBreakEvenSummary()
    }
    val hiddenCosts by viewModel.hiddenCosts.collectAsState()
    var isAddCostDialogOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Break-even Analysis Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = OrangePrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "วิเคราะห์จุดคุ้มทุน (Break-even Point)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ต้องขายอย่างน้อย:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${summary.breakEvenUnitsNeeded}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangePrimary
                        )
                        Text(
                            text = " ไม้ / เดือน เพื่อครอบคลุมต้นทุนแฝงทั้งหมด",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar towards break-even
                    val pct = (summary.progressToBreakEvenPercent * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "ขายแล้ว ${summary.totalUnitsSold} ไม้", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "$pct% สู่จุดคุ้มทุน", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pct >= 100) CucumberGreen else OrangePrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { summary.progressToBreakEvenPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (pct >= 100) CucumberGreen else OrangePrimary,
                        trackColor = Color(0xFFEEEEEE)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Metric Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricSmallBox(title = "ราคาขายเฉลี่ย/ไม้", value = "฿${String.format(Locale.US, "%.1f", summary.averageSellingPricePerUnit)}")
                        MetricSmallBox(title = "ต้นทุนตรง/ไม้", value = "฿${String.format(Locale.US, "%.1f", summary.averageDirectCostPerUnit)}")
                        MetricSmallBox(title = "กำไรส่วนเกิน/ไม้", value = "฿${String.format(Locale.US, "%.1f", summary.averageContributionMarginPerUnit)}", highlight = true)
                    }
                }
            }
        }

        // 2. Gas Tank Cycle Tracker (Operating Cost)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🔥", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "การใช้แก๊สหุงต้ม (Gas Tank Usage Cycle)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "คำนวณอัตโนมัติตามรอบออเดอร์ทอด", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "แก๊สคงเหลือประมาณ:", fontSize = 13.sp)
                        Text(
                            text = "${summary.gasTankRemainingPercent}% (ใช้ได้อีก ~${summary.estimatedGasDaysRemaining} วัน)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (summary.gasTankRemainingPercent < 25) ChiliRed else CucumberGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { summary.gasTankRemainingPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (summary.gasTankRemainingPercent < 25) ChiliRed else GoldenCrisp,
                        trackColor = Color(0xFFEEEEEE)
                    )
                }
            }
        }

        // 3. Smart Advice Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenCrisp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "คำแนะนำอัจฉริยะ (Smart Advice)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OrangePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    summary.smartAdvices.forEach { advice ->
                        Text(
                            text = advice,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 3b. Cost breakdown per item
        item {
            val menuItems by viewModel.menuItems.collectAsState()
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ต้นทุนหลัก (แยกตามรายการ)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    menuItems.forEach { m ->
                        val unit = if (m.packagePrice > 0 && m.piecesPerPackage > 0) m.packagePrice / m.piecesPerPackage else m.directCost
                        val profit = m.price - unit
                        Text(
                            text = "${m.emoji} ${m.name} • ถุง ฿${m.packagePrice.toInt()} ÷ ${m.piecesPerPackage} ชิ้น = ฿${String.format(Locale.US, "%.2f", unit)}/หน่วย • กำไร ฿${String.format(Locale.US, "%.2f", profit)}",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 4. Hidden & Operating Costs Log Header & Add button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "บันทึกต้นทุนแฝง & ค่าใช้จ่าย", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "รวมต้นทุนแฝงเดือนนี้: ฿${String.format(Locale.US, "%.0f", summary.totalHiddenCostThisMonth)}", fontSize = 12.sp, color = ChiliRed, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isAddCostDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "เพิ่มรายจ่าย", fontSize = 12.sp)
                }
            }
        }

        // Costs List
        items(hiddenCosts, key = { it.id }) { cost ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = cost.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "${cost.date} • ${cost.notes}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text(
                        text = "฿${String.format(Locale.US, "%.0f", cost.amount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ChiliRed
                    )
                }
            }
        }
    }

    if (isAddCostDialogOpen) {
        AddCostDialog(
            onDismiss = { isAddCostDialogOpen = false },
            onAdd = { name, cat, amount, note ->
                viewModel.addHiddenCost(name, cat, amount, note)
                isAddCostDialogOpen = false
            }
        )
    }
}

@Composable
fun MetricSmallBox(title: String, value: String, highlight: Boolean = false) {
    Column {
        Text(text = title, fontSize = 10.sp, color = Color.Gray)
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = if (highlight) CucumberGreen else Color.Black
        )
    }
}

@Composable
fun AddCostDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, cat: String, amount: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "📝 บันทึกต้นทุนแฝงใหม่", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ชื่อรายการ (เช่น เติมแก๊ส, ถุงพลาสติก)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("จำนวนเงิน (บาท)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("หมายเหตุเพิ่มเติม") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ยกเลิก")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (name.isNotEmpty() && amount > 0) {
                                onAdd(name, "utilities", amount, note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("บันทึก")
                    }
                }
            }
        }
    }
}
