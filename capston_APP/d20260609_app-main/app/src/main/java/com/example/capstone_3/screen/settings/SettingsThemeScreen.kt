package com.example.capstone_3.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.Blue
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.SubTopBar

private data class ThemeOption(val color: Color, val label: String)

// 기본 색상 먼저, 이후 빨 → 검 순서
private val themeColorOptions = listOf(
    ThemeOption(Blue,               "기본"),
    ThemeOption(Color(0xFFE87C52),  ""),
    ThemeOption(Color(0xFF927AB8),  ""),
    ThemeOption(Color(0xFF5A87B8),  ""),
    ThemeOption(Color(0xFF649552),  ""),
    ThemeOption(Color(0xFF4A5A68),  ""),
)

@Composable
fun SettingsThemeScreen(
    onBack: () -> Unit,
    onThemeChange: (Color) -> Unit
) {
    val currentTheme = LocalThemeColor.current

    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("테마 설정", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.padding(24.dp)) {
            Text("색상 테마", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("원하는 색상을 선택하면 앱 전체 테마에 적용됩니다.", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    themeColorOptions.forEach { option ->
                        val isSelected = option.color == currentTheme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(option.color)
                                    .then(
                                        if (isSelected)
                                            Modifier.border(3.dp, Color(0xFF1A1A1A), CircleShape)
                                        else
                                            Modifier
                                    )
                                    .clickable { onThemeChange(option.color) }
                            )
                            if (option.label.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = option.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF1A1A1A) else Color.Gray
                                )
                            } else {
                                Spacer(Modifier.height(6.dp + 14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
