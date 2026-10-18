package com.example.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.model.BreastSide
import com.example.data.model.TimeFormat
import com.example.data.repository.BabyBuddyRepository

/**
 * Quick Settings tiles: Breastfeed (start / pause / resume, starting on the side set in Settings → You),
 * Sleep (start / woke up) and Diaper (opens the diaper log). Without a login they open the app.
 * [refreshAll] asks the system to redraw them when a timer changes.
 */
abstract class QuickTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            draw(tile)
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (!BabyBuddyRepository.instance.connectionConfig.value.isConnected) {
            openApp("TODAY_DASHBOARD")
            return
        }
        onTap()
        qsTile?.let { tile ->
            draw(tile)
            tile.updateTile()
        }
        WidgetManagerHelper.updateAllWidgets(this)
    }

    protected abstract fun draw(tile: Tile)

    protected abstract fun onTap()

    protected fun Tile.set(label: String, subtitle: String, active: Boolean, icon: Int) {
        this.label = label
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) this.subtitle = subtitle
        this.icon = Icon.createWithResource(this@QuickTile, icon)
        state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    protected fun signedOut(): Boolean = !BabyBuddyRepository.instance.connectionConfig.value.isConnected

    /** Opens the app on [screen] (an EXTRA_NAVIGATE_TO value) and closes the shade. */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    protected fun openApp(screen: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BabyBuddyQuickLogWidget.EXTRA_NAVIGATE_TO, screen)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, screen.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        /** Redraws the tiles that are in the user's Quick Settings (the system calls onStartListening). */
        fun refreshAll(context: Context) {
            listOf(FeedTileService::class.java, SleepTileService::class.java).forEach {
                runCatching { requestListeningState(context, ComponentName(context, it)) }
            }
        }
    }
}

class FeedTileService : QuickTile() {

    override fun draw(tile: Tile) {
        val repo = BabyBuddyRepository.instance
        val t = repo.liveTimerState.value
        when {
            signedOut() -> tile.set("Breastfeed", "Open the app", false, R.drawable.ic_widget_feed)

            !t.isRunning -> {
                val next = if (repo.nextStartSide() == BreastSide.LEFT) "Left" else "Right"
                tile.set("Breastfeed", "Start $next", false, R.drawable.ic_widget_feed)
            }

            t.isPaused -> tile.set("Feed paused", "Tap to resume", true, R.drawable.ic_widget_pause)

            else -> tile.set("Feeding", "${if (t.activeBreast == BreastSide.LEFT) "Left" else "Right"} · tap to pause", true, R.drawable.ic_widget_play)
        }
    }

    override fun onTap() {
        val repo = BabyBuddyRepository.instance
        val t = repo.liveTimerState.value
        if (!t.isRunning) {
            // Start on the side from Settings → You, like the timer widget
            repo.tapSide(repo.nextStartSide())
        } else {
            repo.togglePause()
        }
    }
}

class SleepTileService : QuickTile() {

    override fun draw(tile: Tile) {
        val sleep = BabyBuddyRepository.instance.sleepTimer.value
        when {
            signedOut() -> tile.set("Sleep", "Open the app", false, R.drawable.ic_widget_sleep)
            sleep.isRunning -> tile.set("Sleeping", "Since ${TimeFormat.clock(sleep.startMillis)} · tap when awake", true, R.drawable.ic_widget_sleep)
            else -> tile.set("Sleep", "Tap when asleep", false, R.drawable.ic_widget_sleep)
        }
    }

    override fun onTap() {
        val repo = BabyBuddyRepository.instance
        if (repo.sleepTimer.value.isRunning) {
            repo.stopSleep()
            Toast.makeText(this, "Sleep saved", Toast.LENGTH_SHORT).show()
        } else {
            repo.startSleep()
        }
    }
}

class DiaperTileService : QuickTile() {

    override fun draw(tile: Tile) = tile.set("Diaper", "Log a change", false, R.drawable.ic_widget_diaper)

    override fun onTap() = openApp("LOG_DIAPER")
}
