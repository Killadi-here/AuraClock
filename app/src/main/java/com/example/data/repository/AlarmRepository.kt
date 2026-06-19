package com.example.data.repository

import android.content.Context
import com.example.data.database.AlarmDao
import com.example.data.model.Alarm
import com.example.util.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val context: Context
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(id: Int): Alarm? = alarmDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: Alarm) {
        val id = alarmDao.insertAlarm(alarm).toInt()
        val savedAlarm = alarmDao.getAlarmById(id) ?: alarm.copy(id = id)
        
        if (savedAlarm.isEnabled) {
            AlarmScheduler.schedule(context, savedAlarm)
        } else {
            AlarmScheduler.cancel(context, savedAlarm)
        }
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
        if (alarm.isEnabled) {
            AlarmScheduler.schedule(context, alarm)
        } else {
            AlarmScheduler.cancel(context, alarm)
        }
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        AlarmScheduler.cancel(context, alarm)
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        val updated = alarm.copy(isEnabled = isEnabled)
        alarmDao.updateAlarm(updated)
        
        if (isEnabled) {
            AlarmScheduler.schedule(context, updated)
        } else {
            AlarmScheduler.cancel(context, updated)
        }
    }
}
