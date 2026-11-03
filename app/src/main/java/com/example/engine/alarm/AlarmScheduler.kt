package com.example.engine.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.db.AnkitaDatabase
import com.example.data.model.AlarmEntity
import com.example.receiver.AlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val TAG = "AlarmScheduler"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val db = AnkitaDatabase.getDatabase(context)

    @SuppressLint("ScheduleExactAlarm")
    suspend fun scheduleAlarm(
        title: String,
        hour: Int,
        minute: Int,
        isBengali: Boolean = true
    ): AlarmEntity {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                // If scheduled time is in past for today, schedule for tomorrow
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarm = AlarmEntity(
            title = title,
            timeMillis = calendar.timeInMillis,
            hour = hour,
            minute = minute,
            isEnabled = true,
            isRecurring = false,
            isBengaliAnnouncement = isBengali
        )

        val id = db.alarmDao().insertAlarm(alarm)
        val savedAlarm = alarm.copy(id = id)

        setSystemAlarm(savedAlarm)
        return savedAlarm
    }

    @SuppressLint("ScheduleExactAlarm")
    fun setSystemAlarm(alarm: AlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, alarm.minute)
            putExtra(AlarmReceiver.EXTRA_IS_BENGALI, alarm.isBengaliAnnouncement)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.timeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarm.timeMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarm #${alarm.id} set for ${alarm.hour}:${alarm.minute} (timeMillis: ${alarm.timeMillis})")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission error setting exact alarm: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}", e)
        }
    }

    suspend fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        db.alarmDao().setAlarmEnabled(alarmId, false)
        Log.d(TAG, "Alarm #$alarmId cancelled")
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        cancelAlarm(alarm.id)
        db.alarmDao().deleteAlarm(alarm)
    }

    suspend fun rescheduleAllActiveAlarms() {
        val activeAlarms = db.alarmDao().getEnabledAlarms()
        for (alarm in activeAlarms) {
            // If one-shot alarm has already passed, advance or disable
            if (alarm.timeMillis < System.currentTimeMillis()) {
                if (alarm.isRecurring) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    val updated = alarm.copy(timeMillis = cal.timeInMillis)
                    db.alarmDao().updateAlarm(updated)
                    setSystemAlarm(updated)
                } else {
                    db.alarmDao().setAlarmEnabled(alarm.id, false)
                }
            } else {
                setSystemAlarm(alarm)
            }
        }
    }
}
