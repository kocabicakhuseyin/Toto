package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SupportedTeam

@Composable
fun TeamBadgeView(
    team: SupportedTeam,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val brush = Brush.sweepGradient(
        colors = listOf(
            Color(team.primaryColor),
            Color(team.secondaryColor),
            Color(team.primaryColor)
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF0F172A))
            .border(2.dp, brush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = team.shortName,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = if (size > 40.dp) 13.sp else 10.sp,
            letterSpacing = 0.5.sp
        )
    }
}
