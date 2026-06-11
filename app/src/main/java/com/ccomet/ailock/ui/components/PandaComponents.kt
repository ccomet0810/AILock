package com.ccomet.ailock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle


@Composable
fun RedPandaMascot(
    emotion: PandaEmotion,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.red_panda),
            contentDescription = emotionLabel(emotion),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PandaSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    emotion: PandaEmotion = PandaEmotion.DEFAULT,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpeechBubbleCard(text = text, modifier = Modifier.fillMaxWidth())
        RedPandaMascot(
            emotion = emotion,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun SpeechBubbleCard(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppSurface,
            shape = AILockShape.card,
            border = BorderStroke(1.dp, AppBorder),
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTextStrong,
                        textAlign = TextAlign.Center,
                    )
                }
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (title == null) AppTextStrong else AppTextSubtle,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Canvas(
            modifier = Modifier
                .offset(y = (-1).dp)
                .size(width = 22.dp, height = 12.dp),
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width / 2f, size.height)
                lineTo(size.width, 0f)
                close()
            }
            val stroke = 1.dp.toPx()
            val halfStroke = stroke / 2f
            drawPath(path, AppSurface)
            drawLine(
                color = AppBorder,
                start = Offset(halfStroke, 0f),
                end = Offset(size.width / 2f, size.height - halfStroke),
                strokeWidth = stroke,
            )
            drawLine(
                color = AppBorder,
                start = Offset(size.width - halfStroke, 0f),
                end = Offset(size.width / 2f, size.height - halfStroke),
                strokeWidth = stroke,
            )
        }
    }
}

private fun emotionLabel(emotion: PandaEmotion): String = when (emotion) {
    PandaEmotion.DEFAULT -> "기본"
    PandaEmotion.HAPPY -> "기쁨"
    PandaEmotion.ENCOURAGING -> "응원"
    PandaEmotion.THINKING -> "생각 중"
    PandaEmotion.SUSPICIOUS -> "의심"
    PandaEmotion.ANGRY -> "단호"
    PandaEmotion.SAD -> "속상"
    PandaEmotion.DISAPPOINTED -> "실망"
}
