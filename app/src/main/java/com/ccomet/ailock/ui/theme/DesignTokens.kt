package com.ccomet.ailock.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object AILockShape {
    val card = RoundedCornerShape(8.dp)
    val control = RoundedCornerShape(8.dp)
    val graphBar = RoundedCornerShape(3.dp)
    val pill = RoundedCornerShape(8.dp)
    val bottomSheet = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
}

object AILockSpacing {
    val screenHorizontal = 20.dp
    val sectionGap = 18.dp
    val contentVertical = 16.dp
    val listGap = 14.dp
    val compactGap = 10.dp
    val cardPadding = 16.dp
    val itemPadding = 14.dp
    val iconTextGap = 12.dp
    val buttonIconGap = 8.dp
    val buttonContentHorizontal = 18.dp
    val bottomChromeTopPadding = 8.dp
}

object AILockLayout {
    val expandedHeaderHeight = 168.dp
    val collapsedHeaderHeight = 64.dp
    val bottomNavHeight = 64.dp
    val bottomActionAreaHeight = 132.dp
    val scrollContentBottomPadding = bottomActionAreaHeight + AILockSpacing.sectionGap
    val buttonHeight = 52.dp
    val navIconSize = 24.dp
    val appIconSize = 48.dp
    val compactSegmentWidth = 48.dp
    val compactSegmentHeight = 32.dp
}

@Composable
fun ailockCardBorder(): BorderStroke = BorderStroke(1.dp, AppBorder)

@Composable
fun ailockStrongBorder(): BorderStroke = BorderStroke(1.dp, AppBorderStrong)
