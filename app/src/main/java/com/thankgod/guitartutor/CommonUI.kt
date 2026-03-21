package com.thankgod.guitartutor

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PixelFont = FontFamily(Font(R.font.pixel_font))

data class AppColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val text: Color,
    val textSecondary: Color,
    val button: Color,
    val overlay: Color
)

fun getAppColors(isDark: Boolean) = if (isDark) AppColors(
    Color(0xFF4A634C),
    Color(0xFF121A12),
    Color(0xFF2C3E2D),
    Color.White,
    Color(0xFFAAAAAA),
    Color(0xFF3A4F3B),
    Color(0xDD000000)
)
else AppColors(
    Color(0xFF9EBA90),
    Color(0xFFDCE6C3),
    Color(0xFFB5C9A6),
    Color.Black,
    Color(0xFF4A5D4E),
    Color(0xFFF0F4E6),
    Color(0xAA8CA381)
)

fun getChordImage(c: String) = when (c) {
    "A Major" -> R.drawable.a_major
    "A Minor" -> R.drawable.a_minor
    "B Major" -> R.drawable.b_major
    "B Minor" -> R.drawable.b_minor
    "C Major" -> R.drawable.c_major
    "C Minor" -> R.drawable.c_minor
    "D Major" -> R.drawable.d_major
    "D Minor" -> R.drawable.d_minor
    "E Major" -> R.drawable.e_major
    "E Minor" -> R.drawable.e_minor
    "F Major" -> R.drawable.f_major
    "F Minor" -> R.drawable.f_minor
    "G Major" -> R.drawable.g_major
    "G Minor" -> R.drawable.g_minor
    else -> R.drawable.chord_placeholder
}

@Composable
fun BouncyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp,
    buttonColor: Color? = null,
    textColor: Color? = null,
    colors: AppColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.92f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "bounce"
    )
    Box(
        modifier
            .height(height)
            .scale(scale)
            .shadow(if (isPressed) 2.dp else 6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(buttonColor ?: colors.button)
            .clickable(interactionSource, null) { onClick() }
            .padding(horizontal = 24.dp),
        Alignment.Center
    ) {
        Text(
            text,
            fontFamily = PixelFont,
            fontSize = if (height > 60.dp) 24.sp else 18.sp,
            color = textColor ?: colors.text
        )
    }
}

@Composable
fun TopBackButton(onBackClick: () -> Unit, colors: AppColors) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.85f else 1f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "bounceBack"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, end = 24.dp), Arrangement.End
    ) {
        Box(
            Modifier
                .size(48.dp)
                .scale(scale)
                .shadow(if (isPressed) 1.dp else 4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary)
                .clickable(interactionSource, null) { onBackClick() },
            Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Go Back",
                tint = colors.background,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun TutorialOverlay(
    step: Int,
    maxSteps: Int,
    text1: () -> String,
    text2: () -> String,
    text3: () -> String,
    onComplete: () -> Unit,
    colors: AppColors,
    text4: (() -> String)? = null
) {
    var currentStep by remember { mutableIntStateOf(step) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { }) {
        Box(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .border(4.dp, Color(0xFFDCE6C3), RoundedCornerShape(16.dp))
                .background(Color(0xFF9EBA90), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (currentStep) {
                        1 -> text1()
                        2 -> text2()
                        3 -> text3()
                        else -> text4?.invoke() ?: ""
                    },
                    fontFamily = PixelFont,
                    fontSize = 20.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(24.dp))
                BouncyButton(
                    if (currentStep < maxSteps) "Next" else "Got it!",
                    { if (currentStep < maxSteps) currentStep++ else onComplete() },
                    height = 50.dp,
                    colors = colors
                )
            }
        }
    }
}
