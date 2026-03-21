package com.thankgod.guitartutor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", MODE_PRIVATE)
        val savedName = sharedPref.getString("USERNAME", "") ?: ""
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)
        val savedMenuTut = sharedPref.getBoolean("TUTORIAL_MENU", false)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val lastOpen = sharedPref.getString("LAST_OPEN_DATE", "") ?: ""
        var currentStreak = sharedPref.getInt("STREAK_COUNT", 0)

        if (lastOpen != todayStr) {
            if (lastOpen.isNotEmpty()) {
                try {
                    val diff = (sdf.parse(todayStr)!!.time - sdf.parse(lastOpen)!!.time) / (1000 * 60 * 60 * 24)
                    currentStreak = if (diff == 1L) currentStreak + 1 else 1
                } catch (_: Exception) { currentStreak = 1 }
            } else currentStreak = 1
            sharedPref.edit {
                putString("LAST_OPEN_DATE", todayStr).putInt("STREAK_COUNT", currentStreak)
            }
        }

        setContent {
            var isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    var currentScreen by remember { mutableStateOf(if (savedName.isNotEmpty()) "MenuScreen" else "WelcomeScreen") }
                    var userName by remember { mutableStateOf(savedName) }
                    var hasSeenMenuTut by remember { mutableStateOf(savedMenuTut) }
                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var showResetDialog by remember { mutableStateOf(false) }
                    val context = LocalContext.current

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
                                        Switch(checked = isDarkMode,
                                            onCheckedChange = { isDarkMode = it; sharedPref.edit {putBoolean("IS_DARK_MODE", it)} }, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = Color.White))
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    BouncyButton("TARSOS ANALYZER", { context.startActivity(Intent(context, TarsosActivity::class.java)) }, height = 50.dp, colors = colors)
                                    Spacer(modifier = Modifier.weight(1f))
                                    BouncyButton("RESET APP", { showResetDialog = true }, height = 50.dp, buttonColor = Color(0xFFE06666), textColor = Color.White, colors = colors)
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                        }
                    ) {
                        AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "Screen") { target ->
                            when (target) {
                                "WelcomeScreen" -> WelcomeScreen(colors) { currentScreen = "NameScreen" }
                                "NameScreen" -> NameScreen(colors) { userName = it; sharedPref.edit().putString("USERNAME", it).apply(); currentScreen = "MenuScreen" }
                                "MenuScreen" -> MenuScreen(userName, currentStreak, colors, hasSeenMenuTut, 
                                    { hasSeenMenuTut = true; sharedPref.edit().putBoolean("TUTORIAL_MENU", true).apply() }, 
                                    { context.startActivity(Intent(context, InteractiveActivity::class.java)) }, 
                                    { context.startActivity(Intent(context, TuneUpActivity::class.java)) }, 
                                    { scope.launch { drawerState.open() } })
                            }
                        }
                    }

                    if (showResetDialog) {
                        AlertDialog(
                            onDismissRequest = { showResetDialog = false },
                            title = { Text("RESET PROFILE?", fontFamily = PixelFont, fontSize = 22.sp, color = colors.text) },
                            text = { Text("This will erase your name, favorites, and streak.\n\nAre you sure?", fontFamily = PixelFont, fontSize = 16.sp, color = colors.textSecondary) },
                            confirmButton = { BouncyButton("YES", { showResetDialog = false; scope.launch { drawerState.close() }; sharedPref.edit().clear().apply(); userName = ""; currentStreak = 1; hasSeenMenuTut = false; currentScreen = "WelcomeScreen" }, height = 45.dp, buttonColor = Color(0xFF9EBA90), colors = colors) },
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
fun WelcomeScreen(colors: AppColors, onProceed: () -> Unit) {
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
                    Text("${nameText.length} / 12", fontFamily = PixelFont, fontSize = 14.sp, color = colors.text)
                }
            }
            Spacer(Modifier.weight(1.5f))
            BouncyButton("CONTINUE", { validate() }, Modifier.padding(bottom = 60.dp), height = 60.dp, colors = colors)
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
        if (!hasSeenTutorial) TutorialOverlay(1, 2, { "↓ INTERACTIVE ↓\n\nLearn chords, see finger placements, and practice with the metronome!" }, { "↓ TUNE-IT-UP ↓\n\nLet our AI listen to your strumming and check if you're in tune!" }, { "↑ SETTINGS ↑\n\nSwipe from the left edge or tap the menu icon to access Dark Mode and Reset options." }, onTutorialComplete, colors)
    }
}
