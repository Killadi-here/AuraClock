package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.api.WeatherService
import com.example.data.database.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClockWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "ClockWidgetProvider"
        
        const val ACTION_SHIFT_THEME = "com.example.widgets.ACTION_SHIFT_THEME"
        const val ACTION_REFRESH_WEATHER = "com.example.widgets.ACTION_REFRESH_WEATHER"
        
        const val PREFS_NAME = "widget_prefs"
        const val KEY_THEME = "widget_theme"
        const val KEY_WEATHER_TEMP = "key_weather_temp"
        const val KEY_WEATHER_CODE = "key_weather_code"
        const val KEY_WEATHER_CITY = "key_weather_city"
        const val KEY_NEXT_ALARM = "key_next_alarm"

        fun getSelectedTheme(context: Context): WidgetTheme {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val themeId = prefs.getString(KEY_THEME, WidgetTheme.RETRO_FLIP.id) ?: WidgetTheme.RETRO_FLIP.id
            return WidgetTheme.getById(themeId)
        }

        fun saveSelectedTheme(context: Context, theme: WidgetTheme) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_THEME, theme.id).apply()
        }

        fun triggerWidgetUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ClockWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val intent = Intent(context, ClockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Run update in background scope if needed, or query Alarm DB quickly
        CoroutineScope(Dispatchers.IO).launch {
            queryAndUpdateNextAlarmCache(context)
            
            for (appWidgetId in appWidgetIds) {
                updateWidgetInstance(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive action: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                // System clock update every minute, update widget visuals
                triggerWidgetUpdate(context)
            }
            ACTION_SHIFT_THEME -> {
                val currentTheme = getSelectedTheme(context)
                val themes = WidgetTheme.values()
                val nextIndex = (themes.indexOf(currentTheme) + 1) % themes.size
                val nextTheme = themes[nextIndex]
                saveSelectedTheme(context, nextTheme)
                Log.d(TAG, "Shifted theme to ${nextTheme.displayName}")
                
                triggerWidgetUpdate(context)
                
                // Show short Toast
                val toastIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                // We'll update the screen as well if it's already active
            }
            ACTION_REFRESH_WEATHER -> {
                Log.d(TAG, "Manual weather refresh requested from widget...")
                refreshWeatherFromApi(context)
            }
        }
    }

    private fun updateWidgetInstance(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Generate gorgeous painted widget bitmap
        val width = 450
        val height = 220
        val bitmap = renderWidgetBitmap(context, width, height)
        views.setImageViewBitmap(R.id.widget_canvas, bitmap)

        // Setup PendingIntents for clickable partitions
        // 1. App Launch (Left/Center)
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_zone_app, appPendingIntent)

        // 2. Theme Cycle (Top Right)
        val themeIntent = Intent(context, ClockWidgetProvider::class.java).apply {
            action = ACTION_SHIFT_THEME
        }
        val themePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            themeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_zone_theme, themePendingIntent)

        // 3. Refresh Weather/Alarms (Bottom Right)
        val refreshIntent = Intent(context, ClockWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WEATHER
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_zone_refresh, refreshPendingIntent)

        // Perform the actual update on this widget instances
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun renderWidgetBitmap(context: Context, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val theme = getSelectedTheme(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Draw rounded container background
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.backgroundColor
            style = Paint.Style.FILL
        }
        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val cornerRadius = 32f
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)

        // If Aqua Glass theme, give it a beautiful semi-transparent glassy gradient border
        if (theme == WidgetTheme.AQUA_GLASS) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
                    Color.parseColor("#4000BFA5"), Color.parseColor("#104DD0E1"), Shader.TileMode.CLAMP)
                setShader(shader)
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, strokePaint)
        } else if (theme == WidgetTheme.NEON_CYBER) {
            // Neon pink cyber border
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.secondaryColor
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, strokePaint)
        }

        // Draw horizontal splitter for retro flip cards or styling highlights
        if (theme == WidgetTheme.RETRO_FLIP) {
            val splitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#10FFFFFF")
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, splitPaint)
        }

        // 2. Format current date & time
        val now = Date()
        val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())

        val timeString = timeFormat.format(now)
        val amPmString = amPmFormat.format(now)
        val dateString = dateFormat.format(now)

        // 3. Setup text paint tools
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 72f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val amPmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryColor
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.labelColor
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.labelColor
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        // 4. Draw Time & AM/PM
        canvas.drawText(timeString, 30f, 100f, timePaint)
        val timeWidth = timePaint.measureText(timeString)
        canvas.drawText(amPmString, 30f + timeWidth + 10f, 100f, amPmPaint)

        // 5. Draw Date
        canvas.drawText(dateString, 32f, 140f, datePaint)

        // 6. Draw Alarm info
        val nextAlarmText = prefs.getString(KEY_NEXT_ALARM, "No active alarms") ?: "No active alarms"
        val alarmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (nextAlarmText.contains("No")) theme.labelColor else theme.primaryColor
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val alarmIcon = "⏰ "
        canvas.drawText(alarmIcon + nextAlarmText, 32f, 175f, alarmPaint)

        // 7. Draw Weather on right side
        val temp = prefs.getFloat(KEY_WEATHER_TEMP, 21.5f)
        val city = prefs.getString(KEY_WEATHER_CITY, "Paris") ?: "Paris"
        val code = prefs.getInt(KEY_WEATHER_CODE, 0)
        val weatherDesc = mapWeatherCodeToDesc(code)

        val weatherTempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryColor
            textSize = 48f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val weatherCityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val weatherCodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.labelColor
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        // Draw weather elements offset to the right
        val rightPadding = 30f
        canvas.drawText(String.format(Locale.getDefault(), "%.1f°C", temp), width - rightPadding, 90f, weatherTempPaint)
        canvas.drawText(city, width - rightPadding, 125f, weatherCityPaint)
        canvas.drawText(weatherDesc, width - rightPadding, 155f, weatherCodePaint)

        // 8. Draw footer partitioning guides (app, theme shift, sync indicator)
        val footerY = height - 15f
        canvas.drawText("⚡ APP", 35f, footerY, footerPaint)
        canvas.drawText("✦ THEME", width * 0.55f, footerY, footerPaint)
        canvas.drawText("↻ SYNC", width * 0.80f, footerY, footerPaint)

        return bitmap
    }

    private fun mapWeatherCodeToDesc(code: Int): String {
        return when (code) {
            0, 1 -> "Sunny"
            2 -> "Mostly Clear"
            3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75, 77, 85, 86 -> "Snowy"
            80, 81, 82 -> "Rain Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Fine"
        }
    }

    private suspend fun queryAndUpdateNextAlarmCache(context: Context) {
        try {
            val db = AlarmDatabase.getDatabase(context)
            val alarms = db.alarmDao().getAllAlarms().first()
            val activeAlarms = alarms.filter { it.isEnabled }
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (activeAlarms.isEmpty()) {
                prefs.edit().putString(KEY_NEXT_ALARM, "No active alarms").apply()
            } else {
                // Find next alarm chronologically
                // (For simplicity we'll just show the shortest time today/tomorrow)
                val alarmTimes = activeAlarms.map { it.formattedTime + " (" + (if (it.daysOfWeek.isNotEmpty()) "Rep" else "Once") + ")" }
                prefs.edit().putString(KEY_NEXT_ALARM, activeAlarms.first().formattedTime).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating next alarm cache", e)
        }
    }

    private fun refreshWeatherFromApi(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val city = prefs.getString(KEY_WEATHER_CITY, "Paris") ?: "Paris"
        
        // Find city lat/lon
        val preset = com.example.data.model.PresetCity.CITIES.firstOrNull { it.name.lowercase() == city.lowercase() }
            ?: com.example.data.model.PresetCity.DEFAULT

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = WeatherService.create()
                val response = service.getCurrentWeather(preset.latitude, preset.longitude)
                
                prefs.edit().apply {
                    putFloat(KEY_WEATHER_TEMP, response.currentWeather.temperature.toFloat())
                    putInt(KEY_WEATHER_CODE, response.currentWeather.weathercode)
                    putString(KEY_WEATHER_CITY, preset.name)
                    apply()
                }
                Log.d(TAG, "Weather fetched successfully! Temp: ${response.currentWeather.temperature}°C, Code: ${response.currentWeather.weathercode}")
                
                // Redraw widget
                triggerWidgetUpdate(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update weather: ${e.message}", e)
            }
        }
    }
}
