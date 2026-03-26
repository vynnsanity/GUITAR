package com.thankgod.guitartutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

class ChordtionaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("GuitarTutorPrefs", MODE_PRIVATE)
        val savedIsDark = sharedPref.getBoolean("IS_DARK_MODE", false)
        val savedFavorites = sharedPref.getStringSet("FAVORITES", emptySet()) ?: emptySet()

        setContent {
            val isDarkMode by remember { mutableStateOf(savedIsDark) }
            val colors = getAppColors(isDarkMode)
            var favoriteChords by remember { mutableStateOf(savedFavorites) }
            var selectedChord by remember { mutableStateOf("") }
            var currentScreen by remember { mutableStateOf("Chordtionary") }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    when (currentScreen) {
                        "Chordtionary" -> ChordtionaryScreen(
                            favoriteChords = favoriteChords,
                            colors = colors,
                            onToggleFavorite = { chord ->
                                favoriteChords = if (favoriteChords.contains(chord)) favoriteChords - chord else favoriteChords + chord
                                sharedPref.edit().putStringSet("FAVORITES", favoriteChords).apply()
                            },
                            onBackClick = { finish() },
                            onChordClick = { chord ->
                                selectedChord = chord
                                currentScreen = "ChordDetail"
                            }
                        )
                        "ChordDetail" -> ChordDetailScreen(
                            chordName = selectedChord,
                            colors = colors,
                            onBackClick = { currentScreen = "Chordtionary" }
                        )
                    }
                }
            }
        }
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
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_guitar), "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Column(Modifier.fillMaxSize()) {
            TopBackButton(onBackClick, colors)
            Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 48.dp, top = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Row(Modifier.background(colors.button, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).padding(24.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(chordName, fontFamily = PixelFont, fontSize = 22.sp, color = colors.text)
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
