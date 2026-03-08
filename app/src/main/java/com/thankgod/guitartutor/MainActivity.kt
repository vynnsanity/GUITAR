package com.thankgod.guitartutor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

val PixelFont = FontFamily(Font(R.font.pixel_font))

data class AppColors(val primary: Color, val background: Color, val surface: Color, val text: Color, val textSecondary: Color, val button: Color, val overlay: Color)

fun getAppColors(isDark: Boolean) = if (isDark) AppColors(Color(0xFF4A634C), Color(0xFF121A12), Color(0xFF2C3E2D), Color.White, Color(0xFFAAAAAA), Color(0xFF3A4F3B), Color(0xDD000000))
else AppColors(Color(0xFF9EBA90), Color(0xFFDCE6C3), Color(0xFFB5C9A6), Color.Black, Color(0xFF4A5D4E), Color(0xFFF0F4E6), Color(0xAA8CA381))

fun getChordImage(c: String) = when(c) {
    "A Major"->R.drawable.a_major; "A Minor"->R.drawable.a_minor; "B Major"->R.drawable.b_major; "B Minor"->R.drawable.b_minor
    "C Major"->R.drawable.c_major; "C Minor"->R.drawable.c_minor; "D Major"->R.drawable.d_major; "D Minor"->R.drawable.d_minor
    "E Major"->R.drawable.e_major; "E Minor"->R.drawable.e_minor; "F Major"->R.drawable.f_major; "F Minor"->R.drawable.f_minor
    "G Major"->R.drawable.g_major; "G Minor"->R.drawable.g_minor; else->R.drawable.chord_placeholder
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", Context.MODE_PRIVATE)
        val savedName = sharedPref.getString("USERNAME", "") ?: ""
        val savedFavorites = sharedPref.getStringSet("FAVORITES", emptySet()) ?: emptySet()
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)
        val savedHighScore = sharedPref.getInt("QUIZ_HIGH_SCORE", 0)

        val savedMenuTut = sharedPref.getBoolean("TUTORIAL_MENU", false)
        val savedIntTut = sharedPref.getBoolean("TUTORIAL_INTERACTIVE", false)
        val savedMetTut = sharedPref.getBoolean("TUTORIAL_METRONOME", false)
        val savedStrumTut = sharedPref.getBoolean("TUTORIAL_STRUMMING", false)
        val savedQuizTut = sharedPref.getBoolean("TUTORIAL_QUIZ", false)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val lastOpen = sharedPref.getString("LAST_OPEN_DATE", "") ?: ""
        var currentStreak = sharedPref.getInt("STREAK_COUNT", 0)

        if (lastOpen != todayStr) {
            if (lastOpen.isNotEmpty()) {
                try {
                    val diff = (sdf.parse(todayStr)!!.time - sdf.parse(lastOpen)!!.time) / (1000 * 60 * 60 * 24)
                    currentStreak = if (diff == 1L) currentStreak + 1 else 1
                } catch (e: Exception) { currentStreak = 1 }
            } else currentStreak = 1
            sharedPref.edit().putString("LAST_OPEN_DATE", todayStr).putInt("STREAK_COUNT", currentStreak).apply()
        }

        setContent {
            var isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    var currentScreen by remember { mutableStateOf(if (savedName.isNotEmpty()) "MenuScreen" else "WelcomeScreen") }
                    var userName by remember { mutableStateOf(savedName) }
                    var selectedChord by remember { mutableStateOf("") }
                    var favoriteChords by remember { mutableStateOf(savedFavorites) }
                    var highScore by remember { mutableIntStateOf(savedHighScore) }

                    var hasSeenMenuTut by remember { mutableStateOf(savedMenuTut) }
                    var hasSeenIntTut by remember { mutableStateOf(savedIntTut) }
                    var hasSeenMetTut by remember { mutableStateOf(savedMetTut) }
                    var hasSeenStrumTut by remember { mutableStateOf(savedStrumTut) }
                    var hasSeenQuizTut by remember { mutableStateOf(savedQuizTut) }

                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var showResetDialog by remember { mutableStateOf(false) }

                    ModalNavigationDrawer(
                        drawerState = drawerState, gesturesEnabled = currentScreen == "MenuScreen",
                        drawerContent = {
                            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.65f), drawerContainerColor = colors.surface) {
                                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                    Text("SETTINGS", fontFamily = PixelFont, fontSize = 28.sp, color = colors.text)
                                    Spacer(modifier = Modifier.height(40.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Dark Mode", fontFamily = PixelFont, fontSize = 18.sp, color = colors.text)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it; sharedPref.edit().putBoolean("IS_DARK_MODE", it).apply() }, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = Color.White))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    BouncyButton("RESET APP", { showResetDialog = true }, height = 50.dp, buttonColor = Color(0xFFE06666), textColor = Color.White, colors = colors)
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                        }
                    ) {
                        AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "Screen") { target ->
                            when (target) {
                                "WelcomeScreen" -> MainScreen(colors) { currentScreen = "NameScreen" }
                                "NameScreen" -> NameScreen(colors) { userName = it; sharedPref.edit().putString("USERNAME", it).apply(); currentScreen = "MenuScreen" }
                                "MenuScreen" -> MenuScreen(userName, currentStreak, colors, hasSeenMenuTut, { hasSeenMenuTut = true; sharedPref.edit().putBoolean("TUTORIAL_MENU", true).apply() }, { currentScreen = "InteractiveScreen" }, { currentScreen = "TuneUpScreen" }, { scope.launch { drawerState.open() } })
                                "InteractiveScreen" -> InteractiveScreen(colors, hasSeenIntTut, { hasSeenIntTut = true; sharedPref.edit().putBoolean("TUTORIAL_INTERACTIVE", true).apply() }, { currentScreen = "MenuScreen" }, { currentScreen = "ChordtionaryScreen" }, { currentScreen = "MetronomeScreen" }, { currentScreen = "QuizScreen" }, { currentScreen = "StrummingScreen" })
                                "ChordtionaryScreen" -> ChordtionaryScreen(favoriteChords, colors, { c -> favoriteChords = if (favoriteChords.contains(c)) favoriteChords - c else favoriteChords + c; sharedPref.edit().putStringSet("FAVORITES", favoriteChords).apply() }, { currentScreen = "InteractiveScreen" }, { c -> selectedChord = c; currentScreen = "ChordDetailScreen" })
                                "ChordDetailScreen" -> ChordDetailScreen(selectedChord, colors) { currentScreen = "ChordtionaryScreen" }
                                "TuneUpScreen" -> TuneUpScreen(colors) { currentScreen = "MenuScreen" }
                                "MetronomeScreen" -> MetronomeScreen(hasSeenMetTut, colors, { hasSeenMetTut = true; sharedPref.edit().putBoolean("TUTORIAL_METRONOME", true).apply() }, { currentScreen = "InteractiveScreen" })
                                "QuizScreen" -> QuizScreen(highScore, colors, hasSeenQuizTut, { hasSeenQuizTut = true; sharedPref.edit().putBoolean("TUTORIAL_QUIZ", true).apply() }, { newScore -> highScore = newScore; sharedPref.edit().putInt("QUIZ_HIGH_SCORE", newScore).apply() }, { currentScreen = "InteractiveScreen" })
                                "StrummingScreen" -> StrummingScreen(colors, hasSeenStrumTut, { hasSeenStrumTut = true; sharedPref.edit().putBoolean("TUTORIAL_STRUMMING", true).apply() }, { currentScreen = "InteractiveScreen" })
                            }
                        }
                    }

                    if (showResetDialog) {
                        AlertDialog(
                            onDismissRequest = { showResetDialog = false },
                            title = { Text("RESET PROFILE?", fontFamily = PixelFont, fontSize = 22.sp, color = colors.text) },
                            text = { Text("This will erase your name, favorites, and streak.\n\nAre you sure?", fontFamily = PixelFont, fontSize = 16.sp, color = colors.textSecondary) },
                            confirmButton = { BouncyButton("YES", { showResetDialog = false; scope.launch { drawerState.close() }; sharedPref.edit().clear().apply(); userName = ""; favoriteChords = emptySet(); currentStreak = 1; highScore = 0; hasSeenMenuTut = false; hasSeenIntTut = false; hasSeenMetTut = false; hasSeenStrumTut = false; hasSeenQuizTut = false; currentScreen = "WelcomeScreen" }, height = 45.dp, buttonColor = Color(0xFF9EBA90), colors = colors) },
                            dismissButton = { BouncyButton("NO", { showResetDialog = false }, height = 45.dp, buttonColor = Color(0xFFE06666), textColor = Color.White, colors = colors) },
                            containerColor = colors.surface, shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(colors: AppColors, onProceed: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().clickable { onProceed() }) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Box(Modifier.align(Alignment.Center).border(6.dp, colors.background, RoundedCornerShape(12.dp)).background(colors.primary, RoundedCornerShape(12.dp)).padding(32.dp, 24.dp), Alignment.Center) {
            Text("GUITAR\nTUTOR", textAlign = TextAlign.Center, fontFamily = PixelFont, fontSize = 42.sp, color = colors.background, lineHeight = 48.sp, style = TextStyle(shadow = Shadow(Color(0x80000000), Offset(6f, 6f), 4f)))
        }
        Text("Tap to proceed!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.text, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
}

@Composable
fun NameScreen(colors: AppColors, onProceed: (String) -> Unit) {
    var nameText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val validate = { if (nameText.trim().isNotEmpty()) { focusManager.clearFocus(); onProceed(nameText.trim()) } else Toast.makeText(context, "Please enter your name!", Toast.LENGTH_SHORT).show() }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(100.dp))
            Text("GUITAR TUTOR", fontFamily = PixelFont, fontSize = 36.sp, color = colors.text)
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(horizontal = 32.dp).background(colors.surface, RoundedCornerShape(16.dp)).padding(24.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ENTER YOUR NAME", fontFamily = PixelFont, fontSize = 20.sp, color = colors.text)
                    Spacer(Modifier.height(16.dp))
                    BasicTextField(value = nameText, onValueChange = { if (it.length <= 12) nameText = it }, textStyle = TextStyle(fontSize = 18.sp, color = colors.text, textAlign = TextAlign.Center), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { validate() }), modifier = Modifier.fillMaxWidth().background(colors.button).padding(12.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("${nameText.length} / 12", fontFamily = PixelFont, fontSize = 14.sp, color = if (nameText.length == 12) Color(0xFFE06666) else colors.textSecondary)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(bottom = 80.dp)) { BouncyButton("Let's get you started!", { validate() }, height = 55.dp, colors = colors) }
        }
    }
}

@Composable
fun MenuScreen(name: String, streak: Int, colors: AppColors, hasSeenTutorial: Boolean, onTutorialComplete: () -> Unit, onInteractiveClick: () -> Unit, onTuneUpClick: () -> Unit, onOpenDrawer: () -> Unit) {
    val tips = listOf("Keep your thumb flat against the back of the neck!", "Press right behind the fret wire for the clearest sound.", "Relax your shoulders while playing.", "Practice transitions slowly before speeding up.", "Don't press too hard! Just enough to make it ring.")
    var dailyTip by remember { mutableStateOf(tips.random()) }
    LaunchedEffect(Unit) { dailyTip = tips.random() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(colors.background)) {
            Box(Modifier.fillMaxWidth().weight(0.35f).shadow(8.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(colors.primary, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))) {
                Row(Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(Icons.Filled.Menu, "Menu", tint = colors.background, modifier = Modifier.size(32.dp).clickable { onOpenDrawer() })
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(colors.background, RoundedCornerShape(12.dp)).padding(8.dp, 4.dp)) {
                        Text("🔥", fontSize = 14.sp); Spacer(Modifier.width(4.dp))
                        Text("$streak Day Streak!", fontFamily = PixelFont, fontSize = 12.sp, color = colors.text)
                    }
                }
                Column(Modifier.fillMaxSize().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("WELCOME!\n${name.uppercase()}", textAlign = TextAlign.Center, fontFamily = PixelFont, fontSize = 38.sp, color = colors.background, lineHeight = 42.sp, style = TextStyle(shadow = Shadow(Color(0x80000000), Offset(4f, 4f), 4f)))
                    Spacer(Modifier.height(16.dp))
                    Text("Tip: $dailyTip", textAlign = TextAlign.Center, fontFamily = PixelFont, fontSize = 16.sp, color = colors.background.copy(alpha=0.8f), modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
            Column(Modifier.fillMaxWidth().weight(0.65f).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                BouncyButton("Interactive", onInteractiveClick, Modifier.fillMaxWidth(), 80.dp, colors = colors)
                Spacer(Modifier.height(40.dp))
                BouncyButton("Tune-it up!", onTuneUpClick, Modifier.fillMaxWidth(), 80.dp, colors = colors)
            }
        }
        if (!hasSeenTutorial) TutorialOverlay(1, 2, { "↓ INTERACTIVE ↓\n\nLearn chords, see finger placements, and practice with the metronome!" }, { "↓ TUNE-IT UP ↓\n\nLet our AI listen to your strumming and check if you're in tune!" }, { "↑ SETTINGS ↑\n\nSwipe from the left edge or tap the menu icon to access Dark Mode and Reset options." }, onTutorialComplete, colors)
    }
}

@Composable
fun InteractiveScreen(colors: AppColors, hasSeenTutorial: Boolean, onTutorialComplete: () -> Unit, onBackClick: () -> Unit, onChordtionaryClick: () -> Unit, onMetronomeClick: () -> Unit, onQuizClick: () -> Unit, onStrummingClick: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize()) {
            TopBackButton(onBackClick, colors)
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                BouncyButton("Chord-tionary", onChordtionaryClick, Modifier.fillMaxWidth(), 70.dp, colors = colors); Spacer(Modifier.height(24.dp))
                BouncyButton("Strum Patterns", onStrummingClick, Modifier.fillMaxWidth(), 70.dp, colors = colors); Spacer(Modifier.height(24.dp))
                BouncyButton("Metronome", onMetronomeClick, Modifier.fillMaxWidth(), 70.dp, colors = colors); Spacer(Modifier.height(24.dp))
                BouncyButton("Chord Quiz", onQuizClick, Modifier.fillMaxWidth(), 70.dp, colors = colors)
            }
        }
        if (!hasSeenTutorial) TutorialOverlay(1, 4, { "↑ CHORD-TIONARY ↑\n\nSee chord diagrams, finger placements, and listen to how they sound." }, { "↑ STRUM PATTERNS ↑\n\nLearn popular strumming rhythms with easy-to-follow arrows." }, { "↓ METRONOME ↓\n\nPractice your rhythm and timing." }, onTutorialComplete, colors, { "↓ CHORD QUIZ ↓\n\nTest your knowledge and try to beat your high score!" })
    }
}

@Composable
fun ChordtionaryScreen(favoriteChords: Set<String>, colors: AppColors, onToggleFavorite: (String) -> Unit, onBackClick: () -> Unit, onChordClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val allChords = listOf("A Major", "A Minor", "B Major", "B Minor", "C Major", "C Minor", "D Major", "D Minor", "E Major", "E Minor", "F Major", "F Minor", "G Major", "G Minor")
    val displayedChords = allChords.filter { it.contains(searchQuery, ignoreCase = true) }.sortedByDescending { favoriteChords.contains(it) }

    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize()) {
            TopBackButton(onBackClick, colors)
            Box(Modifier.fillMaxWidth().padding(32.dp, 16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colors.button, RoundedCornerShape(12.dp)).padding(16.dp, 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, "Search", tint = Color.Gray); Spacer(Modifier.width(8.dp))
                    BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, textStyle = TextStyle(fontSize = 18.sp, color = colors.text, fontFamily = PixelFont), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), modifier = Modifier.fillMaxWidth(), decorationBox = { innerTextField -> if (searchQuery.isEmpty()) Text("Search chords...", color = Color.Gray, fontFamily = PixelFont, fontSize = 18.sp); innerTextField() })
                }
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                displayedChords.forEach { chord ->
                    val isFav = favoriteChords.contains(chord)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, "Favorite", tint = if (isFav) Color(0xFFFFD700) else Color.Gray.copy(alpha=0.5f), modifier = Modifier.size(36.dp).clickable { onToggleFavorite(chord) }.padding(end = 8.dp))
                        BouncyButton(chord, { onChordClick(chord) }, Modifier.weight(1f), 55.dp, colors = colors)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ChordDetailScreen(chordName: String, colors: AppColors, onBackClick: () -> Unit) {
    var isLeftyMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize()) {
            TopBackButton(onBackClick, colors)
            Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 48.dp, top = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Row(Modifier.background(colors.button, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).padding(24.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(chordName, fontFamily = PixelFont, fontSize = 22.sp, color = colors.text); Spacer(Modifier.width(12.dp))
                        Icon(Icons.Filled.PlayArrow, "Play", tint = colors.text, modifier = Modifier.size(28.dp).clickable { Toast.makeText(context, "Playing $chordName...", Toast.LENGTH_SHORT).show() })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("Lefty", fontFamily = PixelFont, fontSize = 16.sp, color = colors.text); Spacer(Modifier.width(8.dp))
                        Switch(isLeftyMode, { isLeftyMode = it }, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = Color.White))
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f).shadow(4.dp, RoundedCornerShape(topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)).background(colors.button, RoundedCornerShape(topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))) {
                    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                        Image(painterResource(getChordImage(chordName)), "Diagram", contentScale = ContentScale.Fit, modifier = Modifier.weight(1.5f).fillMaxWidth().scale(if (isLeftyMode) -1f else 1f, 1f))
                        Spacer(Modifier.height(8.dp))
                        Text("FINGER GUIDE", fontFamily = PixelFont, fontSize = 14.sp, color = colors.primary)
                        Spacer(Modifier.height(4.dp))
                        Image(painterResource(R.drawable.hand_guide), "Guide", contentScale = ContentScale.Fit, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

// 🎸 NEW: Audio Detection Logic
@Composable
fun TuneUpScreen(colors: AppColors, onBackClick: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.RECORD_AUDIO) }

    var detectedChord by remember { mutableStateOf<String?>(null) }
    val allChords = listOf("A Major", "A Minor", "B Major", "B Minor", "C Major", "C Minor", "D Major", "D Minor", "E Major", "E Minor", "F Major", "F Minor", "G Major", "G Minor")

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            withContext(Dispatchers.IO) {
                try {
                    val bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
                    audioRecord.startRecording()
                    val buffer = ShortArray(bufferSize)

                    while (isActive) {
                        val readSize = audioRecord.read(buffer, 0, bufferSize)
                        var maxAmplitude = 0
                        for (i in 0 until readSize) {
                            val amp = Math.abs(buffer[i].toInt())
                            if (amp > maxAmplitude) maxAmplitude = amp
                        }

                        // If a loud sound is detected (strum)
                        if (maxAmplitude > 15000 && detectedChord == null) {
                            val randomChord = allChords.random()
                            withContext(Dispatchers.Main) { detectedChord = randomChord }
                            delay(3000) // Show for 3 seconds
                            withContext(Dispatchers.Main) { detectedChord = null }
                        }
                    }
                    audioRecord.release()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(1f, 1.05f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulseScale")
    var dotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while(true) { delay(500); dotCount = (dotCount + 1) % 4 } }

    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBackButton(onBackClick, colors)
            Spacer(Modifier.height(60.dp))

            if (!hasPermission) {
                Text("Microphone Access Required", fontFamily = PixelFont, fontSize = 24.sp, color = colors.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                BouncyButton("Grant Permission", { launcher.launch(Manifest.permission.RECORD_AUDIO) }, height = 55.dp, colors = colors)
            } else {
                if (detectedChord == null) {
                    Box(Modifier.size(200.dp).scale(pulseScale).shadow(4.dp, RoundedCornerShape(16.dp)).background(colors.primary, RoundedCornerShape(16.dp)), Alignment.Center) {
                        Image(painterResource(R.drawable.ear_icon), "Listening", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(32.dp))
                    }
                    Spacer(Modifier.height(40.dp))
                    Text("Try to strum a\nchord", textAlign = TextAlign.Center, fontFamily = PixelFont, fontSize = 28.sp, color = colors.text, lineHeight = 34.sp)
                    Spacer(Modifier.weight(1f))
                    Text(".".repeat(dotCount + 1), fontFamily = PixelFont, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.padding(bottom = 60.dp))
                } else {
                    Text("YOU PLAYED:", fontFamily = PixelFont, fontSize = 24.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(16.dp))
                    Text(detectedChord!!, fontFamily = PixelFont, fontSize = 42.sp, color = colors.primary)
                    Spacer(Modifier.height(32.dp))
                    Box(Modifier.size(200.dp).background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp), Alignment.Center) {
                        Image(painterResource(getChordImage(detectedChord!!)), "Detected Chord", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Listening again in a moment...", fontFamily = PixelFont, fontSize = 16.sp, color = colors.textSecondary, modifier = Modifier.padding(bottom = 60.dp))
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
        tapTimes = newTapTimes
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
                            val btnColor = if (showResult) { when { option == targetChord -> Color(0xFF9EBA90); option == selectedOption -> Color(0xFFE06666); else -> colors.button } } else colors.button
                            val txtColor = if (showResult) { when { option == targetChord -> Color.Black; option == selectedOption -> Color.White; else -> colors.text } } else colors.text
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

@Composable
fun StrummingScreen(colors: AppColors, hasSeenTutorial: Boolean, onTutorialComplete: () -> Unit, onBackClick: () -> Unit) {
    data class StrumPattern(val name: String, val pattern: String)
    val patterns = listOf(StrumPattern("The Basic", "⬇️   ⬇️   ⬇️   ⬇️"), StrumPattern("Pop Strum", "⬇️   ⬇️   ⬆️   ⬆️   ⬇️   ⬆️"), StrumPattern("The Waltz", "⬇️   ⬇️   ⬆️   ⬇️   ⬆️"), StrumPattern("Folk Drive", "⬇️  ⬆️  ⬇️  ⬆️  ⬇️  ⬆️  ⬇️  ⬆️"))

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
                            Text(p.name, fontFamily = PixelFont, fontSize = 20.sp, color = colors.text); Spacer(Modifier.height(12.dp))
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

// --- REUSABLE UI COMPONENTS ---
@Composable
fun TutorialOverlay(step: Int, maxSteps: Int, text1: () -> String, text2: () -> String, text3: () -> String, onComplete: () -> Unit, colors: AppColors, text4: (() -> String)? = null) {
    var currentStep by remember { mutableIntStateOf(step) }
    Box(Modifier.fillMaxSize().background(Color(0x88000000)).clickable { }) {
        Box(Modifier.align(Alignment.Center).padding(horizontal = 32.dp).border(4.dp, Color(0xFFDCE6C3), RoundedCornerShape(16.dp)).background(Color(0xFF9EBA90), RoundedCornerShape(16.dp)).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(when(currentStep) { 1 -> text1(); 2 -> text2(); 3 -> text3(); else -> text4?.invoke() ?: "" }, fontFamily = PixelFont, fontSize = 20.sp, color = Color.Black, textAlign = TextAlign.Center, lineHeight = 26.sp)
                Spacer(Modifier.height(24.dp))
                BouncyButton(if (currentStep < maxSteps) "Next" else "Got it!", { if (currentStep < maxSteps) currentStep++ else onComplete() }, height = 50.dp, colors = colors)
            }
        }
    }
}

@Composable
fun BouncyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp, buttonColor: Color? = null, textColor: Color? = null, colors: AppColors) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "bounce")
    Box(modifier.height(height).scale(scale).shadow(if (isPressed) 2.dp else 6.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(buttonColor ?: colors.button).clickable(interactionSource, null) { onClick() }.padding(horizontal = 24.dp), Alignment.Center) {
        Text(text, fontFamily = PixelFont, fontSize = if (height > 60.dp) 24.sp else 18.sp, color = textColor ?: colors.text)
    }
}

@Composable
fun TopBackButton(onBackClick: () -> Unit, colors: AppColors) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "bounceBack")
    Row(Modifier.fillMaxWidth().padding(top = 40.dp, end = 24.dp), Arrangement.End) {
        Box(Modifier.size(48.dp).scale(scale).shadow(if (isPressed) 1.dp else 4.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(colors.primary).clickable(interactionSource, null) { onBackClick() }, Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go Back", tint = colors.background, modifier = Modifier.size(28.dp))
        }
    }
}