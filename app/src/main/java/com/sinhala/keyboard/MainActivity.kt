package com.sinhala.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0A14),
                    surface = Color(0xFF15151F),
                    surfaceVariant = Color(0xFF1E1E2A),
                    primary = Color(0xFF8B5CF6),
                    onPrimary = Color.White,
                    onSurface = Color(0xFFF0F0F5),
                    onSurfaceVariant = Color(0xFFA0A0B0)
                )
            ) {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val selectedTheme by Prefs.theme.collectAsState()
    val language by Prefs.language.collectAsState()
    val vibrate by Prefs.vibrateOn.collectAsState()
    val sound by Prefs.soundOn.collectAsState()
    val popup by Prefs.popupOn.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF14141F), Color(0xFF08080F))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFD946EF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aa", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Sinhala Keyboard",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF0F0F5)
                    )
                    Text("English · සිංහල", fontSize = 13.sp, color = Color(0xFF8888A0))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151F)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Setup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF0F0F5)
                    )
                    Text(
                        "Enable keyboard and set as default input",
                        fontSize = 12.sp,
                        color = Color(0xFF8888A0)
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Keyboard Settings")
                    }
                    OutlinedButton(
                        onClick = {
                            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE)
                                as InputMethodManager
                            imm.showInputMethodPicker()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Keyboard", color = Color(0xFFF0F0F5))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Themes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF0F0F5),
                    modifier = Modifier.padding(start = 4.dp)
                )
                Themes.all.forEach { theme ->
                    ThemeCard(theme, selected = selectedTheme == theme.id) {
                        Prefs.setTheme(theme.id)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151F)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Preferences",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF0F0F5)
                    )
                    Spacer(Modifier.height(14.dp))

                    SettingSwitch(
                        "Vibration",
                        "Haptic feedback on key press",
                        vibrate
                    ) { Prefs.setVibrate(it) }

                    HorizontalDivider(
                        color = Color(0xFF1E1E2A),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingSwitch(
                        "Key sound",
                        "Click sound on key press",
                        sound
                    ) { Prefs.setSound(it) }

                    HorizontalDivider(
                        color = Color(0xFF1E1E2A),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingSwitch(
                        "Key popup",
                        "Show letter popup on press",
                        popup
                    ) { Prefs.setPopup(it) }

                    HorizontalDivider(
                        color = Color(0xFF1E1E2A),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Default language",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF0F0F5)
                            )
                            Text(
                                if (language == "en") "English" else "සිංහල",
                                fontSize = 12.sp,
                                color = Color(0xFF8888A0)
                            )
                        }
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1E2A))
                                .padding(3.dp)
                        ) {
                            listOf("en" to "EN", "si" to "සිං").forEach { (code, label) ->
                                val sel = language == code
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (sel) Color(0xFF8B5CF6) else Color.Transparent
                                        )
                                        .clickable { Prefs.setLanguage(code) }
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        label,
                                        color = if (sel) Color.White else Color(0xFFA0A0B0),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151F)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Test",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF0F0F5)
                    )
                    Text(
                        "Tap below to try your keyboard",
                        fontSize = 12.sp,
                        color = Color(0xFF8888A0)
                    )
                    Spacer(Modifier.height(4.dp))
                    var testText by remember { mutableStateOf("") }
                    TextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("Type here...", color = Color(0xFF666680)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E2A),
                            unfocusedContainerColor = Color(0xFF1E1E2A),
                            focusedTextColor = Color(0xFFF0F0F5),
                            unfocusedTextColor = Color(0xFFF0F0F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun ThemeCard(theme: KbTheme, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    if (theme.isLight) listOf(Color(0xFFEAECF0), Color(0xFFD8DAE0))
                    else listOf(Color(0xFF1A1A24), Color(0xFF101018))
                )
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF8B5CF6) else Color(0xFF2A2A3A),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        theme.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (theme.isLight) Color(0xFF1A1A24) else Color(0xFFF0F0F5)
                    )
                    Text(
                        theme.description,
                        fontSize = 12.sp,
                        color = if (theme.isLight) Color(0xFF6A6A80) else Color(0xFF8888A0)
                    )
                }
                if (selected) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("q", "w", "e", "r").forEach { k ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.keyBg)
                            .border(1.dp, theme.border, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            k,
                            fontSize = 13.sp,
                            color = theme.keyText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.linearGradient(theme.accentGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "A",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF0F0F5)
            )
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF8888A0))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Color(0xFF8888A0),
                uncheckedTrackColor = Color(0xFF1E1E2A)
            )
        )
    }
}
