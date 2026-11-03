package com.example.engine.nlp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.example.engine.alarm.AlarmScheduler
import java.util.Calendar
import java.util.regex.Pattern

data class CommandResponse(
    val userSpoken: String,
    val spokenResponse: String,
    val displayText: String,
    val isBengali: Boolean,
    val actionType: ActionType
)

enum class ActionType {
    TIME,
    DATE,
    DAY,
    BATTERY,
    ALARM,
    STOP,
    GREETING,
    AI_CONVERSATION,
    UNKNOWN
}

class VoiceCommandProcessor(private val context: Context) {

    private val TAG = "VoiceCommandProcessor"
    private val alarmScheduler = AlarmScheduler(context)

    suspend fun processCommand(
        query: String,
        preferredLanguage: String = "auto"
    ): CommandResponse {
        val clean = query.trim().lowercase()
        Log.d(TAG, "Processing voice command: '$clean'")

        val isBengaliDetected = when (preferredLanguage) {
            "en" -> false
            "bn" -> true
            else -> isBengaliScript(clean)
        }
        val now = Calendar.getInstance()

        // 1. STOP / SILENCE COMMANDS
        if (matchesAny(clean, listOf("চুপ করো", "থামো", "থেমে যাও", "কথা বন্ধ করো", "কথা বলা বন্ধ", "stop", "silence", "quiet", "shut up", "cancel"))) {
            val replyBn = "আচ্ছা, কথা বলা বন্ধ করছি।"
            val replyEn = "Stopping speech."
            return CommandResponse(
                userSpoken = query,
                spokenResponse = if (isBengaliDetected) replyBn else replyEn,
                displayText = if (isBengaliDetected) replyBn else replyEn,
                isBengali = isBengaliDetected,
                actionType = ActionType.STOP
            )
        }

        // 2. TIME COMMANDS
        if (matchesAny(clean, listOf(
                "সময় কত", "কয়টা বাজে", "সময় বলো", "টাইম কত", "বর্তমান সময়", "এখন কয়টা",
                "what time", "what's the time", "tell me the time", "current time", "the time"
            ))) {
            val speech = if (isBengaliDetected) {
                BengaliDateUtils.getSpokenBengaliTime(now)
            } else {
                BengaliDateUtils.getSpokenEnglishTime(now)
            }
            return CommandResponse(
                userSpoken = query,
                spokenResponse = speech,
                displayText = speech,
                isBengali = isBengaliDetected,
                actionType = ActionType.TIME
            )
        }

        // 3. DATE COMMANDS
        if (matchesAny(clean, listOf(
                "কত তারিখ", "আজকের তারিখ", "তারিখ বলো", "তারিখ কত", "আজ কত তারিখ",
                "what date", "today's date", "what is the date", "what is today's date", "current date"
            ))) {
            val speech = if (isBengaliDetected) {
                BengaliDateUtils.getSpokenBengaliDate(now)
            } else {
                BengaliDateUtils.getSpokenEnglishDate(now)
            }
            return CommandResponse(
                userSpoken = query,
                spokenResponse = speech,
                displayText = speech,
                isBengali = isBengaliDetected,
                actionType = ActionType.DATE
            )
        }

        // 4. DAY OF WEEK COMMANDS
        if (matchesAny(clean, listOf(
                "কী বার", "কি বার", "আজকের বার", "আজ কি বার", "আজ কী বার",
                "what day", "what day is today", "which day", "day today"
            ))) {
            val speech = if (isBengaliDetected) {
                BengaliDateUtils.getSpokenBengaliDay(now)
            } else {
                BengaliDateUtils.getSpokenEnglishDay(now)
            }
            return CommandResponse(
                userSpoken = query,
                spokenResponse = speech,
                displayText = speech,
                isBengali = isBengaliDetected,
                actionType = ActionType.DAY
            )
        }

        // 5. BATTERY COMMANDS
        if (matchesAny(clean, listOf(
                "ব্যাটারি", "ব্যাটারি কত", "চার্জ কত", "ব্যাটারি লেভেল", "কত পার্সেন্ট চার্জ",
                "battery", "battery percentage", "battery level", "how much battery", "charge level"
            ))) {
            val (pct, isCharging) = getBatteryStatus()
            val speech = if (isBengaliDetected) {
                val pctBn = BengaliDateUtils.toBengaliDigits(pct)
                val chargeStatusBn = if (isCharging) " এবং ফোনটি চার্জ হচ্ছে।" else "।"
                "আপনার ফোনের ব্যাটারি চার্জ আছে $pctBn শতাংশ$chargeStatusBn"
            } else {
                val chargeStatusEn = if (isCharging) " and currently charging." else "."
                "Your battery level is $pct percent$chargeStatusEn"
            }
            return CommandResponse(
                userSpoken = query,
                spokenResponse = speech,
                displayText = speech,
                isBengali = isBengaliDetected,
                actionType = ActionType.BATTERY
            )
        }

        // 6. ALARM COMMANDS
        if (matchesAny(clean, listOf(
                "অ্যালার্ম", "অ্যালার্ম দাও", "অ্যালার্ম সেট করো", "অ্যালার্ম বাজাও", "ঘুম থেকে উঠাও",
                "alarm", "set an alarm", "set alarm", "wake me up", "reminder"
            ))) {
            val (hour, minute) = parseTimeFromCommand(clean)
            val title = if (isBengaliDetected) "ভয়েস অ্যালার্ম" else "Voice Alarm"
            val alarm = alarmScheduler.scheduleAlarm(title, hour, minute, isBengali = isBengaliDetected)

            val speech = if (isBengaliDetected) {
                val hourBn = BengaliDateUtils.toBengaliDigits(if (hour > 12) hour - 12 else if (hour == 0) 12 else hour)
                val minBn = BengaliDateUtils.toBengaliDigits(minute)
                val periodBn = if (hour < 12) "সকাল" else "দুপুর/রাত"
                if (minute == 0) {
                    "ঠিক আছে, $periodBn $hourBn টায় অ্যালার্ম সেট করে দিয়েছি।"
                } else {
                    "ঠিক আছে, $periodBn $hourBn টা বেজে $minBn মিনিটে অ্যালার্ম সেট করা হয়েছে।"
                }
            } else {
                val amPm = if (hour < 12) "AM" else "PM"
                val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
                val minStr = if (minute < 10) "0$minute" else "$minute"
                "Alarm has been set for $displayHour:$minStr $amPm."
            }
            return CommandResponse(
                userSpoken = query,
                spokenResponse = speech,
                displayText = speech,
                isBengali = isBengaliDetected,
                actionType = ActionType.ALARM
            )
        }

        // 7. GREETINGS & INTRODUCTIONS
        if (matchesAny(clean, listOf(
                "হ্যালো অঙ্কিতা", "হ্যালো", "কেমন আছো", "তোমার নাম কি", "অঙ্কিতা কে", "নমস্কার", "সালাম",
                "hello ankita", "hey ankita", "hi ankita", "hello", "hi", "how are you", "who are you", "what is your name"
            ))) {
            val speech = if (isBengaliDetected) {
                "নমস্কার! আমি অঙ্কিতা, আপনার ব্যক্তিগত ভয়েস অ্যাসিস্ট্যান্ট। আমি সম্পূর্ণ অফলাইনে সময়, তারিখ, অ্যালার্ম এবং ব্যাটারি তথ্য জানাতে পারি। বলুন, কীভাবে সাহায্য করতে পারি?"
            } else {
                "Hello! I am Ankita, your personal voice assistant. I can help you with time, date, day, alarms, and battery status offline. How can I help you today?"
            }
            return CommandResponse(
                userSpoken = query,
                spokenResponse = speech,
                displayText = speech,
                isBengali = isBengaliDetected,
                actionType = ActionType.GREETING
            )
        }

        // 8. CONVERSATIONAL / AI / GENERAL KNOWLEDGE FALLBACK
        val fallback = if (isBengaliDetected) {
            when {
                clean.contains("কেমন আছো") -> "আমি খুব ভালো আছি! আপনার দিনটি সুন্দর কাটুক।"
                clean.contains("ধন্যবাদ") || clean.contains("থ্যাঙ্কস") -> "আপনাকে অনেক ধন্যবাদ! পাশে আছি সবসময়।"
                clean.contains("গান") || clean.contains("কবিতা") -> "আমি আপনার ব্যক্তিগত সহকারী অঙ্কিতা। আমার কাছে সময়, অ্যালার্ম বা ব্যাটারি তথ্য যেকোনো সময় জানতে পারেন।"
                else -> "আমি আপনার অনুরোধ বুঝতে পেরেছি। 'এখন সময় কত', 'আজ কত তারিখ', 'ব্যাটারি কত' অথবা 'অ্যালার্ম দাও' বলে দেখতে পারেন।"
            }
        } else {
            when {
                clean.contains("how are you") -> "I am doing wonderful, thank you! How are you doing today?"
                clean.contains("thank") -> "You're very welcome! Always here to assist you."
                else -> "I heard: \"$query\". You can ask me for the current time, today's date, battery level, or to set an alarm!"
            }
        }

        return CommandResponse(
            userSpoken = query,
            spokenResponse = fallback,
            displayText = fallback,
            isBengali = isBengaliDetected,
            actionType = ActionType.AI_CONVERSATION
        )
    }

    private fun isBengaliScript(text: String): Boolean {
        for (char in text) {
            val block = Character.UnicodeBlock.of(char)
            if (block == Character.UnicodeBlock.BENGALI) {
                return true
            }
        }
        return false
    }

    private fun matchesAny(text: String, keywords: List<String>): Boolean {
        for (kw in keywords) {
            if (text.contains(kw, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun getBatteryStatus(): Pair<Int, Boolean> {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 50
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            Pair(pct, isCharging)
        } catch (e: Exception) {
            Pair(85, false)
        }
    }

    private fun parseTimeFromCommand(text: String): Pair<Int, Int> {
        val now = Calendar.getInstance()
        var hour = 7
        var minute = 0

        // Parse English formats like "7:30", "7 pm", "8 am"
        val regexColon = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(text)
        if (regexColon.find()) {
            val h = regexColon.group(1)?.toIntOrNull() ?: 7
            val m = regexColon.group(2)?.toIntOrNull() ?: 0
            val isPm = text.contains("pm", ignoreCase = true) || text.contains("রাত", ignoreCase = true) || text.contains("বিকেল", ignoreCase = true)
            hour = if (isPm && h < 12) h + 12 else h
            minute = m
            return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        }

        // Parse Bengali digits / words
        val digitMap = mapOf(
            "১" to 1, "২" to 2, "৩" to 3, "৪" to 4, "৫" to 5,
            "৬" to 6, "৭" to 7, "৮" to 8, "৯" to 9, "১০" to 10,
            "১১" to 11, "১২" to 12, "এক" to 1, "দুই" to 2, "তিন" to 3,
            "চার" to 4, "পাঁচ" to 5, "ছয়" to 6, "সাত" to 7, "আট" to 8,
            "নয়" to 9, "দশ" to 10, "এগারো" to 11, "বারো" to 12
        )

        for ((key, value) in digitMap) {
            if (text.contains(key)) {
                hour = value
                break
            }
        }

        // Check for minutes like "৩০" or "30"
        if (text.contains("৩০") || text.contains("30") || text.contains("সাড়ে")) {
            minute = 30
        } else if (text.contains("১৫") || text.contains("15") || text.contains("সোয়া")) {
            minute = 15
        } else if (text.contains("৪৫") || text.contains("45") || text.contains("পৌনে")) {
            minute = 45
        }

        val isNight = text.contains("রাত") || text.contains("সন্ধ্যা") || text.contains("pm")
        if (isNight && hour < 12) {
            hour += 12
        }

        // If no time was specified, default to 15 minutes from now for quick voice testing
        if (hour == 7 && minute == 0 && !text.contains("৭") && !text.contains("7") && !text.contains("সাত")) {
            now.add(Calendar.MINUTE, 10)
            hour = now.get(Calendar.HOUR_OF_DAY)
            minute = now.get(Calendar.MINUTE)
        }

        return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }
}
