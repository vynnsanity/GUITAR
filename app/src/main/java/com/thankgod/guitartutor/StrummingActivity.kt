package com.thankgod.guitartutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

class StrummingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", MODE_PRIVATE)
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)
        val savedStrumTut = sharedPref.getBoolean("TUTORIAL_STRUMMING", false)

        setContent {
            val isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)
            var hasSeenStrumTut by remember { mutableStateOf(savedStrumTut) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    StrummingScreen(
                        colors = colors,
                        hasSeenTutorial = hasSeenStrumTut,
                        onTutorialComplete = {
                            hasSeenStrumTut = true
                            sharedPref.edit().putBoolean("TUTORIAL_STRUMMING", true).apply()
                        },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun StrummingScreen(colors: AppColors, hasSeenTutorial: Boolean, onTutorialComplete: () -> Unit, onBackClick: () -> Unit) {
    data class StrumPattern(val name: String, val pattern: String)
    val patterns = listOf(
        StrumPattern("The Basic", "⬇️   ⬇️   ⬇️   ⬇️"),
        StrumPattern("Pop Strum", "⬇️   ⬇️   ⬆️   ⬆️   ⬇️   ⬆️"),
        StrumPattern("The Waltz", "⬇️   ⬇️   ⬆️   ⬇️   ⬆️"),
        StrumPattern("Folk Drive", "⬇️  ⬆️  ⬇️  ⬆️  ⬇️  ⬆️  ⬇️  ⬆️")
    )

    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize()) {
            TopBackButton(onBackClick, colors)
            Text("STRUMMING PATTERNS", fontFamily = PixelFont, fontSize = 28.sp, color = colors.text, modifier = Modifier.padding(32.dp, 16.dp))
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp).verticalScroll(rememberScrollState())) {
                patterns.forEach { p ->
                    Box(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)).background(colors.surface, RoundedCornerShape(12.dp)).padding(20.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(p.name, fontFamily = PixelFont, fontSize = 20.sp, color = colors.text)
                            Spacer(Modifier.height(12.dp))
                            Text(p.pattern, fontSize = 24.sp, color = colors.primary, letterSpacing = 4.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Spacer(Modifier.height(40.dp))
            }
        }
        if (!hasSeenTutorial) TutorialOverlay(1, 1, { "↓ STRUM ARROWS ↓\n\n⬇️ = Strum Down\n(towards the floor)\n\n⬆️ = Strum Up\n(towards the ceiling)\n\nPractice these slowly!" }, { "" }, { "" }, onTutorialComplete, colors)
    }
}
