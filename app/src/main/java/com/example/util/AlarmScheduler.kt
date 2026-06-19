package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Alarm
import com.example.receiver.AlarmReceiver
import java.util.*

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(context, alarm)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = calculateNextTriggerTime(alarm)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_HOUR", alarm.hour)
            putExtra("ALARM_MINUTE", alarm.minute)
            putExtra("ALARM_VIBRATE", alarm.isVibrate)
            putExtra("ALARM_SOUND", alarm.soundOption)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // setAlarmClock guarantees precise trigger and registers widget alarm symbol on status bar
                val showIntent = Intent(context, com.example.MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    alarm.id + 100000,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm ${alarm.id} for ${Date(triggerTime)}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm, falling back to setAndAllowWhileIdle", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancel(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled alarm ${alarm.id}")
        }
    }

    private fun calculateNextTriggerTime(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.daysOfWeek.isEmpty()) {
            // One-time alarm
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        } else {
            // Find the closest active day of the week
            val activeDays = alarm.daysOfWeek.split(",")
                .map { it.trim().toInt() }
                .toSet() // 1 = Mon ... 7 = Sun. Note: Calendar.MONDAY = 2, Calendar.SUNDAY = 1 etc.

            // Convert active days indices (1=Mon ... 7=Sun) to Calendar Constant days (2=Mon ... 1=Sun)
            val calendarDays = activeDays.map { indexToCalendarDay(it) }.toSet()

            var daysToDiff = 0
            while (daysToDiff < 8) {
                val checkCal = (target.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, daysToDiff)
                }
                val checkDay = checkCal.get(Calendar.DAY_OF_WEEK)
                if (calendarDays.contains(checkDay)) {
                    if (daysToDiff > 0 || checkCal.after(now)) {
                        return checkCal.timeInMillis
                    }
                }
                daysToDiff++
            }
            // Fallback
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }
    }

    private fun indexToCalendarDay(index: Int): Int {
        return when (index) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }
}
