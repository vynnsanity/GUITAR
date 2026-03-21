package com.thankgod.guitartutor

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.delay

class MetronomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", MODE_PRIVATE)
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)
        val savedMetTut = sharedPref.getBoolean("TUTORIAL_METRONOME", false)

        setContent {
            val isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)
            var hasSeenMetTut by remember { mutableStateOf(savedMetTut) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    MetronomeScreen(
                        hasSeenTutorial = hasSeenMetTut,
                        colors = colors,
                        onTutorialComplete = {
                            hasSeenMetTut = true
                            sharedPref.edit().putBoolean("TUTORIAL_METRONOME", true).apply()
                        },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun MetronomeScreen(hasSeenTutorial: Boolean, colors: AppColors, onTutorialComplete: () -> Unit, onBackClick: () -> Unit) {
    var bpm by remember { mutableFloatStateOf(120f) }
    var isPlaying by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    var tapTimes by remember { mutableStateOf(listOf<Long>()) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    DisposableEffect(Unit) { onDispose { toneGenerator.release() } }

    LaunchedEffect(isPlaying, bpm) {
        if (isPlaying) {
            while (true) {
                flash = true; toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 35); delay(100); flash = false
                val delayTime = (60000 / bpm.toLong()) - 100
                if (delayTime > 0) delay(delayTime)
            }
        } else { flash = false }
    }

    val handleTap = {
        val now = System.currentTimeMillis()
        val newTapTimes = if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2000) listOf(now) else (tapTimes + now).takeLast(4)
        if (newTapTimes.size >= 2) {
            val avgInterval = (newTapTimes.last() - newTapTimes.first()) / (newTapTimes.size - 1)
            if (avgInterval > 0) bpm = (60000f / avgInterval).coerceIn(60f, 200f)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBackButton(onBackClick, colors)
            Spacer(Modifier.height(40.dp))
            Box(Modifier.size(120.dp).shadow(8.dp, CircleShape).background(if (flash) colors.background else colors.primary, CircleShape).border(4.dp, Color.White, CircleShape), Alignment.Center) {
                Text("${bpm.toInt()}", fontFamily = PixelFont, fontSize = 42.sp, color = if (flash) colors.text else colors.background)
            }
            Spacer(Modifier.height(60.dp))
            Box(Modifier.padding(horizontal = 32.dp).background(colors.surface, RoundedCornerShape(16.dp)).padding(24.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SPEED (BPM)", fontFamily = PixelFont, fontSize = 20.sp, color = colors.text)
                    Slider(bpm, { bpm = it }, valueRange = 60f..200f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = colors.primary))
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BouncyButton(if (isPlaying) "STOP" else "PLAY", { isPlaying = !isPlaying }, Modifier.weight(1f), 60.dp, colors = colors); Spacer(Modifier.width(16.dp))
                        BouncyButton("TAP", handleTap, Modifier.weight(1f), 60.dp, colors = colors)
                    }
                }
            }
        }
        if (!hasSeenTutorial) TutorialOverlay(1, 3, { "↓ SPEED (BPM) ↓\n\nUse the slider to adjust how fast the metronome ticks." }, { "↓ PLAY / STOP ↓\n\nTap here to start or pause the metronome." }, { "↓ TAP TEMPO ↓\n\nTap this button to the beat of a song, and the app will automatically find its exact speed!" }, onTutorialComplete, colors)
    }
}
