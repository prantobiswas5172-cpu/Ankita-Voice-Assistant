package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class AnkitaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground Service Channel (Low importance for non-intrusive persistent listening indicator)
            val serviceChannel = NotificationChannel(
                CHANNEL_VOICE_SERVICE,
                "Ankita Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while Ankita is actively listening for 'Hey Ankita'"
                setShowBadge(false)
            }

            // Alarms & Reminders Channel (High importance with sound and heads-up alert)
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                "Ankita Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when scheduled alarms or voice reminders fire"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    companion object {
        lateinit var instance: AnkitaApplication
            private set

        const val CHANNEL_VOICE_SERVICE = "ankita_voice_service_channel"
        const val CHANNEL_ALARMS = "ankita_alarms_channel"
        const val SERVICE_NOTIFICATION_ID = 1001
        const val ALARM_NOTIFICATION_ID = 2002
    }
}
