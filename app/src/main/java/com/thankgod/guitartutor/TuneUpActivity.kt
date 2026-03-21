package com.thankgod.guitartutor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.abs
import kotlin.math.log2

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
fun TuneUpTarsosScreen(colors: AppColors, onBackClick: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    // Audio Analysis State
    var frequency by remember { mutableFloatStateOf(0f) }
    var noteName by remember { mutableStateOf("--") }
    var chordName by remember { mutableStateOf("Detecting...") }
    var isTunerMode by remember { mutableStateOf(true) }
    
    // Tuning state
    var targetString by remember { mutableStateOf("--") }
    var tuningStatus by remember { mutableStateOf("Waiting...") }
    var centsOff by remember { mutableFloatStateOf(0f) }

    val chordDetector = remember { ChordDetector() }

    // Helper to update tuning state
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
                val note = PitchToNote.getNoteName(pitch)
                noteName = note
                
                if (isTunerMode) {
                    updateTuningState(pitch)
                } else {
                    val detectedChord = chordDetector.processNote(note)
                    if (detectedChord != null) {
                        chordName = detectedChord
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

    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBackButton(onBackClick, colors)
            
            Spacer(Modifier.height(16.dp))
            
            // Mode Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tuner", color = if (isTunerMode) colors.primary else colors.textSecondary, fontFamily = PixelFont, fontSize = 16.sp)
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = !isTunerMode, 
                    onCheckedChange = { 
                        isTunerMode = !it 
                        chordDetector.clear()
                        chordName = "Detecting..."
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.primary)
                )
                Spacer(Modifier.width(12.dp))
                Text("Chords", color = if (!isTunerMode) colors.primary else colors.textSecondary, fontFamily = PixelFont, fontSize = 16.sp)
            }

            Spacer(Modifier.height(40.dp))

            if (!hasPermission) {
                Text("MIC REQUIRED", fontFamily = PixelFont, fontSize = 24.sp, color = colors.text)
                Spacer(Modifier.height(24.dp))
                BouncyButton("GRANT ACCESS", { launcher.launch(Manifest.permission.RECORD_AUDIO) }, height = 55.dp, colors = colors)
            } else {
                // Main Content Card
                Box(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .height(300.dp)
                        .background(colors.surface.copy(0.9f), RoundedCornerShape(24.dp))
                        .border(2.dp, colors.primary.copy(0.5f), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isTunerMode) {
                            Text("TARGET: $targetString", fontFamily = PixelFont, fontSize = 20.sp, color = colors.textSecondary)
                            Text(noteName, fontFamily = PixelFont, fontSize = 72.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                            Text(
                                tuningStatus, 
                                fontFamily = PixelFont, 
                                fontSize = 28.sp, 
                                color = if (tuningStatus == "IN TUNE") Color(0xFF4CAF50) else if (tuningStatus == "SHARP" || tuningStatus == "FLAT") Color(0xFFE91E63) else colors.text
                            )
                            
                            Spacer(Modifier.height(32.dp))
                            
                            // Tuner Meter
                            TunerMeter(centsOff = centsOff, colors = colors)
                        } else {
                            Text("DETECTED CHORD", fontFamily = PixelFont, fontSize = 20.sp, color = colors.textSecondary)
                            Spacer(Modifier.height(16.dp))
                            Text(chordName, fontFamily = PixelFont, fontSize = 42.sp, color = colors.primary, textAlign = TextAlign.Center, lineHeight = 50.sp)
                            
                            Spacer(Modifier.height(24.dp))
                            Text("Current Note: $noteName", fontFamily = PixelFont, fontSize = 16.sp, color = colors.textSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
                
                Text("Frequency: ${"%.1f".format(frequency)} Hz", fontFamily = PixelFont, fontSize = 16.sp, color = colors.text)
                
                if (!isTunerMode && chordName != "Detecting...") {
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.size(120.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp)) {
                        Image(painterResource(getChordImage(chordName)), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    }
                }
            }
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
