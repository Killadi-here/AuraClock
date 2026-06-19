package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.WeatherResponse
import com.example.data.model.Alarm
import com.example.data.model.PresetCity
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.WeatherUiState
import com.example.widgets.WidgetTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ClockDashboardScreen(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    onToggleDark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val activeTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val currentCity by viewModel.currentCity.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var runningTime by remember { mutableStateOf("") }
    var runningSeconds by remember { mutableStateOf(0) }

    // Ticking coroutine for smooth realtime seconds animation
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("hh:mm", Locale.getDefault())
        while (true) {
            runningTime = sdf.format(Date())
            runningSeconds = Calendar.getInstance().get(Calendar.SECOND)
            delay(1000L)
        }
    }

    Scaffold(
        modifier = modifier.testTag("dashboard_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .testTag("add_alarm_fab")
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Alarm"
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Dashboard Header Panel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AuraClock",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Themes, Weather & Alarms synced",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = onToggleDark,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("theme_toggle_button")
                    ) {
                        Text(
                            text = if (isDarkTheme) "☀️" else "🌙",
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // 2. Interactive Widget Theme Live Preview (The Hero Card)
            item {
                Text(
                    text = "Live Theme Rendering",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("live_theme_card")
                ) {
                    // Draw visual background matching the chosen WidgetTheme
                    LiveThemePlaceholder(
                        theme = activeTheme,
                        time = runningTime,
                        seconds = runningSeconds,
                        cityName = currentCity.name,
                        weatherState = weatherState
                    )
                }
            }

            // 3. Theme Selector horizontally scrolling pill selector
            item {
                Text(
                    text = "Select Widget Theme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(WidgetTheme.values()) { theme ->
                        val isSelected = activeTheme == theme
                        val borderAlpha = if (isSelected) 1f else 0.15f
                        val bgBrush = if (isSelected) {
                            Brush.horizontalGradient(
                                colors = listOf(Color(theme.primaryColor), Color(theme.secondaryColor))
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgBrush)
                                .clickable { viewModel.setTheme(theme) }
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = theme.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 4. Weather Widget Search & City selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Weather Region",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Weather Sync Status",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Scroll of Preset Cities
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PresetCity.CITIES) { city ->
                        val isSelected = currentCity.name == city.name
                        val buttonColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        val titleColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.fetchWeatherForCity(city) },
                            color = buttonColor,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Pin Icon",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${city.name}, ${city.codeName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = titleColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weather Widget Details Panel
                WeatherDetailsPanel(weatherState = weatherState, currentCity = currentCity)
            }

            // 5. Alarms Scheduled List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Synchronized Alarms",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Active: ${alarms.count { it.isEnabled }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (alarms.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "No Alarms icon",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No Alarms set.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Tap the float action button to configure a precise wake-up clock.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmItemRow(
                        alarm = alarm,
                        onToggle = { isEnabled -> viewModel.toggleAlarm(alarm, isEnabled) },
                        onDelete = { viewModel.deleteAlarm(alarm) },
                        theme = activeTheme
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // 6. Time picker configuration Dialogue
    if (showAddDialog) {
        AlarmAddDialog(
            onDismiss = { showAddDialog = false },
            onSave = { hour, minute, label, days, vibrate, sound ->
                val newAlarm = Alarm(
                    hour = hour,
                    minute = minute,
                    label = label.ifEmpty { "Wake Up" },
                    daysOfWeek = days.joinToString(","),
                    isVibrate = vibrate,
                    soundOption = sound,
                    isEnabled = true
                )
                viewModel.addAlarm(newAlarm)
                showAddDialog = false
            }
        )
    }
}

// Custom theme placeholder layout drawing inside the Compose Sandbox
@Composable
fun LiveThemePlaceholder(
    theme: WidgetTheme,
    time: String,
    seconds: Int,
    cityName: String,
    weatherState: WeatherUiState
) {
    val displayTimeMap = time.ifEmpty { "06:10" }

    // Retrieve active visual properties based on WidgetTheme
    val bgColor = Color(theme.backgroundColor)
    val primaryColor = Color(theme.primaryColor)
    val secondaryColor = Color(theme.secondaryColor)
    val textColor = Color(theme.textColor)
    val labelColor = Color(theme.labelColor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        // Special theme visual flourishes
        when (theme) {
            WidgetTheme.RETRO_FLIP -> {
                // Large split panel divide line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
            WidgetTheme.NEON_CYBER -> {
                // Subtle linear background grid highlights
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = secondaryColor.copy(alpha = 0.1f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            WidgetTheme.MINIMAL_SWISS -> {
                // Beautiful retro Swiss analogue tick background or clean accents
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.04f),
                        radius = size.height * 0.4f,
                        center = Offset(size.width * 0.85f, size.height / 2f)
                    )
                }
            }
            WidgetTheme.COSMIC_DARK -> {
                // Cosmic star nodes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color.White.copy(alpha = 0.3f), 2f, Offset(size.width * 0.2f, size.height * 0.2f))
                    drawCircle(Color.White.copy(alpha = 0.2f), 3f, Offset(size.width * 0.7f, size.height * 0.15f))
                    drawCircle(primaryColor.copy(alpha = 0.4f), 1.5f, Offset(size.width * 0.4f, size.height * 0.8f))
                }
            }
            WidgetTheme.AQUA_GLASS -> {
                // Frost gradient glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                )
            }
        }

        // Widgets Content Elements
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Row 1: Time, seconds progress indicator, AM/PM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayTimeMap,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor,
                        style = TextStyle(
                            shadow = if (theme == WidgetTheme.NEON_CYBER) {
                                Shadow(color = secondaryColor, offset = Offset(2f, 2f), blurRadius = 8f)
                            } else null
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = String.format("%02ds", seconds),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Weather overlay
                Column(horizontalAlignment = Alignment.End) {
                    val temp = when (weatherState) {
                        is WeatherUiState.Success -> String.format(Locale.getDefault(), "%.1f°C", weatherState.weather.currentWeather.temperature)
                        is WeatherUiState.Loading -> "..."
                        else -> "21.5°C"
                    }
                    Text(
                        text = temp,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor
                    )
                    Text(
                        text = cityName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }

            // Row 2: Helper info guides
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "ALARM SYSTEM",
                        fontSize = 9.sp,
                        color = labelColor,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⏰",
                            fontSize = 12.sp,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Next: Alarm Sync Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                }

                // Interface partition labels (tapping guides)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.alpha(0.6f)
                ) {
                    Text(text = "⚡ APP", fontSize = 9.sp, color = labelColor, fontFamily = FontFamily.Monospace)
                    Text(text = "✦ THEME", fontSize = 9.sp, color = labelColor, fontFamily = FontFamily.Monospace)
                    Text(text = "↻ SYNC", fontSize = 9.sp, color = labelColor, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// Weather detailed summary panel implementation with transition states
@Composable
fun WeatherDetailsPanel(
    weatherState: WeatherUiState,
    currentCity: PresetCity
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        AnimatedContent(
            targetState = weatherState
        ) { state ->
            when (state) {
                is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is WeatherUiState.Success -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WeatherIconEmoji(code = state.weather.currentWeather.weathercode)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f°C", state.weather.currentWeather.temperature),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current Weather in ${currentCity.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Wind: ${state.weather.currentWeather.windspeed} km/h",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Code: ${state.weather.currentWeather.weathercode}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                is WeatherUiState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Connection Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Offline View: Weather Cached",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                else -> {
                    // Idle default view
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Initialize region parameter to sync details", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherIconEmoji(code: Int, modifier: Modifier = Modifier) {
    val emoji = when (code) {
        0, 1 -> "☀️" // Sunny
        2 -> "🌤️" // Mostly Clear
        3 -> "⛅" // Partly Cloudy
        45, 48 -> "🌫️" // Foggy
        51, 53, 55 -> "🌧️" // Drizzle
        61, 63, 65 -> "🌧️" // Rainy
        71, 73, 75, 77, 85, 86 -> "❄️" // Snowy
        80, 81, 82 -> "🌦️" // Rain Showers
        95, 96, 99 -> "⛈️" // Thunderstorm
        else -> "☁️"
    }
    Text(text = emoji, fontSize = 28.sp, modifier = modifier)
}

// Custom modern list row item styled inside parent theme
@Composable
fun AlarmItemRow(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    theme: WidgetTheme
) {
    var animateChecked by remember { mutableStateOf(alarm.isEnabled) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_item_${alarm.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = alarm.formattedTime,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = alarm.label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = "Repeat: ${alarm.daysText}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enabled status switch
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = {
                        animateChecked = it
                        onToggle(it)
                    },
                    modifier = Modifier.testTag("switch_${alarm.id}")
                )

                // Delete action
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                        .testTag("delete_${alarm.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete alarm button",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Styled dialogue to construct alarms perfectly
@Composable
fun AlarmAddDialog(
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, days: List<Int>, vibrate: Boolean, sound: String) -> Unit
) {
    var hour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    var label by remember { mutableStateOf("") }
    var vibrate by remember { mutableStateOf(true) }
    var selectedSound by remember { mutableStateOf("Default") }
    
    // 1=Mon ... 7=Sun
    val activeDays = remember { mutableStateListOf<Int>() }
    val daysLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Alarm Time",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_alarm_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Compose Wheel/Sliders for Time Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour choice slider
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Hour: $hour", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = hour.toFloat(),
                            onValueChange = { hour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Minute choice slider
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Min: ${String.format("%02d", minute)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = minute.toFloat(),
                            onValueChange = { minute = it.toInt() },
                            valueRange = 0f..59f,
                            steps = 58
                        )
                    }
                }

                // Custom Label Picker
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label") },
                    placeholder = { Text("e.g. Work, Workout") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_label_field")
                )

                // Days of week multi-select pills
                Column {
                    Text(text = "Days Repeat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysLabels.forEachIndexed { i, dayName ->
                            val dayIndex = i + 1
                            val isSelected = activeDays.contains(dayIndex)
                            val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val chipTc = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(chipBg)
                                    .clickable {
                                        if (isSelected) activeDays.remove(dayIndex)
                                        else activeDays.add(dayIndex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipTc
                                )
                            }
                        }
                    }
                }

                // Vibrate checkbox toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Activate Vibration", fontSize = 14.sp)
                    Checkbox(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(hour, minute, label, activeDays.toList(), vibrate, selectedSound)
                },
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
