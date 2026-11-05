package ir.dadban.glass.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.dadban.glass.model.BillingPlan
import ir.dadban.glass.model.UserDto
import ir.dadban.glass.net.DadbanApi
import ir.dadban.glass.security.AuthStore
import ir.dadban.glass.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(authStore: AuthStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(authStore.getCachedUser()) }
    var plans by remember { mutableStateOf(listOf<BillingPlan>()) }
    var loading by remember { mutableStateOf(false) }

    // متغیرهای ورود با شماره موبایل
    var mobileNumber by remember { mutableStateOf(currentUser?.mobile ?: "0912") }
    var verificationCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpLoading by remember { mutableStateOf(false) }

    // متغیرهای کلید اختصاصی API
    var apiKey by remember { mutableStateOf(authStore.getApiKey()) }
    var showApiKeyInput by remember { mutableStateOf(false) }

    fun refreshUser() {
        scope.launch {
            val user = DadbanApi.getMe(authStore)
            currentUser = user
            authStore.saveUser(user)
        }
    }

    LaunchedEffect(Unit) {
        plans = DadbanApi.getBillingPlans()
        if (authStore.hasValidAuth()) {
            refreshUser()
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
                    .background(Brush.linearGradient(listOf(Teal, Color(0xFF1E8276)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("حساب کاربری و اشتراک", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("مدیریت ورود پیامکی، وضعیت پلن و کلید امنیتی", fontSize = 11.sp, color = Muted)
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // کارت ورود پیامکی (SMS OTP)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, null, tint = Teal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (currentUser != null && authStore.getJwtToken().isNotBlank()) "نشست کاربری فعال" else "ورود سریع با پیامک (کاوه‌نگار)",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink
                            )
                        }

                        if (currentUser != null && authStore.getJwtToken().isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text("کاربر: ${currentUser!!.name}", fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Medium)
                            Text("شماره تماس: ${currentUser!!.mobile}", fontSize = 12.sp, color = Muted)
                            Spacer(Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(10.dp), color = MintSoft) {
                                Text(
                                    "اشتراک: ${currentUser!!.subscriptionPlan}",
                                    fontSize = 11.sp,
                                    color = Teal,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    authStore.logout()
                                    currentUser = null
                                    otpSent = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent)
                            ) {
                                Text("خروج از حساب کاربری", fontSize = 12.sp)
                            }
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Text("شماره تلفن همراه خود را جهت دریافت کد تایید پیامکی وارد کنید:", fontSize = 11.sp, color = Muted)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = mobileNumber,
                                onValueChange = { mobileNumber = it },
                                label = { Text("شماره موبایل (مانند 09121234567)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            if (otpSent) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = verificationCode,
                                    onValueChange = { verificationCode = it },
                                    label = { Text("کد تایید پیامک‌شده") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        otpLoading = true
                                        scope.launch {
                                            try {
                                                val res = DadbanApi.verifyOtp(mobileNumber, verificationCode)
                                                authStore.saveJwtToken(res.token)
                                                if (res.user != null) {
                                                    authStore.saveUser(res.user)
                                                    currentUser = res.user
                                                }
                                                Toast.makeText(context, "ورود با موفقیت انجام شد", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, e.message ?: "کد نامعتبر است", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                otpLoading = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !otpLoading && verificationCode.length >= 4
                                ) {
                                    Text("تایید کد و ورود", fontSize = 13.sp)
                                }
                            } else {
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        otpLoading = true
                                        scope.launch {
                                            try {
                                                DadbanApi.sendOtp(mobileNumber)
                                                otpSent = true
                                                Toast.makeText(context, "کد تایید پیامک شد", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, e.message ?: "خطا در ارسال پیامک", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                otpLoading = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !otpLoading && mobileNumber.length >= 10
                                ) {
                                    Text("دریافت کد تایید پیامکی", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // بخش کلید اختصاصی API (توسعه‌دهنده / ادمین)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { showApiKeyInput = !showApiKeyInput },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, null, tint = Teal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("کلید اختصاصی API دادبان (X-API-Key)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
                                Text("اتصال مستقیم بدون پیامک با رمزنگاری سخت‌افزاری Keystore", fontSize = 10.sp, color = Muted)
                            }
                            Icon(
                                if (showApiKeyInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = Muted
                            )
                        }

                        AnimatedVisibility(visible = showApiKeyInput) {
                            Column(Modifier.padding(top = 12.dp)) {
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = { apiKey = it },
                                    label = { Text("کلید X-API-Key") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            authStore.saveApiKey(apiKey.trim())
                                            Toast.makeText(context, "کلید امن ذخیره شد", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("ذخیره در Keystore", fontSize = 12.sp)
                                    }
                                    if (apiKey.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                apiKey = ""
                                                authStore.saveApiKey("")
                                                Toast.makeText(context, "کلید حذف شد", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("حذف", fontSize = 12.sp, color = RedAccent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // پلن‌های ارتقای اشتراک
            item {
                Text("پلن‌های اشتراک دادبان هوشمند:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
            }

            items(plans) { plan ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${"%,d".format(plan.priceToman).replace(',', '،')} تومان",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Teal
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        plan.features.forEach { feat ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.Check, null, tint = Teal, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(feat, fontSize = 11.sp, color = Muted)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "انتقال به درگاه پرداخت شاپرک...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ارتقای اشتراک (${plan.durationDays} روزه)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
