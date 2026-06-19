package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.ClockDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.widgets.ClockWidgetProvider
import com.example.widgets.WidgetTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    // Handles runtime notification request on Android 13+ (SDK 33)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted.")
            Toast.makeText(this, "Notification permission successfully enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Log.w("MainActivity", "Notification permission refused.")
            Toast.makeText(
                this,
                "Permission needed to ring alarms and post notifications properly.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support
        enableEdgeToEdge()

        // Request permissions on Android 13+
        checkAndRequestPermissions()

        // Set up View Model
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // Capture incoming intent launches (e.g. from Alarm ringing notification clicks)
        val openAlarmDialog = intent.getBooleanExtra("OPEN_ALARM_DISMISS_DIALOG", false)
        if (openAlarmDialog) {
            val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up"
            Toast.makeText(this, "Alarm Activated: $alarmLabel", Toast.LENGTH_LONG).show()
        }

        setContent {
            // Keep app theme responsive to preferences and system settings
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClockDashboardScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleDark = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger widget updates whenever returning to the application to synchronize custom states
        ClockWidgetProvider.triggerWidgetUpdate(this)
    }
}
