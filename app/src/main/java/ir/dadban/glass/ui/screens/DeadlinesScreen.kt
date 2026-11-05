package ir.dadban.glass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.dadban.glass.model.JudicialDeadline
import ir.dadban.glass.net.DadbanApi
import ir.dadban.glass.security.AuthStore
import ir.dadban.glass.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DeadlinesScreen(authStore: AuthStore) {
    val scope = rememberCoroutineScope()
    var deadlines by remember { mutableStateOf(listOf<JudicialDeadline>()) }
    var loading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    var newCaseNumber by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
    var newDays by remember { mutableStateOf("20") }
    var newDate by remember { mutableStateOf("1403/08/25") }

    LaunchedEffect(Unit) {
        loading = true
        scope.launch {
            try {
                deadlines = DadbanApi.getDeadlines(authStore)
            } finally {
                loading = false
            }
        }
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
                    .background(Brush.linearGradient(listOf(Color(0xFFE07A5F), Color(0xFFD46243)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Event, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("مواعید و مهلت‌های قضایی ثنا", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("شمارش معکوس مهلت‌های تجدیدنظرخواهی، واخواهی و کارشناسی", fontSize = 11.sp, color = Muted)
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.AddAlarm, "ثبت مهلت جدید", tint = Color(0xFFD46243), modifier = Modifier.size(22.dp))
            }
        }

        // کارت راهنمای مواعد قانونی
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteCard),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                .border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Teal, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "طبق قانون آیین دادرسی مدنی، روز ابلاغ و روز اقدام جزء مدت محسوب نمی‌شود و در صورت تعطیلی، مهلت تا پایان اولین روز کاری تمدید می‌گردد.",
                    fontSize = 11.sp, lineHeight = 18.sp, color = Muted
                )
            }
        }

        // لیست مهلت‌ها
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(deadlines) { item ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { checked ->
                                deadlines = deadlines.map {
                                    if (it.id == item.id) it.copy(isCompleted = checked) else it
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Teal)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (item.isCompleted) Muted else Ink
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "شماره پرونده: ${item.caseNumber} • سررسید: ${item.dueDate}",
                                fontSize = 11.sp, color = Muted
                            )
                        }
                        if (!item.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (item.daysRemaining <= 3) Color(0xFFFFECE5) else MintSoft
                            ) {
                                Text(
                                    if (item.daysRemaining > 0) "${item.daysRemaining} روز مانده" else "منقضی‌شده",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.daysRemaining <= 3) RedAccent else Teal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF0F4F2)) {
                                Text("انجام شد", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("ثبت مهلت قضایی جدید", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("عنوان مهلت (مثلاً تجدیدنظرخواهی شعبه ۲)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCaseNumber,
                        onValueChange = { newCaseNumber = it },
                        label = { Text("شماره پرونده یا بایگانی") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newDays,
                            onValueChange = { newDays = it },
                            label = { Text("تعداد روز") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newDate,
                            onValueChange = { newDate = it },
                            label = { Text("تاریخ سررسید") },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) {
                        val newItem = JudicialDeadline(
                            id = "dl_${System.currentTimeMillis()}",
                            caseNumber = newCaseNumber.ifBlank { "۱۴۰۳..." },
                            title = newTitle,
                            daysRemaining = newDays.toIntOrNull() ?: 20,
                            dueDate = newDate,
                            isCompleted = false
                        )
                        deadlines = listOf(newItem) + deadlines
                        showAddDialog = false
                    }
                }) {
                    Text("ثبت یادآور", color = Teal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("انصراف", color = Muted)
                }
            },
            containerColor = Color(0xFFF9FBFB),
            shape = RoundedCornerShape(22.dp)
        )
    }
}
