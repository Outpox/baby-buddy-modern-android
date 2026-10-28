package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DashboardWidgetConfig
import com.example.data.model.DashboardWidgetType
import com.example.data.model.TodayTile
import com.example.data.model.TrackedItem
import com.example.data.model.WakeWindowPart
import com.example.ui.theme.ItemColors
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SecondaryColor
import com.example.ui.theme.SecondaryContainer
import com.example.ui.theme.SecondaryFixed
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TertiaryColor
import com.example.ui.theme.TertiaryFixed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetModal(
    dashboardWidgets: List<DashboardWidgetConfig>,
    onToggleWidget: (DashboardWidgetType) -> Unit,
    onPinHomeScreenWidget: (String) -> Unit,
    onDismiss: () -> Unit,
    hiddenTiles: Set<TodayTile> = emptySet(),
    onToggleTile: (TodayTile) -> Unit = {},
    hiddenWakeParts: Set<WakeWindowPart> = emptySet(),
    onToggleWakePart: (WakeWindowPart) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Customize Today",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cards and home-screen widgets",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tab Switcher
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceContainerLow,
                contentColor = PrimaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryColor,
                        height = 3.dp
                    )
                }
            ) {
                val activeCount = dashboardWidgets.count { it.isEnabled }
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Dashboard Cards",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedTab == 0) PrimaryFixed else SurfaceContainerHigh
                            ) {
                                Text(
                                    text = "$activeCount active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedTab == 0) PrimaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                "Phone Home Screen",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
            }

            // Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedTab == 0) {
                    DashboardWidgetsTab(
                        widgets = dashboardWidgets,
                        onToggleWidget = onToggleWidget,
                        hiddenTiles = hiddenTiles,
                        onToggleTile = onToggleTile,
                        hiddenWakeParts = hiddenWakeParts,
                        onToggleWakePart = onToggleWakePart
                    )
                } else {
                    HomeScreenWidgetsTab(
                        onPinWidget = onPinHomeScreenWidget
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardWidgetsTab(
    widgets: List<DashboardWidgetConfig>,
    onToggleWidget: (DashboardWidgetType) -> Unit,
    hiddenTiles: Set<TodayTile>,
    onToggleTile: (TodayTile) -> Unit,
    hiddenWakeParts: Set<WakeWindowPart>,
    onToggleWakePart: (WakeWindowPart) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = PrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Turn cards on or off for your Today view.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Summary tiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "The \"Today\" grid of totals. Items you don't track stay hidden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ToggleChips(
                        options = TodayTile.entries.map { it.label to it },
                        isOn = { it !in hiddenTiles },
                        onToggle = onToggleTile
                    )
                }
            }
        }

        items(widgets) { widget ->
            DashboardWidgetItemCard(
                widget = widget,
                onToggle = { onToggleWidget(widget.type) },
                // The Wake windows card's parts, while it's on
                options = if (widget.type == DashboardWidgetType.WAKE_WINDOWS && widget.isEnabled) {
                    {
                        Text("Show", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ToggleChips(
                            options = WakeWindowPart.entries.map { it.label to it },
                            isOn = { it !in hiddenWakeParts },
                            onToggle = onToggleWakePart
                        )
                    }
                } else {
                    null
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardWidgetItemCard(
    widget: DashboardWidgetConfig,
    onToggle: () -> Unit,
    options: (@Composable ColumnScope.() -> Unit)? = null // settings shown under the card
) {
    val icon = getWidgetIcon(widget.type)
    val colorAccent = getWidgetAccent(widget.type)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (widget.isEnabled) SurfaceContainerLowest else SurfaceContainerLow
        ),
        border = if (widget.isEnabled) androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant) else null,
        elevation = CardDefaults.cardElevation(if (widget.isEnabled) 1.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("widget_card_${widget.type.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = widget.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = getWidgetDescription(widget.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceContainerHigh
                ) {
                    Text(
                        text = widget.type.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (widget.isEnabled) SecondaryFixed else PrimaryColor,
                    contentColor = if (widget.isEnabled) SecondaryColor else OnPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (widget.isEnabled) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Added", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (options != null) {
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = options
            )
        }
    }
}

/** Pill chips that turn options on or off. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ToggleChips(options: List<Pair<String, T>>, isOn: (T) -> Boolean, onToggle: (T) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, value) ->
            val on = isOn(value)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) SecondaryFixed else SurfaceContainerHigh)
                    .clickable { onToggle(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (on) Icon(Icons.Default.Check, contentDescription = null, tint = SecondaryColor, modifier = Modifier.size(14.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) SecondaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HomeScreenWidgetsTab(
    onPinWidget: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PrimaryFixed.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📱 Android Home Screen Widgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add these widgets directly to your phone's home screen for rapid 1-tap logging and live timer controls without opening the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Quick Log Launcher Widget Preview
        item {
            HomeScreenWidgetCard(
                title = "1-Tap Quick Logger Widget",
                sizeLabel = "4 × 1 Standard",
                description = "Feed, Diaper and Tummy time buttons.",
                onPinClick = { onPinWidget("QUICK_LOG") }
            ) {
                // Interactive Preview of Quick Log Widget
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Baby Leo • 3m 12d",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor
                            )
                            Text(
                                "Last fed 45m ago",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionChip(icon = Icons.Default.WaterDrop, label = "Feed", color = PrimaryColor, modifier = Modifier.weight(1f))
                            QuickActionChip(icon = Icons.Default.Opacity, label = "Diaper", color = ItemColors.tint(TrackedItem.DIAPERS), modifier = Modifier.weight(1f))
                            QuickActionChip(icon = Icons.Default.FitnessCenter, label = "Tummy", color = ItemColors.tint(TrackedItem.TUMMY_TIME), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 2. Live Nursing Timer Widget Preview
        item {
            HomeScreenWidgetCard(
                title = "Live Active Nursing Timer Widget",
                sizeLabel = "4 × 2 Full Controls",
                description = "Real-time stopwatch with Left/Right breakdown and Pause/Switch buttons.",
                onPinClick = { onPinWidget("TIMER") }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤱 LIVE NURSING TIMER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PrimaryColor)
                            Text("Right Breast Active", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SecondaryColor)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("08:42", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryColor)
                                Text("L: 05:45 • R: 02:57", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SurfaceContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = SecondaryColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Switch Side", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = PrimaryColor,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = OnPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Daily Care Summary Widget Preview
        item {
            HomeScreenWidgetCard(
                title = "Today At-a-Glance Summary Widget",
                sizeLabel = "4 × 1",
                description = "Today's feeds and diapers.",
                onPinClick = { onPinWidget("SUMMARY") }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Today", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PrimaryColor)
                            Text("Baby Buddy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SecondaryColor)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniSummaryBox("FEEDS", "6", "", PrimaryColor, Modifier.weight(1f))
                            MiniSummaryBox("DIAPERS", "5 wet · 2 dirty", "", ItemColors.tint(TrackedItem.DIAPERS), Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Tips footer
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "💡 Manual Installation Tip",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If your launcher doesn't support 1-tap pinning, touch & hold any empty spot on your Android home screen, tap 'Widgets', scroll to 'Baby Buddy', and drag it into position.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeScreenWidgetCard(
    title: String,
    sizeLabel: String,
    description: String,
    onPinClick: () -> Unit,
    previewContent: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainerHigh
                ) {
                    Text(
                        text = sizeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            previewContent()

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onPinClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = OnPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Add to Android Home Screen",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun MiniSummaryBox(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerLow,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color, fontSize = 9.sp)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

private fun getWidgetIcon(type: DashboardWidgetType): ImageVector = when (type) {
    DashboardWidgetType.LAST_FEED -> Icons.Default.Restaurant
    DashboardWidgetType.LAST_DIAPER -> Icons.Default.WaterDrop
    DashboardWidgetType.WAKE_WINDOWS -> Icons.Default.WbSunny
    DashboardWidgetType.TUMMY_TIME_GOAL -> Icons.Default.FitnessCenter
    DashboardWidgetType.HEALTH_VITALS -> Icons.Default.Thermostat
    DashboardWidgetType.CAREGIVER_NOTES -> Icons.Default.NoteAlt
    DashboardWidgetType.GROWTH -> Icons.Default.MonitorWeight
}

private fun getWidgetAccent(type: DashboardWidgetType): Color = when (type) {
    DashboardWidgetType.LAST_FEED -> ItemColors.tint(TrackedItem.BREASTFEEDING)
    DashboardWidgetType.LAST_DIAPER -> ItemColors.tint(TrackedItem.DIAPERS)
    DashboardWidgetType.WAKE_WINDOWS -> ItemColors.tint(TrackedItem.SLEEP)
    DashboardWidgetType.TUMMY_TIME_GOAL -> ItemColors.tint(TrackedItem.TUMMY_TIME)
    DashboardWidgetType.HEALTH_VITALS -> PrimaryColor
    DashboardWidgetType.CAREGIVER_NOTES -> ItemColors.tint(TrackedItem.QUICK_NOTES)
    DashboardWidgetType.GROWTH -> ItemColors.tint(TrackedItem.GROWTH)
}

private fun getWidgetDescription(type: DashboardWidgetType): String = when (type) {
    DashboardWidgetType.LAST_FEED -> "When the last feed started, how long ago, and the last side."
    DashboardWidgetType.LAST_DIAPER -> "When the last diaper change was, how long ago, and its type."
    DashboardWidgetType.WAKE_WINDOWS -> "How long the baby has been awake, when the next nap is due, and today's wake windows."
    DashboardWidgetType.TUMMY_TIME_GOAL -> "Log tummy time towards a daily goal."
    DashboardWidgetType.HEALTH_VITALS -> "Log and see temperature readings."
    DashboardWidgetType.CAREGIVER_NOTES -> "Shared notes like baths and routines."
    DashboardWidgetType.GROWTH -> "Latest weight, length and head size with WHO percentiles."
}
