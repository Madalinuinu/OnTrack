package com.example.ontrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.PlatformTextStyle

/**
 * Primary pill-style (fully rounded) button with brand color.
 * Layer 1: blue/purple chenar (background). Layer 2: white text on top.
 */
@Composable
fun OnTrackPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val white = Color(0xFFFFFFFF)
    val bgColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 100.dp, minHeight = 52.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        // Layer 1: fundal albastru/mov (chenarul/butonul)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (enabled) bgColor else bgColor.copy(alpha = 0.5f),
                    shape = shape
                )
        )
        // Layer 2: scrisul peste fundal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 20.dp)
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = text,
                style = TextStyle(
                    color = white,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                ),
                modifier = Modifier.alpha(if (enabled) 1f else 0.7f)
            )
        }
    }
}

/**
 * Secondary pill-style button for Daily / Weekly toggles.
 */
@Composable
fun OnTrackSegmentButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg: Color
    val content: Color
    if (selected) {
        bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        content = MaterialTheme.colorScheme.onPrimary
    } else {
        bg = Color.White
        content = Color(0xFF666666)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = bg,
            contentColor = content
        ),
        border = BorderStroke(
            width = 0.dp,
            color = Color.Transparent
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

