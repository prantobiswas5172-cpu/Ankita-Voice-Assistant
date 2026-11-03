package com.example.ui.screen

import android.app.TimePickerDialog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AnkitaDatabase
import com.example.data.model.AlarmEntity
import com.example.engine.alarm.AlarmScheduler
import com.example.engine.nlp.BengaliDateUtils
import com.example.ui.strings.AppStrings
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AlarmsScreen(
    modifier: Modifier = Modifier,
    language: String = "bn"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AnkitaDatabase.getDatabase(context) }
    val scheduler = remember { AlarmScheduler(context) }

    val alarms by db.alarmDao().getAllAlarmsFlow().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf(AppStrings.defaultAlarmTitle(language)) }
    var selectedHour by remember { mutableIntStateOf(7) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var isBengaliVoice by remember { mutableStateOf(language != "en") }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.alarmsTitle(language),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = AppStrings.alarmsSubtitle(language),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = AppStrings.savedAlarmsBadge(language, alarms.size),
                        fontSize = 12.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (alarms.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CardSurface)
                                .border(1.dp, CardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = AppStrings.noAlarmsTitle(language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = AppStrings.noAlarmsDesc(language),
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCardItem(
                            alarm = alarm,
                            language = language,
                            onToggle = { enabled ->
                                coroutineScope.launch {
                                    if (enabled) {
                                        scheduler.setSystemAlarm(alarm.copy(isEnabled = true))
                                        db.alarmDao().setAlarmEnabled(alarm.id, true)
                                    } else {
                                        scheduler.cancelAlarm(alarm.id)
                                    }
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    scheduler.deleteAlarm(alarm)
                                }
                            }
                        )
                    }
                }
            }
        }

        // FAB to add alarm
        FloatingActionButton(
            onClick = {
                val now = Calendar.getInstance()
                selectedHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
                selectedMinute = 0
                newTitle = AppStrings.defaultAlarmTitle(language)
                isBengaliVoice = language != "en"
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_alarm_fab"),
            containerColor = NeonCyan,
            contentColor = VoidBlack,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alarm")
        }
    }

    // Add Alarm Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(text = AppStrings.addAlarmTitle(language), fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(AppStrings.alarmTitleField(language)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Time Picker Button
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val displayHour = if (selectedHour > 12) selectedHour - 12 else if (selectedHour == 0) 12 else selectedHour
                    val minFormatted = if (selectedMinute < 10) "0$selectedMinute" else "$selectedMinute"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = AppStrings.selectTimeLabel(language), color = TextSecondary, fontSize = 14.sp)

                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        selectedHour = h
                                        selectedMinute = m
                                    },
                                    selectedHour,
                                    selectedMinute,
                                    false
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceElevated),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            val timeBtnText = if (language == "en") {
                                "$displayHour:$minFormatted $amPm"
                            } else {
                                "$displayHour:$minFormatted $amPm (${BengaliDateUtils.toBengaliDigits(selectedHour)}:${BengaliDateUtils.toBengaliDigits(selectedMinute)})"
                            }
                            Text(
                                text = timeBtnText,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bengali voice toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = AppStrings.bengaliVoiceToggle(language), color = TextPrimary, fontSize = 14.sp)
                        Switch(
                            checked = isBengaliVoice,
                            onCheckedChange = { isBengaliVoice = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VoidBlack,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            scheduler.scheduleAlarm(
                                title = if (newTitle.isBlank()) AppStrings.defaultAlarmTitle(language) else newTitle,
                                hour = selectedHour,
                                minute = selectedMinute,
                                isBengali = isBengaliVoice
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(AppStrings.btnSave(language), color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(AppStrings.btnCancel(language), color = TextSecondary)
                }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun AlarmCardItem(
    alarm: AlarmEntity,
    language: String,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val displayHour = if (alarm.hour > 12) alarm.hour - 12 else if (alarm.hour == 0) 12 else alarm.hour
    val amPm = if (alarm.hour < 12) "AM" else "PM"
    val minStr = if (alarm.minute < 10) "0${alarm.minute}" else "${alarm.minute}"

    val isBn = language != "en"
    val badgeText = if (isBn) {
        val hourBn = BengaliDateUtils.toBengaliDigits(displayHour)
        val minBn = BengaliDateUtils.toBengaliDigits(alarm.minute)
        val periodBn = if (alarm.hour < 12) "সকাল" else "রাত"
        "$periodBn $hourBn:$minBn"
    } else {
        val periodEn = if (alarm.hour < 12) "Morning" else "Evening/Night"
        "$periodEn"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_item_${alarm.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (alarm.isEnabled) NeonCyan.copy(alpha = 0.4f) else CardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$displayHour:$minStr $amPm",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) TextPrimary else TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            color = if (alarm.isEnabled) NeonCyan else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alarm.title,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VoidBlack,
                        checkedTrackColor = EmeraldActive,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardSurfaceElevated
                    )
                )
            }
        }
    }
}
