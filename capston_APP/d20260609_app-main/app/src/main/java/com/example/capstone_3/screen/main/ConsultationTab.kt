package com.example.capstone_3.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.GreenTag
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.TopBar

enum class SlotStatus { Available, Closed, MyBooking }
data class TimeSlotData(val time: String, val status: SlotStatus)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsultationTab(onSettings: () -> Unit = {}, onLogout: () -> Unit = {}) {
    var selectedDate       by remember { mutableStateOf(-1) }
    var selectedTime       by remember { mutableStateOf("") }
    var isScheduleExpanded by remember { mutableStateOf(false) }
    var isDetailExpanded   by remember { mutableStateOf(false) }
    var isTeacherDropdown  by remember { mutableStateOf(false) }
    var selectedTeacher    by remember { mutableStateOf("이선생님 (담임)") }

    val teachers       = listOf("이선생님 (담임)", "박선생님 (수학)", "최선생님 (영어)", "정선생님 (과학)", "강선생님 (사회)")
    val availableDates = setOf(15, 16, 17, 18, 21, 22, 23, 24, 25)
    val slotsByDate: Map<Int, List<TimeSlotData>> = mapOf(
        15 to listOf(TimeSlotData("14:00", SlotStatus.Closed), TimeSlotData("15:00", SlotStatus.Available), TimeSlotData("16:00", SlotStatus.MyBooking)),
        16 to listOf(TimeSlotData("14:00", SlotStatus.Available), TimeSlotData("15:00", SlotStatus.Closed), TimeSlotData("16:00", SlotStatus.Available), TimeSlotData("17:00", SlotStatus.Closed))
    )
    val defaultSlots = listOf(TimeSlotData("14:00", SlotStatus.Available), TimeSlotData("15:00", SlotStatus.Available), TimeSlotData("16:00", SlotStatus.Available))

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("상담 예약", onSettings = onSettings, onLogout = onLogout)
        Column(modifier = Modifier.padding(16.dp)) {

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isScheduleExpanded = !isScheduleExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ ", color = GreenTag, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("예정된 상담 일정", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.background(LocalThemeColor.current, RoundedCornerShape(10.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)) {
                                Text("1", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        Icon(if (isScheduleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null, tint = Color.Gray)
                    }
                    if (isScheduleExpanded) {
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(10.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column {
                                Text("최선생님 (영어)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("2026년 4월 15일 (화)", fontSize = 13.sp, color = Color.Gray)
                                Text("16:00", fontSize = 13.sp, color = LocalThemeColor.current, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { isDetailExpanded = !isDetailExpanded }) {
                                Text("확정", fontSize = 12.sp, color = GreenTag,
                                    modifier = Modifier.border(1.dp, GreenTag, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp))
                                Spacer(Modifier.width(4.dp))
                                Icon(if (isDetailExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (isDetailExpanded) {
                            Spacer(Modifier.height(10.dp))
                            Column(modifier = Modifier.fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(10.dp)).padding(14.dp)) {
                                listOf("상담 교사" to "최선생님 (영어)", "상담 날짜" to "2026년 4월 15일 (화)",
                                    "상담 시간" to "16:00", "상담 방식" to "대면 상담", "상담 장소" to "상담실 (3층)"
                                ).forEach { (label, value) ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.width(80.dp))
                                        Text(value, fontSize = 13.sp, color = Color.DarkGray)
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) {
                                    Text("예약 취소하기", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("새 상담 예약하기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))

                    Text("상담 교사 선택", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .clickable { isTeacherDropdown = !isTeacherDropdown }
                            .padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedTeacher, fontSize = 14.sp)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        if (isTeacherDropdown) {
                            Card(modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                                shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column {
                                    teachers.forEach { teacher ->
                                        Row(modifier = Modifier.fillMaxWidth()
                                            .clickable { selectedTeacher = teacher; isTeacherDropdown = false }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            if (teacher == selectedTeacher) Text("✓ ", fontSize = 13.sp, color = LocalThemeColor.current)
                                            else Spacer(Modifier.width(16.dp))
                                            Text(teacher, fontSize = 14.sp,
                                                color = if (teacher == selectedTeacher) LocalThemeColor.current else Color.DarkGray)
                                        }
                                        if (teacher != teachers.last()) HorizontalDivider(color = Color(0xFFF5F5F5))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = Color.Gray)
                        Text("2026년 4월", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                    Spacer(Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("일", "월", "화", "수", "목", "금", "토").forEach { d ->
                            Text(d, fontSize = 12.sp, color = Color.Gray,
                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    val startDow    = 3
                    val daysInMonth = 30
                    repeat(6) { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            repeat(7) { dow ->
                                val day = week * 7 + dow - startDow + 1
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                    if (day in 1..daysInMonth) {
                                        val isSelected  = day == selectedDate
                                        val isAvailable = day in availableDates
                                        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(50))
                                            .background(when { isSelected -> LocalThemeColor.current; isAvailable -> Color(0xFFE8F0FE); else -> Color.Transparent })
                                            .then(if (isAvailable) Modifier.clickable { selectedDate = day; selectedTime = "" } else Modifier),
                                            contentAlignment = Alignment.Center) {
                                            Text("$day", fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = when { isSelected -> Color.White; isAvailable -> Color.Black; else -> Color(0xFFCCCCCC) })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedDate > 0) {
                        val slots = slotsByDate[selectedDate] ?: defaultSlots
                        Spacer(Modifier.height(18.dp))
                        Text("예약 가능한 시간", fontSize = 13.sp, color = Color.Gray)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            slots.forEach { slot ->
                                val isSelected = selectedTime == slot.time
                                val canPick    = slot.status == SlotStatus.Available
                                Box(modifier = Modifier.then(if (canPick) Modifier.clickable { selectedTime = slot.time } else Modifier)) {
                                    Box(modifier = Modifier.width(88.dp).clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, when { isSelected -> LocalThemeColor.current; canPick -> Color.LightGray; else -> Color.Transparent }, RoundedCornerShape(8.dp))
                                        .background(when { isSelected -> LocalThemeColor.current; slot.status == SlotStatus.Closed -> Color(0xFFEEEEEE); slot.status == SlotStatus.MyBooking -> Color(0xFFFFF9C4); else -> Color.White })
                                        .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                        Text(slot.time, fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when { isSelected -> Color.White; slot.status == SlotStatus.Closed -> Color.Gray; else -> Color.DarkGray })
                                    }
                                    if (slot.status == SlotStatus.Closed || slot.status == SlotStatus.MyBooking) {
                                        Box(modifier = Modifier.align(Alignment.TopEnd)
                                            .background(if (slot.status == SlotStatus.Closed) Color(0xFF757575) else Color(0xFFFFA000),
                                                RoundedCornerShape(topEnd = 8.dp, bottomStart = 6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                            Text(if (slot.status == SlotStatus.Closed) "마감" else "예약됨",
                                                fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("예약 가능" to Color.White, "타인 예약" to Color(0xFFEEEEEE), "내 예약" to Color(0xFFFFF9C4)).forEach { (label, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).border(1.dp, Color.LightGray, RoundedCornerShape(50)).background(color, RoundedCornerShape(50)))
                                    Spacer(Modifier.width(4.dp))
                                    Text(label, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    val canBook = selectedDate > 0 && selectedTime.isNotBlank()
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (canBook) LocalThemeColor.current else Color(0xFFCCCCCC)),
                        enabled = canBook) {
                        Text("예약하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
