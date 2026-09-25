package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern smooth 3-dot jumping animation indicator
 */
@Composable
fun TypingDotsIndicator(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF2563EB),
    dotSize: Dp = 6.dp,
    jumpHeight: Dp = 4.dp,
    spacing: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots_transition")

    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -jumpHeight.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1_anim"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -jumpHeight.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, delayMillis = 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2_anim"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -jumpHeight.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, delayMillis = 260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3_anim"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot1Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .offset(y = dot3Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

/**
 * 1. Top Bar Typing Indicator: displayed under user's name in Chat Detail screen
 * User requirement: "inbox a profile name err niche 3 dot soho typing dekhanor dorkar nei just typing dekhaba" -> Show only "typing..."
 */
@Composable
fun TopBarTypingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2563EB),
    fontSize: TextUnit = 11.sp
) {
    Text(
        text = "typing...",
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * 2. Chat Input Floating Banner: displayed right above the typing box / message bar in Chat Detail screen
 * User requirement: "typing box er upre just 3 dot dekhaba typing dkehanor dorkar nei ar userer name o"
 * Clean 3 jumping dots pill without shaking/jumping text
 */
@Composable
fun ChatInputTypingBanner(
    visible: Boolean,
    isNightMode: Boolean,
    modifier: Modifier = Modifier,
    partnerName: String = ""
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + expandVertically(tween(200)),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(200)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isNightMode) Color(0xFF1E202B).copy(alpha = 0.95f) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f)),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TypingDotsIndicator(
                        dotColor = Color(0xFF2563EB),
                        dotSize = 5.5.dp,
                        jumpHeight = 4.dp,
                        spacing = 4.dp
                    )
                }
            }
        }
    }
}

/**
 * 3. Chat List Item Typing Indicator: displayed in Chat list row
 * User requirement: "inbox a profile name er niche 3 dot soho typing dekhanor dorkar nei just typing dekhaba"
 */
@Composable
fun ChatListTypingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2563EB)
) {
    Text(
        text = "typing...",
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
    )
}
