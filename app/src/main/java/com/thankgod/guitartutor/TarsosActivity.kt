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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.log2

class TarsosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", MODE_PRIVATE)
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)

        setContent {
            val isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)
            
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    TarsosScreen(colors)
                }
            }
        }
    }
}

@Composable
fun TarsosScreen(colors: AppColors) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

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
    val updateTuning: (Float) -> Unit = { pitch ->
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
                cents > 2f -> "SHARP"
                cents < -2f -> "FLAT"
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
                    updateTuning(pitch)
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

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TARSOS ANALYZER", fontFamily = PixelFont, fontSize = 24.sp, color = colors.primary)
        
        Spacer(Modifier.height(32.dp))
        
        // Mode Toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tuner", color = if (isTunerMode) colors.primary else colors.textSecondary, fontWeight = if (isTunerMode) FontWeight.Bold else FontWeight.Normal)
            Spacer(Modifier.width(8.dp))
            Switch(checked = !isTunerMode, onCheckedChange = { 
                isTunerMode = !it 
                chordDetector.clear()
                chordName = "Detecting..."
            }, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary))
            Spacer(Modifier.width(8.dp))
            Text("Chords", color = if (!isTunerMode) colors.primary else colors.textSecondary, fontWeight = if (!isTunerMode) FontWeight.Bold else FontWeight.Normal)
        }

        Spacer(Modifier.height(40.dp))

        // Main Display Area
        Box(Modifier.fillMaxWidth().weight(1f).background(colors.surface, RoundedCornerShape(24.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isTunerMode) {
                    Text("Target: $targetString", fontSize = 20.sp, color = colors.textSecondary, fontFamily = PixelFont)
                    Text(noteName, fontSize = 80.sp, fontWeight = FontWeight.Bold, color = colors.primary, fontFamily = PixelFont)
                    Text(tuningStatus, fontSize = 24.sp, color = if (tuningStatus == "IN TUNE") Color(0xFF4CAF50) else Color(0xFFE91E63), fontFamily = PixelFont)
                    
                    Spacer(Modifier.height(40.dp))
                    
                    // Tuning Meter
                    TuningMeter(centsOff = centsOff, colors = colors)
                } else {
                    Text("CHORD DETECTED", fontSize = 20.sp, color = colors.textSecondary, fontFamily = PixelFont)
                    Text(chordName, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = colors.primary, textAlign = TextAlign.Center, fontFamily = PixelFont)
                    
                    Spacer(Modifier.height(20.dp))
                    Text("Current Note: $noteName", fontSize = 16.sp, color = colors.textSecondary, fontFamily = PixelFont)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        
        // Frequency info
        Text("Frequency: ${"%.2f".format(frequency)} Hz", color = colors.textSecondary, fontFamily = PixelFont)
        
        Spacer(Modifier.height(16.dp))
        BouncyButton("CLOSE", { (context as? ComponentActivity)?.finish() }, height = 50.dp, colors = colors)
    }
}

@Composable
fun TuningMeter(centsOff: Float, colors: AppColors) {
    val animatedCents by animateFloatAsState(targetValue = centsOff, label = "cents")
    
    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val centerX = width / 2
            
            // Draw scale
            drawLine(colors.textSecondary.copy(0.3f), Offset(0f, centerY), Offset(width, centerY), strokeWidth = 2.dp.toPx())
            drawLine(colors.primary, Offset(centerX, centerY - 15.dp.toPx()), Offset(centerX, centerY + 15.dp.toPx()), strokeWidth = 4.dp.toPx())
            
            // Draw indicator
            val indicatorX = centerX + (animatedCents / 50f) * (width / 2)
            drawCircle(
                color = if (abs(centsOff) < 2) Color(0xFF4CAF50) else Color(0xFFE91E63),
                radius = 8.dp.toPx(),
                center = Offset(indicatorX, centerY)
            )
        }
    }
}
