package ir.dadban.glass.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.dadban.glass.model.CalculatorComputeResult
import ir.dadban.glass.model.CalculatorType
import ir.dadban.glass.net.DadbanApi
import ir.dadban.glass.security.AuthStore
import ir.dadban.glass.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun CalculatorsScreen(authStore: AuthStore) {
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(CalculatorType.MEHRIEH) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CalculatorComputeResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // متغیرهای فرم مهریه
    var mehriehYear by remember { mutableStateOf("1392") }
    var mehriehAmount by remember { mutableStateOf("50000000") }

    // متغیرهای فرم هزینه دادرسی
    var dadresiClaim by remember { mutableStateOf("250000000") }
    var dadresiStage by remember { mutableStateOf("badvi") } // badvi, tajdid_nazar, farjam

    // متغیرهای فرم دیه
    var diehPercent by remember { mutableStateOf("100") }
    var isHaramMonth by remember { mutableStateOf(false) }

    // متغیرهای ارث
    var estateAmount by remember { mutableStateOf("3000000000") }
    var hasChildren by remember { mutableStateOf(true) }

    // متغیرهای تأخیر تأدیه
    var takhireAmount by remember { mutableStateOf("400000000") }
    var takhireMonths by remember { mutableStateOf("18") }

    // متغیرهای حق‌الوکاله
    var vekalatClaim by remember { mutableStateOf("800000000") }

    // متغیرهای سنوات
    var sanavatSalary by remember { mutableStateOf("180000000") }
    var sanavatYears by remember { mutableStateOf("4") }

    fun runCalculation() {
        loading = true
        errorMessage = null
        result = null
        scope.launch {
            try {
                val params = JSONObject()
                when (selectedType) {
                    CalculatorType.MEHRIEH -> {
                        params.put("marriage_year", mehriehYear.toIntOrNull() ?: 1390)
                        params.put("amount", mehriehAmount.toLongOrNull() ?: 10_000_000L)
                    }
                    CalculatorType.DADRESI -> {
                        params.put("claim_amount", dadresiClaim.toLongOrNull() ?: 100_000_000L)
                        params.put("stage", dadresiStage)
                    }
                    CalculatorType.DIEH -> {
                        params.put("percent", diehPercent.toDoubleOrNull() ?: 100.0)
                        params.put("is_haram_month", isHaramMonth)
                    }
                    CalculatorType.ERS -> {
                        params.put("estate_value", estateAmount.toLongOrNull() ?: 1_000_000_000L)
                        params.put("has_children", hasChildren)
                    }
                    CalculatorType.TAKHIRE_TADIEH -> {
                        params.put("amount", takhireAmount.toLongOrNull() ?: 100_000_000L)
                        params.put("months", takhireMonths.toIntOrNull() ?: 12)
                    }
                    CalculatorType.VEKALAT -> {
                        params.put("claim_value", vekalatClaim.toLongOrNull() ?: 500_000_000L)
                    }
                    CalculatorType.SANAVAT -> {
                        params.put("monthly_salary", sanavatSalary.toLongOrNull() ?: 100_000_000L)
                        params.put("years_worked", sanavatYears.toDoubleOrNull() ?: 1.0)
                    }
                }
                result = DadbanApi.computeCalculator(selectedType, params, authStore)
            } catch (e: Exception) {
                errorMessage = e.message ?: "خطا در محاسبه"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(selectedType) {
        runCalculation()
    }

    Column(Modifier.fillMaxSize()) {
        // نوار هدر
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp)).background(WhiteGlass)
                .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(Teal, TealLight))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Calculate, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("محاسبه‌گرهای قضایی و حقوقی", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("۷ محاسبه‌گر سرور-ساید منطبق بر آخرین قوانین ۱۴۰۳", fontSize = 11.sp, color = Muted)
            }
        }

        // انتخابگر چیپ‌های محاسبه‌گر
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CalculatorType.values()) { type ->
                val isSelected = type == selectedType
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Teal else WhiteGlass,
                    modifier = Modifier.clickable {
                        selectedType = type
                    }
                ) {
                    Text(
                        type.titleFa,
                        color = if (isSelected) Color.White else Ink,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(selectedType.titleFa, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                        Text(selectedType.desc, fontSize = 11.sp, color = Muted)
                        Spacer(Modifier.height(14.dp))

                        when (selectedType) {
                            CalculatorType.MEHRIEH -> {
                                OutlinedTextField(
                                    value = mehriehYear,
                                    onValueChange = { mehriehYear = it },
                                    label = { Text("سال وقوع عقد ازدواج (شمسی)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = mehriehAmount,
                                    onValueChange = { mehriehAmount = it },
                                    label = { Text("مبلغ مهریه در سند نکاح (ریال)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                            CalculatorType.DADRESI -> {
                                OutlinedTextField(
                                    value = dadresiClaim,
                                    onValueChange = { dadresiClaim = it },
                                    label = { Text("مبلغ بهای خواسته دعوا (ریال)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text("مرحله رسیدگی:", fontSize = 12.sp, color = Ink)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = dadresiStage == "badvi",
                                        onClick = { dadresiStage = "badvi" },
                                        label = { Text("بدوی", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = dadresiStage == "tajdid_nazar",
                                        onClick = { dadresiStage = "tajdid_nazar" },
                                        label = { Text("تجدیدنظر", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = dadresiStage == "farjam",
                                        onClick = { dadresiStage = "farjam" },
                                        label = { Text("فرجام", fontSize = 11.sp) }
                                    )
                                }
                            }
                            CalculatorType.DIEH -> {
                                OutlinedTextField(
                                    value = diehPercent,
                                    onValueChange = { diehPercent = it },
                                    label = { Text("درصد صدمه وارده (مثلاً 100 یا 33.3)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isHaramMonth,
                                        onCheckedChange = { isHaramMonth = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Teal)
                                    )
                                    Text("وقوع صدمه و فوت در ماه‌های حرام (تغلیظ دیه)", fontSize = 12.sp, color = Ink)
                                }
                            }
                            CalculatorType.ERS -> {
                                OutlinedTextField(
                                    value = estateAmount,
                                    onValueChange = { estateAmount = it },
                                    label = { Text("ارزش کل ترکه متوفی (ریال)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = hasChildren,
                                        onCheckedChange = { hasChildren = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Teal)
                                    )
                                    Text("متوفی دارای فرزند است", fontSize = 12.sp, color = Ink)
                                }
                            }
                            CalculatorType.TAKHIRE_TADIEH -> {
                                OutlinedTextField(
                                    value = takhireAmount,
                                    onValueChange = { takhireAmount = it },
                                    label = { Text("مبلغ اصل طلب یا دین (ریال)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = takhireMonths,
                                    onValueChange = { takhireMonths = it },
                                    label = { Text("تعداد ماه‌های تأخیر در پرداخت") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                            CalculatorType.VEKALAT -> {
                                OutlinedTextField(
                                    value = vekalatClaim,
                                    onValueChange = { vekalatClaim = it },
                                    label = { Text("ارزش مالی خواسته پرونده (ریال)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                            CalculatorType.SANAVAT -> {
                                OutlinedTextField(
                                    value = sanavatSalary,
                                    onValueChange = { sanavatSalary = it },
                                    label = { Text("آخرین حقوق پایه ماهانه کارگر (ریال)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = sanavatYears,
                                    onValueChange = { sanavatYears = it },
                                    label = { Text("مدت سابقه کار (سال)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { runCalculation() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !loading
                        ) {
                            if (loading) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("محاسبه دقیق قانونی", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // نمایش نتیجه محاسبه
            result?.let { res ->
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F9F6)),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Mint, RoundedCornerShape(20.dp))
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Teal, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(res.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                res.totalAmount,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Teal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                            Divider(color = Color.White, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                            res.breakdown.forEach { (label, valStr) ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, fontSize = 12.sp, color = Muted)
                                    Text(valStr, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
                                }
                            }
                            if (res.lawNote.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(res.lawNote, fontSize = 10.sp, color = TealLight, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
