package com.thankgod.guitartutor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

class InteractiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", MODE_PRIVATE)
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)
        val savedIntTut = sharedPref.getBoolean("TUTORIAL_INTERACTIVE", false)

        setContent {
            val isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)
            var hasSeenIntTut by remember { mutableStateOf(savedIntTut) }
            val context = LocalContext.current

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    InteractiveScreen(
                        colors = colors,
                        hasSeenTutorial = hasSeenIntTut,
                        onTutorialComplete = {
                            hasSeenIntTut = true
                            sharedPref.edit().putBoolean("TUTORIAL_INTERACTIVE", true).apply()
                        },
                        onBackClick = { finish() },
                        onChordtionaryClick = { context.startActivity(Intent(context, ChordtionaryActivity::class.java)) },
                        onMetronomeClick = { context.startActivity(Intent(context, MetronomeActivity::class.java)) },
                        onQuizClick = { context.startActivity(Intent(context, QuizActivity::class.java)) },
                        onStrummingClick = { context.startActivity(Intent(context, StrummingActivity::class.java)) }
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveScreen(
    colors: AppColors,
    hasSeenTutorial: Boolean,
    onTutorialComplete: () -> Unit,
    onBackClick: () -> Unit,
    onChordtionaryClick: () -> Unit,
    onMetronomeClick: () -> Unit,
    onQuizClick: () -> Unit,
    onStrummingClick: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.bg_guitar),
            "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize()) {
            TopBackButton(onBackClick, colors)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BouncyButton("Chord-tionary", onChordtionaryClick, Modifier.fillMaxWidth(), 70.dp, colors = colors)
                Spacer(Modifier.height(24.dp))
                BouncyButton("Strum Patterns", onStrummingClick, Modifier.fillMaxWidth(), 70.dp, colors = colors)
                Spacer(Modifier.height(24.dp))
                BouncyButton("Metronome", onMetronomeClick, Modifier.fillMaxWidth(), 70.dp, colors = colors)
                Spacer(Modifier.height(24.dp))
                BouncyButton("Chord Quiz", onQuizClick, Modifier.fillMaxWidth(), 70.dp, colors = colors)
            }
        }
        if (!hasSeenTutorial) TutorialOverlay(
            1,
            4,
            { "↑ CHORD-TIONARY ↑\n\nSee chord diagrams, finger placements, and listen to how they sound." },
            { "↑ STRUM PATTERNS ↑\n\nLearn popular strumming rhythms with easy-to-follow arrows." },
            { "↓ METRONOME ↓\n\nPractice your rhythm and timing." },
            onTutorialComplete,
            colors,
            { "↓ CHORD QUIZ ↓\n\nTest your knowledge and try to beat your high score!" }
        )
    }
}
