package ir.dadban.glass.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.dadban.glass.model.*
import ir.dadban.glass.net.DadbanApi
import ir.dadban.glass.security.AuthStore
import ir.dadban.glass.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun ChatScreen(
    authStore: AuthStore,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var conversationId by remember { mutableStateOf<Int?>(null) }
    var lines by remember { mutableStateOf(listOf<ChatLine>()) }
    var error by remember { mutableStateOf<String?>(null) }

    // دیالوگ نمایش متن ماده قانونی
    var selectedArticleForDialog by remember { mutableStateOf<CitedArticle?>(null) }
    // دیالوگ پیش‌نمایش سند قابل چاپ A4
    var activePrintableDoc by remember { mutableStateOf<PrintableDocument?>(null) }

    // انتخاب تصویر ابلاغیه یا سند
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val base64 = uriToBase64(context, uri)
                selectedImageBase64 = base64
            }
        }
    }

    fun sendMessage(text: String) {
        val clean = text.trim()
        val img = selectedImageBase64
        if ((clean.isEmpty() && img == null) || loading) return

        if (!authStore.hasValidAuth()) {
            onNavigateToProfile()
            return
        }

        val userLine = ChatLine(
            text = clean.ifEmpty { "تحلیل و بررسی تصویر پیوست‌شده" },
            fromUser = true,
            imageBase64 = img
        )
        lines = lines + userLine
        input = ""
        selectedImageBase64 = null
        focus.clearFocus()
        loading = true
        error = null

        scope.launch {
            try {
                val req = ChatRequest(
                    conversationId = conversationId,
                    message = userLine.text,
                    imageBase64 = img
                )
                val resp = DadbanApi.chat(req, authStore)
                conversationId = resp.conversationId ?: conversationId
                val botLine = ChatLine(
                    text = resp.answer,
                    fromUser = false,
                    citedArticles = resp.citedArticles,
                    verification = resp.verification
                )
                lines = lines + botLine
            } catch (e: Exception) {
                error = e.message ?: "خطا در برقراری ارتباط با دستیار هوشمند."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(lines.size, loading) {
        val count = lines.size + if (loading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // نوار بالای صفحه چت
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
                Icon(Icons.Default.Balance, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("چت هوشمند دادبان", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    if (conversationId != null) "گفتگوی جاری شماره #$conversationId" else "دستیار و مشاور حقوقی ۲۴ ساعته",
                    fontSize = 11.sp, color = Muted
                )
            }
            if (lines.isNotEmpty()) {
                IconButton(onClick = { lines = emptyList(); conversationId = null; error = null }) {
                    Icon(Icons.Default.AddComment, "گفتگوی جدید", tint = Teal, modifier = Modifier.size(20.dp))
                }
            }
        }

        // بدنه اصلی چت
        if (lines.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyChatWelcome(
                    onPrompt = { sendMessage(it) },
                    onAttachImage = { imagePicker.launch("image/*") }
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(lines) { _, line ->
                    ChatMessageBubble(
                        line = line,
                        onArticleClick = { selectedArticleForDialog = it },
                        onPrintDocClick = { activePrintableDoc = it }
                    )
                }
                if (loading) item { TypingIndicator() }
                if (error != null) item { ErrorBanner(error!!) }
            }
        }

        // پیش‌نمایش تصویر انتخاب‌شده
        AnimatedVisibility(visible = selectedImageBase64 != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.9f))
                    .border(1.dp, Teal.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DocumentScanner, null, tint = Teal, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("تصویر ابلاغیه یا سند حقوقی پیوست شد", fontSize = 12.sp, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = { selectedImageBase64 = null }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "حذف", tint = Muted, modifier = Modifier.size(16.dp))
                }
            }
        }

        // نوار ارسال پیام
        MessageComposer(
            value = input,
            onValueChange = { input = it },
            onSend = { sendMessage(input) },
            onPickImage = { imagePicker.launch("image/*") },
            hasAttachedImage = selectedImageBase64 != null,
            enabled = !loading
        )

        Text(
            "تحلیل‌ها و پاسخ‌های هوش مصنوعی جنبه مشورتی داشته و تصمیم نهایی قضایی بر عهده دادگاه است.",
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center
        )
    }

    // دیالوگ نمایش جزئیات ماده قانونی استناد شده
    selectedArticleForDialog?.let { article ->
        AlertDialog(
            onDismissRequest = { selectedArticleForDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, null, tint = Teal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(article.title.ifBlank { "ماده قانونی" }, fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(article.lawName, color = TealLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(article.summary, color = Ink, fontSize = 13.sp, lineHeight = 22.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ماده قانونی", "${article.title}\n${article.summary}"))
                    selectedArticleForDialog = null
                }) {
                    Text("کپی متن", color = Teal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedArticleForDialog = null }) {
                    Text("بستن", color = Muted)
                }
            },
            containerColor = Color(0xFFF9FBFB),
            shape = RoundedCornerShape(22.dp)
        )
    }

    // دیالوگ پیش‌نمایش اوراق قضایی A4
    activePrintableDoc?.let { doc ->
        Dialog(
            onDismissRequest = { activePrintableDoc = null },
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
                                Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                                Text(doc.docType, fontSize = 11.sp, color = Muted)
                            }
                            IconButton(onClick = { activePrintableDoc = null }) {
                                Icon(Icons.Default.Close, "بستن", tint = Muted)
                            }
                        }
                        Divider(Modifier.padding(vertical = 12.dp), color = Paper)
                        LazyColumn(Modifier.weight(1f)) {
                            item {
                                Text(
                                    doc.content,
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
                                        putExtra(Intent.EXTRA_TEXT, "${doc.title}\n\n${doc.content}")
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
                                    clipboard.setPrimaryClip(ClipData.newPlainText("سند حقوقی", doc.content))
                                    activePrintableDoc = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = Teal)
                                Spacer(Modifier.width(6.dp))
                                Text("کپی متن سند", fontSize = 12.sp, color = Teal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    line: ChatLine,
    onArticleClick: (CitedArticle) -> Unit,
    onPrintDocClick: (PrintableDocument) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (line.fromUser) Arrangement.Start else Arrangement.End
    ) {
        if (!line.fromUser) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth(0.93f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp, end = 6.dp)
                ) {
                    Icon(Icons.Default.Shield, null, tint = Teal, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("پاسخ دادبان هوشمند", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Medium)
                }

                // متن پاسخ با باکس شیشه‌ای
                Column(
                    Modifier.clip(RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                        .background(WhiteGlass)
                        .border(1.dp, BorderGlass, RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                        .padding(14.dp)
                ) {
                    Text(line.text, color = Ink, fontSize = 13.sp, lineHeight = 23.sp)

                    // دکمه نمایش سند A4 قابل چاپ در صورت وجود
                    line.verification.printableDocument?.let { pDoc ->
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(MintSoft).clickable { onPrintDocClick(pDoc) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Print, null, tint = Teal, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("سند تنظیمی قضایی آماده چاپ A4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("مشاهده، ویرایش و ارسال", fontSize = 9.sp, color = Muted)
                            }
                            Icon(Icons.Default.ChevronLeft, null, tint = Teal, modifier = Modifier.size(18.dp))
                        }
                    }

                    // مواد قانونی استناد شده (Cited Articles)
                    if (line.citedArticles.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("مواد قانونی و مستندات آرای وحدت رویه:", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(line.citedArticles) { article ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEDF6F3),
                                    modifier = Modifier.clickable { onArticleClick(article) }
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Gavel, null, tint = Teal, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(article.title.ifBlank { "ماده قانونی" }, fontSize = 10.sp, color = Teal, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth(0.88f)) {
                if (line.imageBase64 != null) {
                    Row(
                        Modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, null, tint = Teal, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("پیوست تصویر ابلاغیه", fontSize = 10.sp, color = Muted)
                    }
                }
                Text(
                    line.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                        .background(Brush.linearGradient(listOf(Teal, TealLight)))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatWelcome(onPrompt: (String) -> Unit, onAttachImage: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(MintSoft, Color(0xFFE9F4F4)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Gavel, null, tint = Teal, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("دستیار هوشمند حقوقی دادبان", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "طرح سوال، اسکن ابلاغیه ثنا، محاسبه مهریه و دیه یا تنظیم دادخواست",
            color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(20.dp))

        // دکمه تحلیل ابلاغیه ثنا
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.85f))
                .border(1.dp, Teal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .clickable { onAttachImage() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DocumentScanner, null, tint = Teal, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("اسکن و تحلیل تصویر ابلاغیه ثنا", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("استخراج مهلت‌های قانونی، مواد استنادی و متن دادنامه", fontSize = 10.sp, color = Muted)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Teal, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(10.dp))

        val prompts = listOf(
            "مهریه به نرخ روز چطور محاسبه می‌شود؟",
            "برای مطالبه وجه سفته چه اقداماتی لازم است؟",
            "یک اظهارنامه رسمی برای تخلیه ملک تنظیم کن"
        )
        prompts.forEach { prompt ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = 0.7f))
                    .border(1.dp, BorderGlass, RoundedCornerShape(15.dp))
                    .clickable { onPrompt(prompt) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Sparkles, null, tint = Teal, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(prompt, fontSize = 12.sp, color = Ink)
            }
        }
    }
}

@Composable
private fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    hasAttachedImage: Boolean,
    enabled: Boolean
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.95f))
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPickImage, enabled = enabled) {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = "پیوست تصویر ابلاغیه",
                tint = if (hasAttachedImage) Teal else Muted,
                modifier = Modifier.size(22.dp)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.weight(1f).padding(vertical = 9.dp),
            textStyle = TextStyle(color = Ink, fontSize = 13.sp, textAlign = TextAlign.Right),
            cursorBrush = Brush.verticalGradient(listOf(Teal, TealLight)),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        if (hasAttachedImage) "توضیح اختیاری در مورد تصویر..." else "پرسش یا موضوع حقوقی خود را بنویسید...",
                        color = Color(0xFF9AA9AA), fontSize = 12.sp
                    )
                }
                inner()
            }
        )

        Spacer(Modifier.width(6.dp))

        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if ((value.isNotBlank() || hasAttachedImage) && enabled) Teal else Color(0xFFD7E5E0))
                .clickable(enabled = enabled && (value.isNotBlank() || hasAttachedImage)) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).background(WhiteGlass)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(14.dp), color = Teal, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("در حال جستجو در بانک قوانین و پردازش حقوقی...", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AmberAlert)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WarningAmber, null, tint = AmberText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = AmberText, fontSize = 11.sp, lineHeight = 18.sp)
    }
}

private fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Exception) { null }
}
