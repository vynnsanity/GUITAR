package com.thankgod.guitartutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.delay

class QuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", MODE_PRIVATE)
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)
        val savedHighScore = sharedPref.getInt("QUIZ_HIGH_SCORE", 0)
        val savedQuizTut = sharedPref.getBoolean("TUTORIAL_QUIZ", false)

        setContent {
            val isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)
            var highScore by remember { mutableIntStateOf(savedHighScore) }
            var hasSeenQuizTut by remember { mutableStateOf(savedQuizTut) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    QuizScreen(
                        highScore = highScore,
                        colors = colors,
                        hasSeenTutorial = hasSeenQuizTut,
                        onTutorialComplete = {
                            hasSeenQuizTut = true
                            sharedPref.edit().putBoolean("TUTORIAL_QUIZ", true).apply()
                        },
                        onNewHighScore = { newScore ->
                            highScore = newScore
                            sharedPref.edit().putInt("QUIZ_HIGH_SCORE", newScore).apply()
                        },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun QuizScreen(highScore: Int, colors: AppColors, hasSeenTutorial: Boolean, onTutorialComplete: () -> Unit, onNewHighScore: (Int) -> Unit, onBackClick: () -> Unit) {
    val allChords = listOf("A Major", "A Minor", "B Major", "B Minor", "C Major", "C Minor", "D Major", "D Minor", "E Major", "E Minor", "F Major", "F Minor", "G Major", "G Minor")
    var currentScore by remember { mutableIntStateOf(0) }
    var targetChord by remember { mutableStateOf(allChords.random()) }
    var options by remember { mutableStateOf((allChords.filter { it != targetChord }.shuffled().take(3) + targetChord).shuffled()) }
    var showResult by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<String?>(null) }

    val handleAnswer = { selected: String ->
        if (!showResult) {
            selectedOption = selected
            if (selected == targetChord) currentScore++ else currentScore = 0
            if (currentScore > highScore) onNewHighScore(currentScore)
            showResult = true
        }
    }

    LaunchedEffect(showResult) {
        if (showResult) {
            delay(1200)
            targetChord = allChords.random()
            options = (allChords.filter { it != targetChord }.shuffled().take(3) + targetChord).shuffled()
            selectedOption = null
            showResult = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBackButton(onBackClick, colors)
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Score: $currentScore", fontFamily = PixelFont, fontSize = 20.sp, color = colors.text)
                Text("High: $highScore", fontFamily = PixelFont, fontSize = 20.sp, color = colors.primary)
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(200.dp).background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp), Alignment.Center) {
                Image(painterResource(getChordImage(targetChord)), "Guess Chord", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(32.dp))
            Text("WHAT CHORD IS THIS?", fontFamily = PixelFont, fontSize = 24.sp, color = colors.text)
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(horizontal = 32.dp)) {
                options.chunked(2).forEach { rowOptions ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowOptions.forEach { option ->
                            val btnColor = if (showResult) {
                                when (option) {
                                    targetChord -> Color(0xFF9EBA90)
                                    selectedOption -> Color(0xFFE06666)
                                    else -> colors.button
                                }
                            } else colors.button
                            val txtColor = if (showResult) {
                                when (option) {
                                    targetChord -> Color.Black
                                    selectedOption -> Color.White
                                    else -> colors.text
                                }
                            } else colors.text
                            BouncyButton(option, { handleAnswer(option) }, Modifier.weight(1f), 60.dp, btnColor, txtColor, colors)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        if (!hasSeenTutorial) TutorialOverlay(1, 1, { "↑ CHORD DIAGRAM ↑\n\nLook at the diagram and guess the correct chord name from the options below.\n\nTry to beat your high score!" }, { "" }, { "" }, onTutorialComplete, colors)
    }
}
