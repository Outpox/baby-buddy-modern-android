package com.example

import android.app.Application
import com.example.data.repository.BabyBuddyRepository
import com.example.widget.FeedNotification
import com.example.widget.Reminders
import com.example.widget.SleepNotification
import com.example.widget.WidgetManagerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Restores saved state on every process start (including ones triggered by a home-screen widget) and keeps widgets current. */
class BabyBuddyApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val repository = BabyBuddyRepository.instance
        repository.attach(this)
        appScope.launch {
            // Lock-screen feed controls: refresh every second while a side runs (so the L/R times count up),
            // and on start/pause/switch/end/edits and when the setting changes
            combine(
                repository.liveTimerState
                    .map {
                        listOf(
                            it.isRunning,
                            it.isPaused,
                            it.activeBreast,
                            it.feedingLocalId,
                            it.editCount,
                            if (it.isRunning && !it.isPaused) it.totalSeconds else -1
                        )
                    }
                    .distinctUntilChanged(),
                repository.feedNotificationEnabled
            ) { _, enabled -> enabled }.collect { enabled ->
                FeedNotification.update(this@BabyBuddyApplication, repository.liveTimerState.value, enabled)
            }
        }
        appScope.launch {
            // Lock-screen sleep timer (same setting as the feed controls)
            combine(repository.sleepTimer, repository.feedNotificationEnabled) { sleep, enabled -> sleep to enabled }
                .distinctUntilChanged()
                .collect { (sleep, enabled) -> SleepNotification.update(this@BabyBuddyApplication, sleep, enabled) }
        }
        appScope.launch {
            // Feed-due / awake reminders follow every feed, sleep and settings change, from any phone
            combine(
                combine(repository.reminders, repository.hiddenItems) { r, h -> r to h },
                repository.lastFeedMillis,
                repository.lastSleepEndMillis,
                repository.sleepTimer.map { it.startMillis }, // a sleep starting/ending, or its start edited
                repository.liveTimerState.map { it.isRunning }
            ) { _, _, _, _, _ -> Unit }.collectLatest {
                delay(500)
                Reminders.schedule(this@BabyBuddyApplication)
            }
        }
        appScope.launch {
            repository.widgetState.collectLatest {
                delay(300) // coalesce bursts of changes
                WidgetManagerHelper.updateAllWidgets(this@BabyBuddyApplication)
            }
        }
    }
}
