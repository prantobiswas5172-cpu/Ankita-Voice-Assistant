package com.example.ui.screen

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AssistantPreferences
import com.example.service.AnkitaVoiceService
import com.example.service.AssistantVisualState
import com.example.ui.orb.VoiceOrb
import com.example.ui.strings.AppStrings
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    language: String = "bn",
    onRequestMicrophonePermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { AssistantPreferences(context) }

    val isServiceRunning by AnkitaVoiceService.isRunning.collectAsState()
    val assistantState by AnkitaVoiceService.assistantState.collectAsState()
    val audioLevel by AnkitaVoiceService.audioLevel.collectAsState()
    val lastTranscript by AnkitaVoiceService.lastTranscript.collectAsState()
    val lastResponse by AnkitaVoiceService.lastResponse.collectAsState()

    val isMasterEnabled by preferences.assistantEnabledFlow.collectAsState(initial = true)

    var batteryPercent by remember { mutableIntStateOf(92) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        bm?.let {
            batteryPercent = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            isCharging = it.isCharging
        }
    }

    val toggleAssistant: (Boolean) -> Unit = { enabled ->
        coroutineScope.launch {
            preferences.setAssistantEnabled(enabled)
            val intent = Intent(context, AnkitaVoiceService::class.java).apply {
                action = if (enabled) AnkitaVoiceService.ACTION_START else AnkitaVoiceService.ACTION_STOP
            }
            if (enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoidBlack, DeepNavy, VoidBlack)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TOP HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ANKITA",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3300F0FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VOICE AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
                Text(
                    text = AppStrings.appSubtitle(language),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Battery Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery",
                    tint = if (isCharging) EmeraldActive else NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = AppStrings.formatBattery(language, batteryPercent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- MASTER SERVICE SWITCH CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("assistant_master_switch_card"),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isMasterEnabled && isServiceRunning) EmeraldActive.copy(alpha = 0.5f) else CardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isMasterEnabled && isServiceRunning) EmeraldActive.copy(alpha = 0.2f) else CardSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (isMasterEnabled && isServiceRunning) EmeraldActive else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isMasterEnabled && isServiceRunning)
                                AppStrings.assistantActive(language)
                            else
                                AppStrings.assistantInactive(language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMasterEnabled && isServiceRunning) TextPrimary else TextSecondary
                        )
                        Text(
                            text = if (isMasterEnabled && isServiceRunning)
                                AppStrings.assistantActiveSub(language)
                            else
                                AppStrings.assistantInactiveSub(language),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Switch(
                    checked = isMasterEnabled && isServiceRunning,
                    onCheckedChange = { toggleAssistant(it) },
                    modifier = Modifier.testTag("master_assistant_toggle"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VoidBlack,
                        checkedTrackColor = EmeraldActive,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardSurfaceElevated
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ANIMATED VOICE ORB ---
        VoiceOrb(
            state = assistantState,
            audioLevel = audioLevel,
            onClick = {
                if (!isServiceRunning) {
                    toggleAssistant(true)
                } else {
                    val intent = Intent(context, AnkitaVoiceService::class.java).apply {
                        action = AnkitaVoiceService.ACTION_TRIGGER_WAKE
                    }
                    context.startService(intent)
                }
            },
            size = 230.dp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // --- STATUS TEXT ---
        val statusLabel = when (assistantState) {
            AssistantVisualState.SLEEPING -> AppStrings.statusSleeping(language)
            AssistantVisualState.LISTENING_WAKE -> AppStrings.statusListeningWake(language)
            AssistantVisualState.WAKE_DETECTED -> AppStrings.statusWakeDetected(language)
            AssistantVisualState.LISTENING_COMMAND -> AppStrings.statusListeningCommand(language)
            AssistantVisualState.PROCESSING -> AppStrings.statusProcessing(language)
            AssistantVisualState.SPEAKING -> AppStrings.statusSpeaking(language)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = statusLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = when (assistantState) {
                    AssistantVisualState.LISTENING_WAKE -> NeonCyan
                    AssistantVisualState.WAKE_DETECTED -> Color(0xFFFACC15)
                    AssistantVisualState.SPEAKING -> Color(0xFFF59E0B)
                    AssistantVisualState.PROCESSING -> NeonPink
                    else -> TextSecondary
                },
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TRANSCRIPT & RESPONSE DISPLAY CARD ---
        AnimatedVisibility(
            visible = lastTranscript.isNotBlank() || lastResponse.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (lastTranscript.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "🗣️ ", fontSize = 14.sp)
                            Text(
                                text = lastTranscript,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    if (lastTranscript.isNotBlank() && lastResponse.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (lastResponse.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "✨ ", fontSize = 14.sp)
                            Text(
                                text = lastResponse,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- ACTION BUTTONS (TEST & QUICK EXECUTE) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, AnkitaVoiceService::class.java).apply {
                        action = AnkitaVoiceService.ACTION_TRIGGER_WAKE
                    }
                    context.startService(intent)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("simulate_wake_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = "Simulate Wake",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = AppStrings.btnHeyAnkita(language), color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val intent = Intent(context, AnkitaVoiceService::class.java).apply {
                        action = AnkitaVoiceService.ACTION_SPEAK_TIME
                    }
                    context.startService(intent)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("speak_time_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    tint = VoidBlack,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = AppStrings.btnSpeakTime(language), color = VoidBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- QUICK COMMAND CHIPS ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = AppStrings.quickCommandsTitle(language),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val commands = if (language == "en") {
                listOf(
                    "What time is it?",
                    "What date is it?",
                    "What day is today?",
                    "Battery percentage",
                    "Set an alarm",
                    "Hello Ankita"
                )
            } else {
                listOf(
                    "এখন সময় কত?",
                    "আজ কত তারিখ?",
                    "আজ কী বার?",
                    "ব্যাটারি কত পার্সেন্ট?",
                    "আমার অ্যালার্ম সেট করো",
                    "হ্যালো অঙ্কিতা"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commands.forEach { cmd ->
                    SuggestionChip(
                        onClick = {
                            val intent = Intent(context, AnkitaVoiceService::class.java).apply {
                                action = AnkitaVoiceService.ACTION_PROCESS_TEXT
                                putExtra(AnkitaVoiceService.EXTRA_TEXT, cmd)
                            }
                            context.startService(intent)
                        },
                        label = { Text(text = cmd, fontSize = 12.sp, color = TextPrimary) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = CardSurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = CardBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}
