package com.thankgod.guitartutor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.log2

enum class PracticeState { MENU, TUNER, SONG_SELECT, PRACTICE }

data class Song(val name: String, val chords: List<String>)

class TuneUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", MODE_PRIVATE)
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)

        setContent {
            val isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    TuneUpTarsosScreen(colors = colors) { finish() }
                }
            }
        }
    }
}

@Composable
fun TuneUpTarsosScreen(colors: AppColors, onExit: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    // Navigation State
    var currentScreen by remember { mutableStateOf(PracticeState.MENU) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    // Audio Analysis State
    var frequency by remember { mutableFloatStateOf(0f) }
    var noteName by remember { mutableStateOf("--") }
    var detectedChord by remember { mutableStateOf("--") }
    
    // Tuning state
    var targetString by remember { mutableStateOf("--") }
    var tuningStatus by remember { mutableStateOf("Waiting...") }
    var centsOff by remember { mutableFloatStateOf(0f) }

    val chordDetector = remember { ChordDetector() }

    val songs = remember {
        listOf(
            Song("Basic G-C-D", listOf("G Major", "C Major", "D Major")),
            Song("Minor Soul", listOf("A Minor", "D Minor", "E Minor")),
            Song("The Blues", listOf("A Major", "D Major", "E Major")),
            Song("Pop Starter", listOf("C Major", "G Major", "A Minor", "F Major"))
        )
    }

    val updateTuningState: (Float) -> Unit = { pitch ->
        var closestString = "--"
        var minDiff = Float.MAX_VALUE
        var targetFreq = 0f

        for ((name, freq) in PitchToNote.guitarStrings) {
            val diff = abs(pitch - freq)
            if (diff < minDiff) {
                minDiff = diff
                closestString = name
                targetFreq = freq
            }
        }

        targetString = closestString
        if (targetFreq > 0) {
            val cents = (1200 * log2(pitch.toDouble() / targetFreq.toDouble())).toFloat()
            centsOff = cents.coerceIn(-50f, 50f)
            tuningStatus = when {
                cents > 3f -> "SHARP"
                cents < -3f -> "FLAT"
                else -> "IN TUNE"
            }
        }
    }

    val audioProcessor = remember {
        AudioProcessor { pitch, probability ->
            if (probability > 0.85f && pitch > 0) {
                frequency = pitch
                noteName = PitchToNote.getNoteName(pitch)
                
                if (currentScreen == PracticeState.TUNER) {
                    updateTuningState(pitch)
                } else {
                    val result = chordDetector.processNote(noteName)
                    if (result != null) {
                        detectedChord = result
                    }
                }
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            audioProcessor.start()
        } else {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { audioProcessor.stop() }
    }

    BackHandler {
        when (currentScreen) {
            PracticeState.MENU -> onExit()
            PracticeState.TUNER -> currentScreen = PracticeState.MENU
            PracticeState.SONG_SELECT -> currentScreen = PracticeState.MENU
            PracticeState.PRACTICE -> currentScreen = PracticeState.SONG_SELECT
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        
        Column(Modifier.fillMaxSize()) {
            TopBackButton({
                when (currentScreen) {
                    PracticeState.MENU -> onExit()
                    PracticeState.TUNER -> currentScreen = PracticeState.MENU
                    PracticeState.SONG_SELECT -> currentScreen = PracticeState.MENU
                    PracticeState.PRACTICE -> currentScreen = PracticeState.SONG_SELECT
                }
            }, colors)

            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier.fillMaxSize(),
                label = "ScreenTransition"
            ) { state ->
                when (state) {
                    PracticeState.MENU -> PracticeMenu(colors) { currentScreen = it }
                    PracticeState.TUNER -> TunerModule(noteName, targetString, tuningStatus, centsOff, frequency, colors)
                    PracticeState.SONG_SELECT -> SongSelectModule(songs, colors) {
                        selectedSong = it
                        currentScreen = PracticeState.PRACTICE
                    }
                    PracticeState.PRACTICE -> SongPracticeModule(selectedSong!!, detectedChord, colors)
                }
            }
        }
    }
}

@Composable
fun PracticeMenu(colors: AppColors, onNavigate: (PracticeState) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PRACTICE TOOL", fontFamily = PixelFont, fontSize = 32.sp, color = colors.primary)
        Spacer(Modifier.height(48.dp))
        BouncyButton(
            text = "INSTRUMENT TUNER",
            onClick = { onNavigate(PracticeState.TUNER) },
            modifier = Modifier.fillMaxWidth(0.8f),
            height = 80.dp,
            colors = colors
        )
        Spacer(Modifier.height(32.dp))
        BouncyButton(
            text = "SONG PRACTICE",
            onClick = { onNavigate(PracticeState.SONG_SELECT) },
            modifier = Modifier.fillMaxWidth(0.8f),
            height = 80.dp,
            colors = colors
        )
    }
}

@Composable
fun TunerModule(
    note: String,
    target: String,
    status: String,
    centsOff: Float,
    freq: Float,
    colors: AppColors
) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("GUITAR TUNER", fontFamily = PixelFont, fontSize = 24.sp, color = colors.primary)
        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.fillMaxWidth().height(320.dp).background(colors.surface.copy(0.9f), RoundedCornerShape(24.dp)).border(2.dp, colors.primary.copy(0.5f), RoundedCornerShape(24.dp)).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TARGET: $target", fontFamily = PixelFont, fontSize = 20.sp, color = colors.textSecondary)
                Text(note, fontFamily = PixelFont, fontSize = 80.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                Text(
                    status, 
                    fontFamily = PixelFont, 
                    fontSize = 28.sp, 
                    color = if (status == "IN TUNE") Color(0xFF4CAF50) else if (status == "SHARP" || status == "FLAT") Color(0xFFE91E63) else colors.text
                )
                Spacer(Modifier.height(40.dp))
                TunerMeter(centsOff = centsOff, colors = colors)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Frequency: ${"%.1f".format(freq)} Hz", fontFamily = PixelFont, fontSize = 16.sp, color = colors.text)
    }
}

@Composable
fun SongSelectModule(songs: List<Song>, colors: AppColors, onSongSelected: (Song) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SELECT A SONG", fontFamily = PixelFont, fontSize = 24.sp, color = colors.primary)
        Spacer(Modifier.height(32.dp))
        songs.forEach { song ->
            BouncyButton(
                text = song.name,
                onClick = { onSongSelected(song) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                height = 60.dp,
                colors = colors
            )
        }
    }
}

@Composable
fun SongPracticeModule(song: Song, detectedChord: String, colors: AppColors) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val targetChord = song.chords[currentIndex]
    val isCorrect = detectedChord == targetChord

    LaunchedEffect(isCorrect) {
        if (isCorrect) {
            // Give user time to see "PERFECT"
            delay(1500)
            if (currentIndex < song.chords.size - 1) {
                currentIndex++
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(song.name.uppercase(), fontFamily = PixelFont, fontSize = 20.sp, color = colors.primary)
        Spacer(Modifier.height(24.dp))
        
        // Chord sequence display
        LazyRow(Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.Center) {
            itemsIndexed(song.chords) { index, chord ->
                Box(
                    Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (index == currentIndex) colors.primary else colors.surface.copy(0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(chord, fontFamily = PixelFont, fontSize = 14.sp, color = if (index == currentIndex) colors.background else colors.text)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Box(
            Modifier.fillMaxWidth().height(300.dp).background(if (isCorrect) Color(0xFF4CAF50).copy(0.2f) else colors.surface.copy(0.9f), RoundedCornerShape(24.dp))
                .border(3.dp, if (isCorrect) Color(0xFF4CAF50) else colors.primary.copy(0.5f), RoundedCornerShape(24.dp)).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TARGET CHORD", fontFamily = PixelFont, fontSize = 18.sp, color = colors.textSecondary)
                Text(targetChord, fontFamily = PixelFont, fontSize = 48.sp, color = if (isCorrect) Color(0xFF2E7D32) else colors.primary, textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(32.dp))
                
                if (isCorrect) {
                    Text("PERFECT!", fontFamily = PixelFont, fontSize = 32.sp, color = Color(0xFF2E7D32))
                } else {
                    Text("HEARING: $detectedChord", fontFamily = PixelFont, fontSize = 18.sp, color = colors.textSecondary)
                    Text("Keep strumming...", fontFamily = PixelFont, fontSize = 14.sp, color = colors.text.copy(0.6f))
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        if (isCorrect) {
            Text("Nice job! Moving to next chord...", fontFamily = PixelFont, fontSize = 14.sp, color = Color(0xFF2E7D32))
        }

        // Image Hint
        Spacer(Modifier.height(16.dp))
        Box(Modifier.size(100.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp)) {
            Image(painterResource(getChordImage(targetChord)), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun TunerMeter(centsOff: Float, colors: AppColors) {
    val animatedCents by animateFloatAsState(targetValue = centsOff, label = "cents")
    
    Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val centerX = width / 2
            
            // Draw horizontal line
            drawLine(colors.textSecondary.copy(0.3f), Offset(0f, centerY), Offset(width, centerY), strokeWidth = 2.dp.toPx())
            
            // Draw center notch
            drawLine(colors.primary, Offset(centerX, centerY - 15.dp.toPx()), Offset(centerX, centerY + 15.dp.toPx()), strokeWidth = 4.dp.toPx())
            
            // Draw indicator
            val indicatorX = centerX + (animatedCents / 50f) * (width / 2)
            drawCircle(
                color = if (abs(centsOff) < 3) Color(0xFF4CAF50) else Color(0xFFE91E63),
                radius = 7.dp.toPx(),
                center = Offset(indicatorX, centerY)
            )
        }
    }
}
