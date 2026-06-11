package com.example.capstone_3.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.LocalThemeColor
import kotlin.math.abs

// ── 드럼롤 스타일 스크롤 피커 ─────────────────────────────────────────
@Composable
fun WheelPicker(
    modifier: Modifier = Modifier,
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 48.dp
) {
    val halfCount     = visibleItemCount / 2
    val listState     = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling) onSelectedChange(listState.firstVisibleItemIndex)
        }
    }
    val centerIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    Box(modifier = modifier.height(itemHeight * visibleItemCount), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth().height(itemHeight), verticalArrangement = Arrangement.SpaceBetween) {
            HorizontalDivider(thickness = 1.5.dp, color = LocalThemeColor.current)
            HorizontalDivider(thickness = 1.5.dp, color = LocalThemeColor.current)
        }
        LazyColumn(
            state = listState, flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(halfCount) { Spacer(modifier = Modifier.height(itemHeight)) }
            itemsIndexed(items) { index, item ->
                val distance   = abs(index - centerIndex)
                val alpha      = when (distance) { 0 -> 1f; 1 -> 0.55f; 2 -> 0.25f; else -> 0.1f }
                val fontSize   = when (distance) { 0 -> 22.sp; 1 -> 17.sp; 2 -> 14.sp; else -> 12.sp }
                val fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal
                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(item, fontSize = fontSize, fontWeight = fontWeight,
                        color = Color.Black.copy(alpha = alpha), textAlign = TextAlign.Center)
                }
            }
            items(halfCount) { Spacer(modifier = Modifier.height(itemHeight)) }
        }
    }
}
