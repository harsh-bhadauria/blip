package com.raven.blip

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.raven.blip.ui.main.MainScreen
import com.raven.blip.ui.main.MainViewModel
import com.raven.blip.ui.overlay.OverlayService
import com.raven.blip.ui.theme.BlipTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var hasOverlayPermission by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlipTheme {
                MainScreen(
                    viewModel = viewModel,
                    hasPermission = hasOverlayPermission,
                    isRunning = isServiceRunning,
                    onRequestPermission = ::requestOverlayPermission,
                    onStartService = ::startOverlayService,
                    onStopService = ::stopOverlayService
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermission = Settings.canDrawOverlays(this)
        isServiceRunning = OverlayService.isRunning
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        isServiceRunning = true
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        isServiceRunning = false
    }
}