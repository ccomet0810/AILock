package com.ccomet.ailock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AppBorderStrong
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AilockSegmentedControl(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    segmentWidth: Dp = AILockLayout.compactSegmentWidth,
    segmentHeight: Dp = AILockLayout.compactSegmentHeight,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            val selected = option == selectedOption
            SegmentedButton(
                selected = selected,
                onClick = { onOptionSelected(option) },
                shape = segmentedControlShape(index, options.size),
                modifier = Modifier
                    .width(segmentWidth)
                    .height(segmentHeight),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = AppSurfaceMuted,
                    activeContentColor = AppTextStrong,
                    inactiveContainerColor = MaterialTheme.colorScheme.background,
                    inactiveContentColor = AppTextSubtle.copy(alpha = 0.62f),
                    disabledActiveContainerColor = AppSurfaceMuted,
                    disabledActiveContentColor = AppTextSubtle,
                    disabledInactiveContainerColor = MaterialTheme.colorScheme.background,
                    disabledInactiveContentColor = AppTextSubtle.copy(alpha = 0.45f),
                ),
                border = BorderStroke(1.dp, AppBorderStrong),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                icon = {},
                label = {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

private fun segmentedControlShape(index: Int, count: Int) = RoundedCornerShape(
    topStart = if (index == 0) 8.dp else 0.dp,
    bottomStart = if (index == 0) 8.dp else 0.dp,
    topEnd = if (index == count - 1) 8.dp else 0.dp,
    bottomEnd = if (index == count - 1) 8.dp else 0.dp,
)
