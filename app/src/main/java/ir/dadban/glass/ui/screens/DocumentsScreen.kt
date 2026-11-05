package ir.dadban.glass.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.dadban.glass.model.DocumentCategory
import ir.dadban.glass.model.JudicialDocument
import ir.dadban.glass.net.DadbanApi
import ir.dadban.glass.security.AuthStore
import ir.dadban.glass.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DocumentsScreen(authStore: AuthStore, onNavigateToChat: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var documents by remember { mutableStateOf(listOf<JudicialDocument>()) }
    var selectedCategory by remember { mutableStateOf<DocumentCategory?>(null) }
    var activeDoc by remember { mutableStateOf<JudicialDocument?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        scope.launch {
            try {
                documents = DadbanApi.getDocuments(authStore)
            } finally {
                loading = false
            }
        }
    }

    val filtered = if (selectedCategory == null) documents else documents.filter { it.category == selectedCategory }

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
                    .background(Brush.linearGradient(listOf(Teal, Color(0xFF208272)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("اوراق قضایی و قراردادها", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("تنظیم هوشمند دادخواست، شکواییه، اظهارنامه و قرارداد A4", fontSize = 11.sp, color = Muted)
            }
            IconButton(onClick = onNavigateToChat) {
                Icon(Icons.Default.Add, "تنظیم جدید", tint = Teal, modifier = Modifier.size(22.dp))
            }
        }

        // ردیف فیلتر دسته‌بندی
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedCategory == null) Teal else WhiteGlass,
                    modifier = Modifier.clickable { selectedCategory = null }
                ) {
                    Text(
                        "همه اوراق",
                        color = if (selectedCategory == null) Color.White else Ink,
                        fontSize = 11.sp,
                        fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            items(DocumentCategory.values()) { cat ->
                val isSelected = cat == selectedCategory
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Teal else WhiteGlass,
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        cat.titleFa,
                        color = if (isSelected) Color.White else Ink,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // کارت دعوت به ساخت سند جدید با هوش مصنوعی
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MintSoft),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onNavigateToChat() }
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoFixHigh, null, tint = Teal, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("تنظیم فوری سند با هوش مصنوعی", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("مشخصات دعوا را در چت بگویید تا سند A4 تحویل بگیرید", fontSize = 10.sp, color = Muted)
                }
                Icon(Icons.Default.ChevronLeft, null, tint = Teal, modifier = Modifier.size(18.dp))
            }
        }

        // لیست اوراق
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { doc ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
                        .clickable { activeDoc = doc }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEBF5F1)) {
                                Text(
                                    doc.category.titleFa,
                                    fontSize = 10.sp,
                                    color = Teal,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(doc.createdAt, fontSize = 10.sp, color = Muted)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
                        Spacer(Modifier.height(6.dp))
                        Text(doc.fullText, fontSize = 11.sp, color = Muted, maxLines = 2, lineHeight = 18.sp)
                    }
                }
            }
        }
    }

    // دیالوگ تمام‌صفحه پیش‌نمایش و اقدام سند
    activeDoc?.let { doc ->
        Dialog(
            onDismissRequest = { activeDoc = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).padding(16.dp), contentAlignment = Alignment.Center) {
                Surface(
                    Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White
                ) {
                    Column(Modifier.fillMaxSize().padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = Teal, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                                Text("${doc.category.titleFa} • ثبت‌شده در ${doc.createdAt}", fontSize = 11.sp, color = Muted)
                            }
                            IconButton(onClick = { activeDoc = null }) {
                                Icon(Icons.Default.Close, "بستن", tint = Muted)
                            }
                        }
                        Divider(Modifier.padding(vertical = 12.dp), color = Paper)
                        LazyColumn(Modifier.weight(1f)) {
                            item {
                                Text(
                                    doc.fullText,
                                    fontSize = 13.sp,
                                    lineHeight = 24.sp,
                                    color = Ink,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Divider(Modifier.padding(vertical = 12.dp), color = Paper)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "${doc.title}\n\n${doc.fullText}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "ارسال سند قضایی"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("اشتراک‌گذاری A4", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("سند حقوقی", doc.fullText))
                                    activeDoc = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = Teal)
                                Spacer(Modifier.width(6.dp))
                                Text("کپی متن", fontSize = 12.sp, color = Teal)
                            }
                        }
                    }
                }
            }
        }
    }
}
