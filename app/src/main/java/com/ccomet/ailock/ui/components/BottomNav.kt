package com.ccomet.ailock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.R
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppBorderStrong
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing


@Composable
fun FloatingBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomPadding = balancedNavigationBottomPadding()

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = AILockSpacing.screenHorizontal)
            .padding(top = AILockSpacing.bottomChromeTopPadding, bottom = bottomPadding)
            .fillMaxWidth()
            .height(AILockLayout.bottomNavHeight)
            .clip(AILockShape.card)
            .background(AppSurfaceMuted)
            .border(1.dp, AppBorderStrong, AILockShape.card),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                BottomItem("home", "홈", R.drawable.ic_nav_home_outlined, R.drawable.ic_nav_home_filled),
                BottomItem("records", "기록", R.drawable.ic_nav_records_outlined, R.drawable.ic_nav_records_filled),
                BottomItem("restrictions", "제한", R.drawable.ic_nav_restrictions_outlined, R.drawable.ic_nav_restrictions_filled),
                BottomItem("settings", "설정", R.drawable.ic_nav_settings_outlined, R.drawable.ic_nav_settings_filled),
            ).forEach { item ->
                BottomNavButton(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavButton(
    item: BottomItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(AILockLayout.buttonHeight)
            .clip(AILockShape.control)
            .background(AppSurfaceMuted)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = if (selected) item.selectedIconRes else item.iconRes),
            contentDescription = item.label,
            modifier = Modifier.size(AILockLayout.navIconSize),
            tint = if (selected) AppTextStrong else AppTextSubtle,
        )
        Text(
            text = item.label,
            modifier = Modifier.padding(top = 2.dp),
            color = if (selected) AppTextStrong else AppTextSubtle,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private data class BottomItem(
    val route: String,
    val label: String,
    val iconRes: Int,
    val selectedIconRes: Int,
)
