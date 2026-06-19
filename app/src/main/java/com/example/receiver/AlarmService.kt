package com.example.receiver

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AlarmDatabase
import com.example.util.AlarmScheduler
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        private const val TAG = "AlarmService"
        const val CHANNEL_ID = "alarm_service_channel"
        const val NOTIFICATION_ID = 8888

        const val ACTION_START_ALARM = "ACTION_START_ALARM"
        const val ACTION_DISMISS_ALARM = "ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "ACTION_SNOOZE_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val label = intent?.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val hour = intent?.getIntExtra("ALARM_HOUR", 0) ?: 0
        val minute = intent?.getIntExtra("ALARM_MINUTE", 0) ?: 0
        val vibrate = intent?.getBooleanExtra("ALARM_VIBRATE", true) ?: true

        Log.d(TAG, "onStartCommand action: $action, alarmId: $alarmId")

        when (action) {
            ACTION_START_ALARM -> {
                startRinging(alarmId, label, hour, minute, vibrate)
            }
            ACTION_DISMISS_ALARM -> {
                dismissAlarm(alarmId)
            }
            ACTION_SNOOZE_ALARM -> {
                snoozeAlarm(alarmId, label)
            }
        }

        return START_NOT_STICKY
    }

    private fun startRinging(alarmId: Int, label: String, hour: Int, minute: Int, vibrate: Boolean) {
        // Start playing ringtone
        try {
            val alertUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ringtone player", e)
        }

        // Start vibration
        if (vibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
            }
        }

        // Build notification with interactive actions
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            this.action = ACTION_DISMISS_ALARM
            putExtra("ALARM_ID", alarmId)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            alarmId + 200,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            this.action = ACTION_SNOOZE_ALARM
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            alarmId + 300,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_ALARM_DISMISS_DIALOG", true)
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId + 400,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayTime = String.format("%02d:%02d", hour, minute)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm Ringing - $displayTime")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_menu_today, "Snooze (5m)", snoozePendingIntent)
            .setSubText("AuraClock Widget")
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun dismissAlarm(alarmId: Int) {
        Log.d(TAG, "Dismissing alarm $alarmId")
        stopSelfAndClear()

        // Disable alarm in DB if it is a non-repeating (one-time) alarm
        if (alarmId != -1) {
            val db = AlarmDatabase.getDatabase(this)
            CoroutineScope(Dispatchers.IO).launch {
                val alarm = db.alarmDao().getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.daysOfWeek.isEmpty()) {
                        db.alarmDao().updateAlarm(alarm.copy(isEnabled = false))
                    } else {
                        // Re-schedule for next period
                        AlarmScheduler.schedule(applicationContext, alarm)
                    }
                }
            }
        }
    }

    private fun snoozeAlarm(alarmId: Int, label: String) {
        Log.d(TAG, "Snoozing alarm $alarmId")
        stopSelfAndClear()

        // Schedule a snooze alarm relative to current time (5 minutes later)
        val snoozeHour: Int
        val snoozeMinute: Int
        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
            snoozeHour = get(Calendar.HOUR_OF_DAY)
            snoozeMinute = get(Calendar.MINUTE)
        }

        val tempSnoozeId = alarmId + 99999
        val snoozeAlarm = com.example.data.model.Alarm(
            id = tempSnoozeId,
            hour = snoozeHour,
            minute = snoozeMinute,
            label = "$label (Snoozed)",
            isEnabled = true,
            daysOfWeek = "" // one-time
        )

        AlarmScheduler.schedule(this, snoozeAlarm)
        
        // Show lightweight Toast or quick notice
        Handler(Looper.getMainLooper()).post {
            android.widget.Toast.makeText(this, "Snoozed for 5 minutes", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopSelfAndClear() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibrator", e)
        }

        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Ongoing Alarms Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows ringing alarms with interactive responses."
                enableVibration(true)
                setSound(null, null) // Handled manually by service media player
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
