package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.BabySex
import com.example.data.model.ReminderSettings
import com.example.data.model.ServerConnectionConfig
import com.example.data.model.ThemeMode
import com.example.data.model.TrackedItem
import com.example.data.model.formatMinutes
import com.example.ui.components.DurationField
import com.example.ui.components.SegmentedToggle
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.SecondaryColor
import com.example.ui.theme.SurfaceContainerLow
import kotlinx.coroutines.delay

/**
 * A settings category. In the list it is a row (icon, title, summary, chevron); once opened it fills
 * the page with its options, and Back returns to the list. [hidden] while another category is open.
 */
@Composable
private fun SettingsSection(
    title: String,
    summary: String,
    icon: ImageVector,
    expanded: Boolean,
    hidden: Boolean,
    onOpen: () -> Unit,
    content: @Composable () -> Unit
) {
    if (hidden) return
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
        return
    }
    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open $title", tint = OutlineColor)
        }
    }
}

@Composable
private fun SwitchSetting(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryColor)
        )
    }
}

@Composable
fun ConnectBackendScreen(
    config: ServerConnectionConfig,
    onBack: () -> Unit,
    onTestConnect: (url: String, token: String, allowSelfSigned: Boolean, cfClientId: String, cfClientSecret: String) -> Unit,
    onShowToast: (String) -> Unit,
    onDisconnect: () -> Unit = {},
    caregiverName: String = "",
    onCaregiverNameChange: (String) -> Unit = {},
    babySex: BabySex? = null,
    onBabySexChange: (BabySex) -> Unit = {},
    feedNotificationEnabled: Boolean = true,
    onFeedNotificationChange: (Boolean) -> Unit = {},
    use24h: Boolean = true,
    onUse24hChange: (Boolean) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.BY_TIME,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    reminders: ReminderSettings = ReminderSettings(),
    onRemindersChange: (ReminderSettings) -> Unit = {},
    hiddenItems: Set<TrackedItem> = emptySet(),
    onItemTrackedChange: (TrackedItem, Boolean) -> Unit = { _, _ -> },
    onEnterDemoMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLoggedIn = config.isConnected && !config.isDemoMode
    var serverUrl by remember { mutableStateOf(config.serverUrl) }
    var apiToken by remember { mutableStateOf(config.apiToken) }
    var tokenVisible by remember { mutableStateOf(false) }
    var allowSelfSigned by remember { mutableStateOf(config.allowSelfSignedCert) }
    var cfClientId by remember { mutableStateOf(config.cfClientId) }
    var cfClientSecret by remember { mutableStateOf(config.cfClientSecret) }
    var cfSecretVisible by remember { mutableStateOf(false) }
    var showCloudflare by remember { mutableStateOf(config.cfClientId.isNotBlank()) }

    // The open category (its own page within Settings), or null for the list of categories
    var openSection by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = isLoggedIn && openSection != null) { openSection = null }
    val sectionTitles = mapOf(
        "connection" to "Server connection",
        "you" to "You",
        "tracked" to "Tracked items",
        "appearance" to "Appearance",
        "notifications" to "Notifications"
    )

    // Server URL, token, certificates, Cloudflare and the Connect button
    val connectionForm: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Server URL
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://babybuddy.example.com") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_url_input")
            )

            // API token
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("API token") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (tokenVisible) "Hide token" else "Show token",
                                tint = OutlineColor
                            )
                        }
                    },
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_token_input")
                )
                Text(
                    "Find it in Baby Buddy under your user settings → API key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Self-signed certificates
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Allow self-signed certificates",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = allowSelfSigned,
                    onCheckedChange = { allowSelfSigned = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryColor)
                )
            }

            // Cloudflare Access service token (for servers behind Cloudflare Zero Trust)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Behind Cloudflare Access", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Sends a service token so the app skips the Cloudflare login",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showCloudflare,
                    onCheckedChange = {
                        showCloudflare = it
                        if (!it) {
                            cfClientId = ""
                            cfClientSecret = ""
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryColor)
                )
            }
            if (showCloudflare) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = cfClientId,
                        onValueChange = { cfClientId = it },
                        label = { Text("CF-Access-Client-Id") },
                        placeholder = { Text("xxxxxxxx.access") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("cf_client_id_input")
                    )
                    OutlinedTextField(
                        value = cfClientSecret,
                        onValueChange = { cfClientSecret = it },
                        label = { Text("CF-Access-Client-Secret") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { cfSecretVisible = !cfSecretVisible }) {
                                Icon(
                                    imageVector = if (cfSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (cfSecretVisible) "Hide secret" else "Show secret",
                                    tint = OutlineColor
                                )
                            }
                        },
                        visualTransformation = if (cfSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("cf_client_secret_input")
                    )
                    Text(
                        "Create one in Cloudflare Zero Trust → Access → Service Auth, and add a Service Auth policy to the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (serverUrl.isBlank() || apiToken.isBlank()) {
                            onShowToast("Enter a server URL and API token")
                        } else if (showCloudflare && (cfClientId.isBlank() || cfClientSecret.isBlank())) {
                            onShowToast("Enter both the Cloudflare client ID and secret")
                        } else {
                            onTestConnect(serverUrl.trim(), apiToken.trim(), allowSelfSigned, cfClientId.trim(), cfClientSecret.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = OnPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("test_connect_btn")
                ) {
                    Text(
                        text = if (isLoggedIn) "Reconnect" else "Connect",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onEnterDemoMode,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("demo_mode_btn")
                ) {
                    Text(
                        text = "Try Demo Mode with Sample Data",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryColor
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isLoggedIn) {
            // First run / logged out: this is the login screen
            Column(
                modifier = Modifier.padding(top = 48.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Baby Buddy", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Connect to your Baby Buddy server to start.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            connectionForm()
            Spacer(modifier = Modifier.height(24.dp))
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back closes an open category first, then leaves Settings
            IconButton(onClick = { if (openSection != null) openSection = null else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = openSection?.let { sectionTitles[it] } ?: "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        SettingsSection(
            title = "Server connection",
            summary = "Connected · ${config.serverUrl}",
            icon = Icons.Default.Cloud,
            expanded = openSection == "connection",
            hidden = openSection != null && openSection != "connection",
            onOpen = { openSection = "connection" }
        ) {
            connectionForm()
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("disconnect_btn")
            ) {
                Text("Disconnect", color = MaterialTheme.colorScheme.error)
            }
        }

        SettingsSection(
            title = "You",
            summary = caregiverName.ifBlank { "Add your name" },
            icon = Icons.Default.Person,
            expanded = openSection == "you",
            hidden = openSection != null && openSection != "you",
            onOpen = { openSection = "you" }
        ) {
            var name by remember { mutableStateOf(caregiverName) }
            LaunchedEffect(name) {
                delay(600) // save shortly after typing stops
                if (name.trim() != caregiverName) onCaregiverNameChange(name)
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                supportingText = { Text("Added to everything you log, so others see who logged it") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Growth charts", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Text(
                "The WHO publishes separate charts for boys and girls. Used only for the growth percentiles on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SegmentedToggle(
                options = BabySex.entries.map { it.label },
                selectedIndex = babySex?.let { BabySex.entries.indexOf(it) } ?: -1,
                onSelect = { onBabySexChange(BabySex.entries[it]) }
            )
        }

        SettingsSection(
            title = "Tracked items",
            summary = if (hiddenItems.isEmpty()) {
                "Tracking everything"
            } else {
                "Hidden: " + TrackedItem.entries.filter { it in hiddenItems }.joinToString(", ") { it.label }
            },
            icon = Icons.Default.Checklist,
            expanded = openSection == "tracked",
            hidden = openSection != null && openSection != "tracked",
            onOpen = { openSection = "tracked" }
        ) {
            Text(
                "Turn off what you don't track. It's hidden from Today, quick actions, History, Trends and reminders on this phone; nothing is deleted from the server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TrackedItem.entries.forEach { item ->
                SwitchSetting(
                    title = item.label,
                    description = item.description,
                    checked = item !in hiddenItems,
                    onChange = { on -> onItemTrackedChange(item, on) }
                )
            }
        }

        SettingsSection(
            title = "Appearance",
            summary = "${themeMode.label} · ${if (use24h) "24-hour" else "12-hour"} clock",
            icon = Icons.Default.Palette,
            expanded = openSection == "appearance",
            hidden = openSection != null && openSection != "appearance",
            onOpen = { openSection = "appearance" }
        ) {
            Column {
                Text("Theme", style = MaterialTheme.typography.labelLarge, color = OutlineColor)
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = mode == themeMode, onClick = { onThemeModeChange(mode) })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == themeMode,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryColor)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            SwitchSetting(
                title = "24-hour clock",
                description = if (use24h) "Times show as 19:06" else "Times show as 7:06 PM",
                checked = use24h,
                onChange = onUse24hChange
            )
        }

        SettingsSection(
            title = "Notifications",
            summary = listOfNotNull(
                "Lock-screen timers".takeIf { feedNotificationEnabled },
                "feed reminder".takeIf { reminders.feedAfterMinutes > 0 },
                "awake reminder".takeIf { reminders.awakeAfterMinutes > 0 },
                "sleep reminder".takeIf { reminders.sleepAfterMinutes > 0 }
            ).joinToString(" · ").replaceFirstChar(Char::uppercase).ifBlank { "Off" },
            icon = Icons.Default.Notifications,
            expanded = openSection == "notifications",
            hidden = openSection != null && openSection != "notifications",
            onOpen = { openSection = "notifications" }
        ) {
            SwitchSetting(
                title = "Timers on lock screen",
                description = "Feed Left/Right/End buttons and the sleep timer while they run",
                checked = feedNotificationEnabled,
                onChange = onFeedNotificationChange
            )
            if (TrackedItem.BREASTFEEDING !in hiddenItems || TrackedItem.BOTTLE !in hiddenItems) {
                SwitchSetting(
                    title = "Feed reminder",
                    description = if (reminders.feedAfterMinutes > 0) {
                        "When ${formatMinutes(reminders.feedAfterMinutes)} have passed since the last feed started"
                    } else {
                        "Notify when the next feed is due"
                    },
                    checked = reminders.feedAfterMinutes > 0,
                    onChange = { on -> onRemindersChange(reminders.copy(feedAfterMinutes = if (on) 180 else 0)) }
                )
            }
            if (reminders.feedAfterMinutes > 0 && (TrackedItem.BREASTFEEDING !in hiddenItems || TrackedItem.BOTTLE !in hiddenItems)) {
                DurationField(
                    label = "Remind after",
                    minutes = reminders.feedAfterMinutes,
                    onChange = { onRemindersChange(reminders.copy(feedAfterMinutes = it)) },
                    dialogNote = "Time since the last feed started (hh:mm)"
                )
            }
            if (TrackedItem.SLEEP !in hiddenItems) {
                SwitchSetting(
                    title = "Awake reminder",
                    description = if (reminders.awakeAfterMinutes > 0) {
                        "When the baby has been awake ${formatMinutes(reminders.awakeAfterMinutes)}"
                    } else {
                        "Notify when it's been a while since the last sleep"
                    },
                    checked = reminders.awakeAfterMinutes > 0,
                    onChange = { on -> onRemindersChange(reminders.copy(awakeAfterMinutes = if (on) 90 else 0)) }
                )
            }
            if (reminders.awakeAfterMinutes > 0 && TrackedItem.SLEEP !in hiddenItems) {
                DurationField(
                    label = "Remind after",
                    minutes = reminders.awakeAfterMinutes,
                    onChange = { onRemindersChange(reminders.copy(awakeAfterMinutes = it)) },
                    dialogNote = "Time awake since the last sleep ended (hh:mm)"
                )
            }
            if (TrackedItem.SLEEP !in hiddenItems) {
                SwitchSetting(
                    title = "Sleep reminder",
                    description = if (reminders.sleepAfterMinutes > 0) {
                        "When a timed sleep has lasted ${formatMinutes(reminders.sleepAfterMinutes)}"
                    } else {
                        "Notify when the baby has been asleep a long time"
                    },
                    checked = reminders.sleepAfterMinutes > 0,
                    onChange = { on -> onRemindersChange(reminders.copy(sleepAfterMinutes = if (on) 120 else 0)) }
                )
            }
            if (reminders.sleepAfterMinutes > 0 && TrackedItem.SLEEP !in hiddenItems) {
                DurationField(
                    label = "Remind after",
                    minutes = reminders.sleepAfterMinutes,
                    onChange = { onRemindersChange(reminders.copy(sleepAfterMinutes = it)) },
                    dialogNote = "Time asleep, counted from the start of the sleep timer (hh:mm)"
                )
            }
            Text(
                "Reminders check the server first, so a feed or sleep logged on another phone counts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
