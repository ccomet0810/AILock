package com.ccomet.ailock.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object AILockShape {
    val card = RoundedCornerShape(8.dp)
    val control = RoundedCornerShape(8.dp)
    val pill = RoundedCornerShape(8.dp)
}

object AILockSpacing {
    val screenHorizontal = 20.dp
    val sectionGap = 18.dp
    val cardPadding = 16.dp
    val itemPadding = 14.dp
}

@Composable
fun ailockCardBorder(): BorderStroke = BorderStroke(1.dp, AppBorder)

@Composable
fun ailockStrongBorder(): BorderStroke = BorderStroke(1.dp, AppBorderStrong)
