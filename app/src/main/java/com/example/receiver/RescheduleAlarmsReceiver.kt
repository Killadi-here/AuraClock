package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.AlarmDatabase
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RescheduleAlarmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "RescheduleReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Device booted. Rescheduling active alarms...")
            
            val pendingResult = goAsync()
            val database = AlarmDatabase.getDatabase(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = database.alarmDao().getAllAlarms().first()
                    for (alarm in alarms) {
                        if (alarm.isEnabled) {
                            AlarmScheduler.schedule(context, alarm)
                            Log.d(TAG, "Rescheduled alarm ${alarm.id} at ${alarm.hour}:${alarm.minute}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
