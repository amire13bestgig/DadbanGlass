package ir.dadban.glass.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.dadban.glass.model.LawArticle
import ir.dadban.glass.model.LawCollection
import ir.dadban.glass.net.DadbanApi
import ir.dadban.glass.security.AuthStore
import ir.dadban.glass.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LawsScreen(authStore: AuthStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var collections by remember { mutableStateOf(listOf<LawCollection>()) }
    var articles by remember { mutableStateOf(listOf<LawArticle>()) }
    var selectedArticle by remember { mutableStateOf<LawArticle?>(null) }

    fun doSearch(q: String) {
        loading = true
        scope.launch {
            try {
                articles = DadbanApi.searchLaws(q, authStore)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        collections = DadbanApi.getLawCollections()
        doSearch("")
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
                    .background(Brush.linearGradient(listOf(BlueLaw, Teal))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("بانک جامع قوانین و آرای قضایی", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("جستجو در قوانین مصوب، آرای وحدت رویه و نظریات مشورتی", fontSize = 11.sp, color = Muted)
            }
        }

        // فیلد جستجو
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                doSearch(it)
            },
            placeholder = { Text("جستجوی شماره ماده یا واژگان حقوقی (مانند مهریه، سفته، تخلیه)...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Teal) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; doSearch("") }) {
                        Icon(Icons.Default.Close, null, tint = Muted)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = WhiteCard,
                unfocusedContainerColor = WhiteCard,
                focusedIndicatorColor = Teal,
                unfocusedIndicatorColor = BorderGlass
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { doSearch(query) })
        )

        // ردیف دسته‌بندی قوانین
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(collections) { col ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WhiteGlass,
                    modifier = Modifier.clickable {
                        query = col.title
                        doSearch(col.title)
                    }
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LibraryBooks, null, tint = Teal, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(col.title, fontSize = 11.sp, color = Ink, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // لیست مواد قانونی
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(articles) { article ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
                            .clickable { selectedArticle = article }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(article.number, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Teal)
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MintSoft
                                ) {
                                    Text(
                                        article.collectionName,
                                        fontSize = 10.sp,
                                        color = Teal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                article.text,
                                fontSize = 12.sp,
                                lineHeight = 21.sp,
                                color = Ink,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }
    }

    // دیالوگ نمایش متن کامل ماده و شرح
    selectedArticle?.let { art ->
        AlertDialog(
            onDismissRequest = { selectedArticle = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, null, tint = Teal, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${art.number} - ${art.collectionName}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                }
            },
            text = {
                Column {
                    Text(art.text, fontSize = 13.sp, lineHeight = 23.sp, color = Ink)
                    if (art.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("نکات و تبصره‌های تفسیری:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealLight)
                        Spacer(Modifier.height(4.dp))
                        Text(art.notes, fontSize = 12.sp, lineHeight = 20.sp, color = Muted)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ماده قانونی", "${art.number} ${art.collectionName}\n\n${art.text}\n\n${art.notes}"))
                    selectedArticle = null
                }) {
                    Text("کپی ماده قانونی", color = Teal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedArticle = null }) {
                    Text("بستن", color = Muted)
                }
            },
            containerColor = Color(0xFFF9FBFB),
            shape = RoundedCornerShape(22.dp)
        )
    }
}
