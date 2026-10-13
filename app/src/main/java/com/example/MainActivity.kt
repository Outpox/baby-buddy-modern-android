package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ThemeMode
import com.example.ui.HistoryFilter
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.AddWidgetModal
import com.example.ui.components.AppBottomNav
import com.example.ui.components.ToastNotificationBar
import com.example.ui.components.TopBabyHeader
import com.example.ui.screens.ConnectBackendScreen
import com.example.ui.screens.DiaperLogScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NursingTimerScreen
import com.example.ui.screens.QuickLogModal
import com.example.ui.screens.SleepScreen
import com.example.ui.screens.TodayDashboardScreen
import com.example.ui.screens.TrendsScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.BabyBuddyQuickLogWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch; after rotation the ViewModel already has the right screen
        if (savedInstanceState == null) handleWidgetNavigation(intent, mainViewModel)
        setContent {
            // Light/dark per Settings → Appearance; "by time of day" uses the dim warm palette from 20:00 to 07:00
            val themeMode by mainViewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            LaunchedEffect(themeMode, systemDark) {
                while (true) {
                    AppTheme.isNight = when (themeMode) {
                        ThemeMode.BY_TIME -> isNightHour()
                        ThemeMode.SYSTEM -> systemDark
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                    }
                    if (themeMode != ThemeMode.BY_TIME) break
                    delay(60_000L)
                }
            }
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !AppTheme.isNight
                    isAppearanceLightNavigationBars = !AppTheme.isNight
                }
            }
            MyApplicationTheme {
                BabyBuddyApp(viewModel = mainViewModel)
            }
        }
        // Pick up entries other caregivers log: on every return to the app, then every few minutes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    mainViewModel.refresh()
                    delay(3 * 60_000L)
                }
            }
        }
        // Feeds and sleeps timed on another phone show up within seconds while the app is open
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(15_000L)
                    mainViewModel.pollTimers()
                }
            }
        }
        // Lock-screen timers and reminders both need it; asked again if it was turned off later
        val reminders = mainViewModel.reminders.value
        val remindersOn = reminders.feedAfterMinutes > 0 || reminders.awakeAfterMinutes > 0 || reminders.sleepAfterMinutes > 0
        if (mainViewModel.feedNotificationEnabled.value || remindersOn) requestNotificationPermission()
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* the notification simply won't show if denied */ }

    /** The lock-screen timers and reminders need this on Android 13+. */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun isNightHour(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour >= 20 || hour < 7
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetNavigation(intent, mainViewModel)
    }

    private fun handleWidgetNavigation(intent: Intent?, vm: MainViewModel) {
        val target = intent?.getStringExtra(BabyBuddyQuickLogWidget.EXTRA_NAVIGATE_TO) ?: return
        when (target) {
            "ACTIVE_NURSING_TIMER" -> vm.navigateTo(ScreenDestination.ACTIVE_NURSING_TIMER)
            "LOG_DIAPER" -> vm.navigateTo(ScreenDestination.LOG_DIAPER)
            "QUICK_LOG_MODAL" -> vm.navigateTo(ScreenDestination.QUICK_LOG_MODAL)
            "CONNECT_SERVER" -> vm.navigateTo(ScreenDestination.CONNECT_SERVER)
            "TODAY_DASHBOARD" -> vm.navigateTo(ScreenDestination.TODAY_DASHBOARD)
            "SLEEP" -> vm.navigateTo(ScreenDestination.SLEEP)
        }
    }
}

@Composable
fun BabyBuddyApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val babyProfile by viewModel.babyProfile.collectAsState()
    val connectionConfig by viewModel.connectionConfig.collectAsState()
    val liveTimerState by viewModel.liveTimerState.collectAsState()
    val recentFeedings by viewModel.feedings.collectAsState()
    val recentDiapers by viewModel.diapers.collectAsState()
    val recentTummyTimes by viewModel.tummyTimes.collectAsState()
    val recentPumpings by viewModel.pumpings.collectAsState()
    val recentSleeps by viewModel.sleeps.collectAsState()
    val sleepTimer by viewModel.sleepTimer.collectAsState()
    val notification by viewModel.notification.collectAsState()
    val showAddWidgetModal by viewModel.showAddWidgetModal.collectAsState()
    val unsentCount by viewModel.pendingWriteCount.collectAsState()
    val trendsDays by viewModel.trendsDays.collectAsState()
    val trendsLoading by viewModel.trendsLoading.collectAsState()
    val trendsError by viewModel.trendsError.collectAsState()
    val trendsRange by viewModel.trendsRange.collectAsState()
    val historyDays by viewModel.historyDays.collectAsState()
    val historyLoading by viewModel.historyLoading.collectAsState()
    val historyError by viewModel.historyError.collectAsState()
    val historyIsCurrentWeek by viewModel.historyIsCurrentWeek.collectAsState()
    val historyFilter by viewModel.historyFilter.collectAsState()
    val lastBreastfeed by viewModel.lastBreastfeed.collectAsState()
    val quickActions by viewModel.quickActions.collectAsState()

    // Widgets state
    val dashboardWidgets by viewModel.dashboardWidgets.collectAsState()
    val tummyTimeGoal by viewModel.tummyTimeGoal.collectAsState()
    val temperatures by viewModel.temperatures.collectAsState()
    val growth by viewModel.growth.collectAsState()
    val babySex by viewModel.babySex.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val feedIntervals by viewModel.feedIntervals.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val caregiverName by viewModel.caregiverName.collectAsState()
    val feedNotificationEnabled by viewModel.feedNotificationEnabled.collectAsState()
    val use24h by viewModel.use24h.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val noteTags by viewModel.noteTags.collectAsState()
    val milkStash by viewModel.milkStash.collectAsState()
    val lastFeedMillis by viewModel.lastFeedMillis.collectAsState()
    val lastSleepEndMillis by viewModel.lastSleepEndMillis.collectAsState()
    val lastBathMillis by viewModel.lastBathMillis.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val hiddenItems by viewModel.hiddenItems.collectAsState()

    // Nothing to show without a server: until logged in (or a saved login is restored), only the login screen
    val needsLogin = !connectionConfig.isConnected
    val screen = if (needsLogin) ScreenDestination.CONNECT_SERVER else currentScreen

    // Handle back button on sub-screens
    if (!needsLogin && currentScreen != ScreenDestination.TODAY_DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD)
        }
    }

    val isPrimaryTab = currentScreen in listOf(
        ScreenDestination.TODAY_DASHBOARD,
        ScreenDestination.TRENDS,
        ScreenDestination.HISTORY,
        ScreenDestination.CONNECT_SERVER
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (!needsLogin && isPrimaryTab && currentScreen != ScreenDestination.CONNECT_SERVER) {
                TopBabyHeader(
                    profile = babyProfile,
                    connectionConfig = connectionConfig,
                    onProfileClick = { viewModel.navigateTo(ScreenDestination.CONNECT_SERVER) },
                    onSettingsClick = { viewModel.navigateTo(ScreenDestination.CONNECT_SERVER) },
                    unsentCount = unsentCount
                )
            }
        },
        bottomBar = {
            if (!needsLogin) {
                AppBottomNav(
                    currentScreen = currentScreen,
                    connectionConfig = connectionConfig,
                    onNavigate = { destination ->
                        // The History tab always opens on everything; Today's tiles open it filtered
                        if (destination == ScreenDestination.HISTORY) {
                            viewModel.openHistory(HistoryFilter.ALL)
                        } else {
                            viewModel.navigateTo(destination)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (screen) {
                ScreenDestination.TODAY_DASHBOARD -> {
                    TodayDashboardScreen(
                        liveTimerState = liveTimerState,
                        connectionConfig = connectionConfig,
                        recentFeedings = recentFeedings,
                        recentDiapers = recentDiapers,
                        recentTummyTimes = recentTummyTimes,
                        recentPumpings = recentPumpings,
                        dashboardWidgets = dashboardWidgets,
                        tummyTimeGoal = tummyTimeGoal,
                        temperatures = temperatures,
                        growth = growth,
                        birthDate = babyProfile.birthDate,
                        babySex = babySex,
                        onLogGrowth = { values, date, notes -> viewModel.logGrowth(values, date, notes) },
                        notes = notes,
                        feedIntervals = feedIntervals,
                        lastBreastfeed = lastBreastfeed,
                        quickActions = quickActions,
                        onQuickActionsChange = { viewModel.setQuickActions(it) },
                        onLogPumping = { ml, minutes, notes, start -> viewModel.logPumping(ml, minutes, notes, start) },
                        sleepTimer = sleepTimer,
                        recentSleeps = recentSleeps,
                        milkStash = milkStash,
                        onSetMilkStash = { viewModel.setMilkStash(it) },
                        noteTags = noteTags,
                        onAddTaggedNote = { text, tags -> viewModel.addNote(text, tags) },
                        lastFeedMillis = lastFeedMillis,
                        lastSleepEndMillis = lastSleepEndMillis,
                        reminders = reminders,
                        hiddenItems = hiddenItems,
                        lastBathMillis = lastBathMillis,
                        onLogBath = { notes, time -> viewModel.logBath(time, notes) },
                        onOpenHistory = { filter -> viewModel.openHistory(filter) },
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        onNavigate = { dest -> viewModel.navigateTo(dest) },
                        onOpenAddWidgetModal = { viewModel.setAddWidgetModalVisible(true) },
                        onAddTummyTimeMinutes = { min -> viewModel.addTummyTimeMinutes(min) },
                        onLogTemperature = { value, notes -> viewModel.logTemperature(value, notes) },
                        onAddNote = { text -> viewModel.addNote(text) },
                        onRemoveDashboardWidget = { type -> viewModel.removeDashboardWidget(type) },
                        onLogBottle = { ml, formula, notes -> viewModel.logBottle(ml, formula, notes) },
                        onTapSide = { side -> viewModel.tapSide(side) },
                        onEndFeed = { viewModel.endFeed() }
                    )
                }

                ScreenDestination.TRENDS -> {
                    TrendsScreen(
                        days = trendsDays,
                        isLoading = trendsLoading,
                        error = trendsError,
                        rangeDays = trendsRange,
                        onSelectRange = { days -> viewModel.loadTrends(days) },
                        onRetry = { viewModel.loadTrends() },
                        onConnect = { viewModel.navigateTo(ScreenDestination.CONNECT_SERVER) },
                        hiddenItems = hiddenItems,
                        growth = growth,
                        birthDate = babyProfile.birthDate,
                        babySex = babySex,
                        onBabySexChange = { viewModel.setBabySex(it) }
                    )
                }

                ScreenDestination.ACTIVE_NURSING_TIMER -> {
                    NursingTimerScreen(
                        liveTimerState = liveTimerState,
                        lastBreastfeed = lastBreastfeed,
                        onSetSideSeconds = { side, sec -> viewModel.setSideSeconds(side, sec) },
                        onSetFeedStart = { viewModel.setFeedStart(it) },
                        recentFeedings = recentFeedings,
                        onBack = { viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD) },
                        onTapSide = { side -> viewModel.tapSide(side) },
                        onEndFeed = { viewModel.endFeed() }
                    )
                }

                ScreenDestination.LOG_DIAPER -> {
                    DiaperLogScreen(
                        babyProfile = babyProfile,
                        recentDiapers = recentDiapers,
                        onBack = { viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD) },
                        onSaveDiaper = { type, amt, cons, col, rash, barriers, time, notes, startNursing ->
                            viewModel.saveDiaperChange(
                                type = type,
                                amount = amt,
                                consistency = cons,
                                color = col,
                                rashPresent = rash,
                                skinBarriers = barriers,
                                timeMillis = time,
                                notes = notes,
                                startNursingAfter = startNursing
                            )
                        }
                    )
                }

                ScreenDestination.QUICK_LOG_MODAL -> {
                    QuickLogModal(
                        liveTimerState = liveTimerState,
                        lastBreastfeed = lastBreastfeed,
                        onSetSideSeconds = { side, sec -> viewModel.setSideSeconds(side, sec) },
                        onSetFeedStart = { viewModel.setFeedStart(it) },
                        onClose = { viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD) },
                        onSaveAndSync = {
                            viewModel.finishAndSaveNursing()
                        },
                        onSaveDraft = {
                            viewModel.showToast("Activity saved to local offline queue")
                            viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD)
                        },
                        onQuickAction = { action ->
                            viewModel.quickLog(action)
                        },
                        onSwitchSide = { viewModel.switchSide() },
                        onTogglePause = { viewModel.togglePauseTimer() },
                        onLogBottle = { ml, formula, notes, time ->
                            viewModel.logBottle(ml, formula, notes, time)
                            viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD)
                        },
                        onTapSide = { side -> viewModel.tapSide(side) },
                        onEndFeed = { viewModel.endFeed() },
                        onLogPumping = { ml, minutes, notes, start ->
                            viewModel.logPumping(ml, minutes, notes, start)
                            viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD)
                        },
                        lastPumpMl = recentPumpings.firstOrNull()?.amountMl ?: 120,
                        lastPumpMinutes = recentPumpings.firstOrNull()?.durationMinutes?.takeIf { it > 0 } ?: 20,
                        onLogDiaper = { type, notes, time ->
                            viewModel.saveDiaperChange(
                                type = type,
                                amount = "",
                                consistency = "",
                                color = "",
                                rashPresent = false,
                                skinBarriers = emptyList(),
                                timeMillis = time,
                                notes = notes
                            )
                        },
                        liveFeedNotes = viewModel.liveFeedNotes(),
                        onLiveFeedNotesChange = { viewModel.setLiveFeedNotes(it) },
                        hiddenItems = hiddenItems,
                        onLogBath = { notes, time ->
                            viewModel.logBath(time, notes)
                            viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD)
                        },
                        sleepTimer = sleepTimer,
                        onStartSleep = { viewModel.startSleep() },
                        onSetSleepStart = { viewModel.setSleepStart(it) },
                        onSleepNotesChange = { viewModel.setSleepNotes(it) },
                        onStopSleep = { isNap -> viewModel.stopSleep(isNap) },
                        onCancelSleep = { viewModel.cancelSleep() },
                        onLogPastSleep = { start, end, isNap, notes ->
                            viewModel.logSleep(start, end, isNap, notes)
                            viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD)
                        },
                        growth = growth,
                        onLogGrowth = { values, date, notes ->
                            viewModel.logGrowth(values, date, notes)
                            viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD)
                        }
                    )
                }

                ScreenDestination.HISTORY -> {
                    HistoryScreen(
                        days = historyDays,
                        isLoading = historyLoading,
                        error = historyError,
                        isCurrentWeek = historyIsCurrentWeek,
                        onPreviousWeek = { viewModel.shiftHistoryWeek(-1) },
                        onNextWeek = { viewModel.shiftHistoryWeek(1) },
                        onRetry = { viewModel.loadHistory() },
                        filter = historyFilter,
                        onFilterChange = { viewModel.setHistoryFilter(it) },
                        onUpdateFeeding = { viewModel.updateFeeding(it) },
                        onDeleteFeeding = { viewModel.deleteFeeding(it) },
                        onUpdateDiaper = { viewModel.updateDiaper(it) },
                        onDeleteDiaper = { viewModel.deleteDiaper(it) },
                        onUpdateTummyTime = { viewModel.updateTummyTime(it) },
                        onDeleteTummyTime = { viewModel.deleteTummyTime(it) },
                        onUpdatePumping = { viewModel.updatePumping(it) },
                        onDeletePumping = { viewModel.deletePumping(it) },
                        onUpdateSleep = { viewModel.updateSleep(it) },
                        onDeleteSleep = { viewModel.deleteSleep(it) },
                        onUpdateNote = { viewModel.updateNote(it) },
                        onDeleteNote = { viewModel.deleteNoteEntry(it) },
                        hiddenItems = hiddenItems
                    )
                }

                ScreenDestination.SLEEP -> {
                    SleepScreen(
                        timer = sleepTimer,
                        todaysSleeps = recentSleeps,
                        onBack = { viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD) },
                        onStart = { viewModel.startSleep() },
                        onSetStart = { viewModel.setSleepStart(it) },
                        onNotesChange = { viewModel.setSleepNotes(it) },
                        onStop = { isNap -> viewModel.stopSleep(isNap) },
                        onCancel = { viewModel.cancelSleep() },
                        onLogPast = { start, end, isNap, notes -> viewModel.logSleep(start, end, isNap, notes) },
                        onUpdate = { viewModel.updateSleep(it) },
                        onDelete = { viewModel.deleteSleep(it) }
                    )
                }

                ScreenDestination.CONNECT_SERVER -> {
                    ConnectBackendScreen(
                        config = connectionConfig,
                        onBack = { viewModel.navigateTo(ScreenDestination.TODAY_DASHBOARD) },
                        onTestConnect = { url, token, allowSelfSigned, cfId, cfSecret ->
                            viewModel.testBackendConnection(url, token, allowSelfSigned, cfId, cfSecret)
                        },
                        onShowToast = { msg -> viewModel.showToast(msg) },
                        onDisconnect = { viewModel.disconnectServer() },
                        onEnterDemoMode = { viewModel.enterDemoMode() },
                        caregiverName = caregiverName,
                        onCaregiverNameChange = { viewModel.setCaregiverName(it) },
                        babySex = babySex,
                        onBabySexChange = { viewModel.setBabySex(it) },
                        feedNotificationEnabled = feedNotificationEnabled,
                        use24h = use24h,
                        themeMode = themeMode,
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        hiddenItems = hiddenItems,
                        onItemTrackedChange = { item, on -> viewModel.setItemTracked(item, on) },
                        reminders = reminders,
                        onRemindersChange = { settings ->
                            viewModel.setReminders(settings)
                            if (settings.feedAfterMinutes > 0 || settings.awakeAfterMinutes > 0 || settings.sleepAfterMinutes > 0) {
                                (context as? MainActivity)?.requestNotificationPermission()
                            }
                        },
                        onUse24hChange = { viewModel.setUse24h(it) },
                        onFeedNotificationChange = { enabled ->
                            viewModel.setFeedNotificationEnabled(enabled)
                            if (enabled) (context as? MainActivity)?.requestNotificationPermission()
                        }
                    )
                }
            }

            // Floating Toast notification
            AnimatedVisibility(
                visible = notification != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                notification?.let { notif ->
                    ToastNotificationBar(
                        message = notif.message,
                        isSuccess = notif.isSuccess,
                        onDismiss = { viewModel.dismissToast() },
                        onUndo = notif.undo?.let { { viewModel.runUndo() } }
                    )
                }
            }
        }
    }

    // Add Widgets Bottom Sheet Modal
    if (showAddWidgetModal) {
        AddWidgetModal(
            dashboardWidgets = dashboardWidgets,
            onToggleWidget = { type -> viewModel.toggleDashboardWidget(type) },
            onPinHomeScreenWidget = { type -> viewModel.pinHomeScreenWidget(context, type) },
            onDismiss = { viewModel.setAddWidgetModalVisible(false) }
        )
    }
}
