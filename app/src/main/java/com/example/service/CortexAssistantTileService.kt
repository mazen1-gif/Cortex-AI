package com.example.service

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity

/**
 * Android Quick Settings Tile (زر الستارة العلوية لنظام أندرويد)
 * Allows the user to toggle the Floating Assistant Orb or trigger listening
 * directly from the system status bar anywhere in the phone.
 */
@RequiresApi(Build.VERSION_CODES.N)
class CortexAssistantTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(intent)
            return
        }

        if (FloatingOrbService.isServiceActive) {
            // Trigger speech listening immediately
            val listenIntent = Intent(context, FloatingOrbService::class.java).apply {
                action = FloatingOrbService.ACTION_START_LISTENING
            }
            startService(listenIntent)
        } else {
            // Launch the floating orb directly over whatever the user is viewing
            FloatingOrbService.start(context)
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        if (FloatingOrbService.isServiceActive) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "المساعد نشط"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "المساعد الصوتي"
        }
        tile.updateTile()
    }
}
