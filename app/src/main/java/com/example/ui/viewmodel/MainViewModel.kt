package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.WeatherService
import com.example.data.api.WeatherResponse
import com.example.data.database.AlarmDatabase
import com.example.data.model.Alarm
import com.example.data.model.PresetCity
import com.example.data.repository.AlarmRepository
import com.example.widgets.ClockWidgetProvider
import com.example.widgets.WidgetTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(val weather: WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val repository: AlarmRepository
    private val weatherService = WeatherService.create()

    private val _currentCity = MutableStateFlow(PresetCity.DEFAULT)
    val currentCity: StateFlow<PresetCity> = _currentCity.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _selectedTheme = MutableStateFlow(WidgetTheme.RETRO_FLIP)
    val selectedTheme: StateFlow<WidgetTheme> = _selectedTheme.asStateFlow()

    init {
        val alarmDao = AlarmDatabase.getDatabase(context).alarmDao()
        repository = AlarmRepository(alarmDao, context)
        
        // Recover cached theme state
        _selectedTheme.value = ClockWidgetProvider.getSelectedTheme(context)
        
        // Recover cached city state or default to Paris
        val prefs = context.getSharedPreferences(ClockWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val savedCityName = prefs.getString(ClockWidgetProvider.KEY_WEATHER_CITY, PresetCity.DEFAULT.name) ?: PresetCity.DEFAULT.name
        val matchingCity = PresetCity.CITIES.firstOrNull { it.name.lowercase() == savedCityName.lowercase() } ?: PresetCity.DEFAULT
        _currentCity.value = matchingCity
        
        // Trigger initial weather pull
        fetchWeatherForCity(matchingCity)
    }

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun fetchWeatherForCity(city: PresetCity) {
        _currentCity.value = city
        
        // Cache city choice globally, so widgets read it
        val prefs = context.getSharedPreferences(ClockWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ClockWidgetProvider.KEY_WEATHER_CITY, city.name).apply()

        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState.Loading
            try {
                val response = weatherService.getCurrentWeather(city.latitude, city.longitude)
                
                // Keep shared widgets in sync by caching weather stats inside shared preferences
                prefs.edit().apply {
                    putFloat(ClockWidgetProvider.KEY_WEATHER_TEMP, response.currentWeather.temperature.toFloat())
                    putInt(ClockWidgetProvider.KEY_WEATHER_CODE, response.currentWeather.weathercode)
                    apply()
                }

                _weatherUiState.value = WeatherUiState.Success(response)
                
                // Feed changes back to widgets
                ClockWidgetProvider.triggerWidgetUpdate(context)
                Log.d("MainViewModel", "Loaded weather for: ${city.name}, Temperature is: ${response.currentWeather.temperature}")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to retrieve weather metrics: ${e.message}", e)
                _weatherUiState.value = WeatherUiState.Error(e.message ?: "Unknown Network Error")
            }
        }
    }

    fun setTheme(theme: WidgetTheme) {
        _selectedTheme.value = theme
        ClockWidgetProvider.saveSelectedTheme(context, theme)
        ClockWidgetProvider.triggerWidgetUpdate(context)
    }

    // Alarm management actions
    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.insertAlarm(alarm)
            refreshWidgetAlarmStatus()
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
            refreshWidgetAlarmStatus()
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            refreshWidgetAlarmStatus()
        }
    }

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm, isEnabled)
            refreshWidgetAlarmStatus()
        }
    }

    private fun refreshWidgetAlarmStatus() {
        // Redraw widgets to pick up latest alarm additions/toggles
        ClockWidgetProvider.triggerWidgetUpdate(context)
    }
}
