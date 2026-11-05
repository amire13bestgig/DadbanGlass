package ir.dadban.glass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.dadban.glass.security.AuthStore
import ir.dadban.glass.ui.screens.*
import ir.dadban.glass.ui.theme.*

enum class NavTab(val title: String, val icon: ImageVector) {
    CHAT("گفتگو", Icons.Default.Forum),
    CALCULATORS("محاسبه‌گر", Icons.Default.Calculate),
    LAWS("قوانین", Icons.Default.MenuBook),
    DOCUMENTS("اوراق قضایی", Icons.Default.Description),
    DEADLINES("مواعید", Icons.Default.Event),
    PROFILE("پروفایل", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authStore = AuthStore(this)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    DadbanMainApp(authStore)
                }
            }
        }
    }
}

@Composable
fun DadbanMainApp(authStore: AuthStore) {
    var activeTab by remember { mutableStateOf(NavTab.CHAT) }

    Box(Modifier.fillMaxSize().background(Paper)) {
        // پس‌زمینه گرادیان لطیف شیشه‌ای دادبان
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFFE5F3EC), Color(0xFFF4F8F6), Color(0xFFE8F0F1)))
            )
        )
        // حباب‌های نوری نئومورفیک
        Box(
            Modifier.offset(x = 240.dp, y = 80.dp).size(200.dp).blur(60.dp)
                .background(Color(0x557CC7B0), CircleShape)
        )
        Box(
            Modifier.offset(x = (-60).dp, y = 420.dp).size(220.dp).blur(70.dp)
                .background(Color(0x4478AEB2), CircleShape)
        )

        Column(
            Modifier.fillMaxSize().imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // محتوای تب فعال
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    NavTab.CHAT -> ChatScreen(
                        authStore = authStore,
                        onNavigateToProfile = { activeTab = NavTab.PROFILE }
                    )
                    NavTab.CALCULATORS -> CalculatorsScreen(authStore = authStore)
                    NavTab.LAWS -> LawsScreen(authStore = authStore)
                    NavTab.DOCUMENTS -> DocumentsScreen(
                        authStore = authStore,
                        onNavigateToChat = { activeTab = NavTab.CHAT }
                    )
                    NavTab.DEADLINES -> DeadlinesScreen(authStore = authStore)
                    NavTab.PROFILE -> ProfileScreen(authStore = authStore)
                }
            }

            // نوار ناوبری شیشه‌ای پایین صفحه (Bottom Navigation Bar)
            GlassBottomNavigation(
                activeTab = activeTab,
                onTabSelect = { activeTab = it }
            )
        }
    }
}

@Composable
private fun GlassBottomNavigation(
    activeTab: NavTab,
    onTabSelect: (NavTab) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp)).background(WhiteGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(26.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTab.values().forEach { tab ->
            val isSelected = tab == activeTab
            Column(
                Modifier.clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) MintSoft else Color.Transparent)
                    .clickable { onTabSelect(tab) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.title,
                    tint = if (isSelected) Teal else Muted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    tab.title,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Teal else Muted
                )
            }
        }
    }
}
