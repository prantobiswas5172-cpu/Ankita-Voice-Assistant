package com.example.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AssistantPreferences
import com.example.engine.speech.SpeechManager
import com.example.ui.strings.AppStrings
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.launch

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    language: String = "bn"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { AssistantPreferences(context) }
    val speechManager = remember { SpeechManager(context) }

    val speechRate by preferences.speechRateFlow.collectAsState(initial = 1.0f)
    val speechPitch by preferences.speechPitchFlow.collectAsState(initial = 1.05f)
    val wakeSensitivity by preferences.wakeSensitivityFlow.collectAsState(initial = 0.75f)
    val screenOffListening by preferences.screenOffListeningFlow.collectAsState(initial = true)
    val aiMode by preferences.aiModeFlow.collectAsState(initial = false)

    var isBatteryIgnoringOptimizations by remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        mutableStateOf(pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- HEADER ---
        Column {
            Text(
                text = AppStrings.settingsTitle(language),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = AppStrings.settingsSubtitle(language),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // --- 1. LANGUAGE SETTINGS ---
        SettingsCard(
            title = AppStrings.sectionLanguage(language),
            icon = Icons.Default.Language
        ) {
            val options = listOf(
                "bn" to "বাংলা (Bengali)",
                "en" to "English (ইংরেজি)",
                "auto" to "স্বয়ংক্রিয় (Auto Detect)"
            )

            options.forEach { (code, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = language == code,
                        onClick = {
                            coroutineScope.launch { preferences.setLanguage(code) }
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = NeonCyan,
                            unselectedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = if (language == code) TextPrimary else TextSecondary,
                        fontWeight = if (language == code) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // --- 2. VOICE SYNTHESIS SETTINGS (TTS) ---
        SettingsCard(
            title = AppStrings.sectionVoice(language),
            icon = Icons.Default.RecordVoiceOver
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Speech Rate
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = AppStrings.speechSpeed(language), fontSize = 13.sp, color = TextSecondary)
                        Text(text = "${String.format("%.1f", speechRate)}x", fontSize = 13.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = { coroutineScope.launch { preferences.setSpeechRate(it) } },
                        valueRange = 0.7f..1.4f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                // Speech Pitch
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = AppStrings.speechPitch(language), fontSize = 13.sp, color = TextSecondary)
                        Text(text = "${String.format("%.2f", speechPitch)}x", fontSize = 13.sp, color = NeonPink, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = { coroutineScope.launch { preferences.setSpeechPitch(it) } },
                        valueRange = 0.8f..1.3f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPink)
                    )
                }

                // Test voice button
                OutlinedButton(
                    onClick = {
                        speechManager.speechRate = speechRate
                        speechManager.speechPitch = speechPitch
                        val testMsg = AppStrings.testVoicePhrase(language)
                        speechManager.speak(testMsg, preferBengali = language != "en")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Text(AppStrings.btnTestVoice(language), color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // --- 3. WAKE WORD SETTINGS ---
        SettingsCard(
            title = AppStrings.sectionWakeWord(language),
            icon = Icons.Default.Tune
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = AppStrings.wakePhraseTitle(language), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = AppStrings.wakePhraseDesc(language), fontSize = 11.sp, color = TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldActive.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Active", fontSize = 11.sp, color = EmeraldActive, fontWeight = FontWeight.Bold)
                    }
                }

                // Sensitivity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = AppStrings.micSensitivity(language), fontSize = 13.sp, color = TextSecondary)
                        val sensLabel = when {
                            wakeSensitivity > 0.8f -> AppStrings.sensHigh(language)
                            wakeSensitivity < 0.6f -> AppStrings.sensLow(language)
                            else -> AppStrings.sensBalanced(language)
                        }
                        Text(text = sensLabel, fontSize = 13.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = wakeSensitivity,
                        onValueChange = { coroutineScope.launch { preferences.setWakeSensitivity(it) } },
                        valueRange = 0.3f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                // Screen-off listening toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = AppStrings.screenOffTitle(language), fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(text = AppStrings.screenOffDesc(language), fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = screenOffListening,
                        onCheckedChange = { coroutineScope.launch { preferences.setScreenOffListening(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = VoidBlack, checkedTrackColor = EmeraldActive)
                    )
                }
            }
        }

        // --- 4. BATTERY OPTIMIZATION & OEM GUIDANCE (VIVO / XIAOMI) ---
        SettingsCard(
            title = AppStrings.sectionBattery(language),
            icon = Icons.Default.BatteryAlert
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = AppStrings.batteryDesc(language),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                // Battery Optimization Exemption Button
                Button(
                    onClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBatteryIgnoringOptimizations) EmeraldActive.copy(alpha = 0.2f) else CardSurfaceElevated
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isBatteryIgnoringOptimizations) EmeraldActive else NeonCyan)
                ) {
                    Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = if (isBatteryIgnoringOptimizations) EmeraldActive else NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.btnBatteryExempt(language, isBatteryIgnoringOptimizations),
                        color = if (isBatteryIgnoringOptimizations) EmeraldActive else NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Vivo OEM Guidance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonPink)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.vivoGuideTitle(language),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = AppStrings.vivoGuideSteps(language),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- 5. AI CONVERSATION & CLOUD MODULE ---
        SettingsCard(
            title = AppStrings.sectionAi(language),
            icon = Icons.Default.Psychology
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = AppStrings.aiTitle(language), fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(text = AppStrings.aiDesc(language), fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = aiMode,
                        onCheckedChange = { coroutineScope.launch { preferences.setAiMode(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = VoidBlack, checkedTrackColor = NeonPink)
                    )
                }

                Text(
                    text = AppStrings.aiSecurityNote(language),
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // --- 6. PRIVACY & PERMISSIONS ---
        SettingsCard(
            title = AppStrings.sectionPrivacy(language),
            icon = Icons.Default.Security
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = AppStrings.privacyDesc(language),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.btnOpenSettings(language), color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CardSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
