package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AnkitaApplication
import com.example.MainActivity
import com.example.R
import com.example.data.db.AnkitaDatabase
import com.example.engine.alarm.AlarmScheduler
import com.example.engine.nlp.BengaliDateUtils
import com.example.engine.speech.SpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_TITLE = "extra_alarm_title"
        const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
        const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
        const val EXTRA_IS_BENGALI = "extra_is_bengali"
        const val ACTION_DISMISS = "com.example.ACTION_DISMISS_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)

        if (action == ACTION_DISMISS) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(AnkitaApplication.ALARM_NOTIFICATION_ID + alarmId.toInt())
            return
        }

        val title = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "Alarms"
        val hour = intent.getIntExtra(EXTRA_ALARM_HOUR, 7)
        val minute = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)
        val isBengali = intent.getBooleanExtra(EXTRA_IS_BENGALI, true)

        Log.i("AlarmReceiver", "Alarm triggered: '$title' at $hour:$minute (Bengali=$isBengali)")

        // 1. Acquire temporary wake lock to ensure device is fully awake
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ankita:alarm_receiver"
        )
        wakeLock.acquire(15000)

        // 2. Vibrate
        vibrateDevice(context)

        // 3. Play alarm tone
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alertUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.w("AlarmReceiver", "Failed to play ringtone: ${e.message}")
        }

        // 4. Construct spoken announcement
        val speechText = if (isBengali) {
            val timeBengali = BengaliDateUtils.getSpokenBengaliTime(Calendar.getInstance())
            "অঙ্কিতা অ্যালার্ম সতর্কতা! $timeBengali $title এর সময় হয়েছে।"
        } else {
            val timeEn = BengaliDateUtils.getSpokenEnglishTime(Calendar.getInstance())
            "Ankita Alarm alert! $timeEn Time for $title."
        }

        // 5. Speak via SpeechManager
        val speechManager = SpeechManager(context)
        speechManager.speak(speechText, preferBengali = isBengali)

        // 6. Show high-priority notification with Dismiss button
        showAlarmNotification(context, alarmId, title, speechText)

        // 7. Update Room database state if one-shot
        CoroutineScope(Dispatchers.IO).launch {
            val db = AnkitaDatabase.getDatabase(context)
            val alarm = db.alarmDao().getAlarmById(alarmId)
            if (alarm != null && !alarm.isRecurring) {
                db.alarmDao().setAlarmEnabled(alarmId, false)
            } else if (alarm != null && alarm.isRecurring) {
                // Reschedule for next day
                val scheduler = AlarmScheduler(context)
                scheduler.setSystemAlarm(alarm.copy(timeMillis = alarm.timeMillis + 86400000L))
            }
        }
    }

    private fun vibrateDevice(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 200, 500), -1)
            }
        } catch (e: Exception) {
            Log.w("AlarmReceiver", "Failed to vibrate: ${e.message}")
        }
    }

    private fun showAlarmNotification(context: Context, alarmId: Long, title: String, text: String) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            (alarmId * 10).toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 10 + 1).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AnkitaApplication.CHANNEL_ALARMS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("অঙ্কিতা অ্যালার্ম: $title")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(AnkitaApplication.ALARM_NOTIFICATION_ID + alarmId.toInt(), notification)
    }
}
