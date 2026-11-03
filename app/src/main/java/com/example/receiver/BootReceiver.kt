package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.engine.alarm.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Device rebooted, rescheduling active alarms...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val scheduler = AlarmScheduler(context)
                    scheduler.rescheduleAllActiveAlarms()
                    Log.d("BootReceiver", "Active alarms rescheduled successfully")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms on boot: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
