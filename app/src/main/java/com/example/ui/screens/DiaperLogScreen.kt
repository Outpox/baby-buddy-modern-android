package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BabyProfile
import com.example.data.model.DiaperEvent
import com.example.data.model.DiaperType
import com.example.data.model.TimeFormat
import com.example.data.model.TrackedItem
import com.example.ui.components.TimeField
import com.example.ui.theme.ItemColors
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SecondaryColor
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import kotlinx.coroutines.delay

@Composable
fun DiaperLogScreen(
    babyProfile: BabyProfile,
    recentDiapers: List<DiaperEvent>,
    onBack: () -> Unit,
    onSaveDiaper: (
        type: DiaperType,
        amount: String,
        consistency: String,
        color: String,
        rashPresent: Boolean,
        skinBarriers: List<String>,
        timeMillis: Long?, // null = now
        notes: String,
        startNursing: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(DiaperType.WET) }
    var selectedAmount by remember { mutableStateOf("Medium") }
    var selectedConsistency by remember { mutableStateOf("") } // optional; "" = not recorded
    var selectedColor by remember { mutableStateOf("Mustard") }
    var rashPresent by remember { mutableStateOf(false) }
    var selectedSkinBarriers by remember { mutableStateOf(setOf<String>()) }
    var pickedTime by remember { mutableStateOf<Long?>(null) } // null = now
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            now = System.currentTimeMillis()
        }
    }
    var notesText by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }

    val isDirty = selectedType == DiaperType.DIRTY || selectedType == DiaperType.BOTH

    val save: (Boolean) -> Unit = { startNursing ->
        onSaveDiaper(
            selectedType,
            selectedAmount,
            selectedConsistency,
            selectedColor,
            rashPresent,
            selectedSkinBarriers.toList(),
            pickedTime,
            notesText,
            startNursing
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerLow)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Diaper change",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Type selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiaperTypeButton(
                    label = "Wet",
                    icon = Icons.Default.WaterDrop,
                    isSelected = selectedType == DiaperType.WET,
                    tint = ItemColors.tint(TrackedItem.DIAPERS),
                    bgSelected = ItemColors.tint(TrackedItem.DIAPERS),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = DiaperType.WET }
                )
                DiaperTypeButton(
                    label = "Dirty",
                    icon = Icons.Default.Eco,
                    isSelected = selectedType == DiaperType.DIRTY,
                    tint = ItemColors.tint(TrackedItem.DIAPERS),
                    bgSelected = ItemColors.tint(TrackedItem.DIAPERS),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = DiaperType.DIRTY }
                )
                DiaperTypeButton(
                    label = "Both",
                    icon = Icons.Default.CleaningServices,
                    isSelected = selectedType == DiaperType.BOTH,
                    tint = ItemColors.tint(TrackedItem.DIAPERS),
                    bgSelected = ItemColors.tint(TrackedItem.DIAPERS),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = DiaperType.BOTH }
                )
                DiaperTypeButton(
                    label = "Dry",
                    icon = Icons.Default.CheckCircle,
                    isSelected = selectedType == DiaperType.DRY,
                    tint = ItemColors.tint(TrackedItem.DIAPERS),
                    bgSelected = ItemColors.tint(TrackedItem.DIAPERS),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = DiaperType.DRY }
                )
            }
        }

        // Time of the change: now unless changed
        item {
            TimeField(
                label = "Time",
                millis = pickedTime ?: now,
                onChange = { pickedTime = it },
                valueText = if (pickedTime == null) "Now · ${TimeFormat.clock(now)}" else null
            )
        }

        // Amount
        if (selectedType != DiaperType.DRY) {
            item {
                DiaperSection(title = "Amount") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Light", "Medium", "Heavy").forEach { amt ->
                            DiaperOptionChip(
                                label = amt,
                                isSelected = selectedAmount == amt,
                                onClick = { selectedAmount = amt },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Stool details (dirty / both only)
        item {
            AnimatedVisibility(visible = isDirty) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    DiaperSection(title = "Consistency (optional)") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "Loose / Liquid" to "Loose",
                                "Seedy / Mustard" to "Seedy",
                                "Paste / Normal" to "Paste",
                                "Hard / Dry" to "Hard"
                            ).forEach { (value, label) ->
                                DiaperOptionChip(
                                    label = label,
                                    isSelected = selectedConsistency == value,
                                    onClick = { selectedConsistency = if (selectedConsistency == value) "" else value },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    DiaperSection(title = "Color") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "Mustard" to PrimaryContainer,
                                "Tan/Brown" to OutlineColor,
                                "Greenish" to SecondaryColor,
                                "Dark" to MaterialTheme.colorScheme.inverseSurface
                            ).forEach { (label, dot) ->
                                ColorSwatch(
                                    label = label,
                                    colorDot = dot,
                                    isSelected = selectedColor == label,
                                    onClick = { selectedColor = label },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // More details (rash, creams, notes)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMore = !showMore }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("More details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Icon(
                        imageVector = if (showMore) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showMore) "Hide details" else "Show details",
                        tint = OutlineColor
                    )
                }

                AnimatedVisibility(visible = showMore) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rash", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = rashPresent,
                                onCheckedChange = { rashPresent = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = OnPrimary, checkedTrackColor = PrimaryColor)
                            )
                        }

                        DiaperSection(title = "Cream") {
                            val barriers = listOf("Aquaphor / Barrier" to "Barrier", "Zinc Oxide Cream" to "Zinc", "Baby Powder" to "Powder")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                barriers.forEach { (value, label) ->
                                    val isSel = value in selectedSkinBarriers
                                    DiaperOptionChip(
                                        label = label,
                                        isSelected = isSel,
                                        onClick = {
                                            selectedSkinBarriers = if (isSel) selectedSkinBarriers - value else selectedSkinBarriers + value
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("Notes", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = SurfaceContainerLow,
                                focusedContainerColor = SurfaceContainerLowest,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = PrimaryColor
                            )
                        )
                    }
                }
            }
        }

        // Actions
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { save(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_diaper_btn")
                ) {
                    Text("Save", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = { save(true) }) {
                    Text("Save & start feed", color = SecondaryColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiaperSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = OutlineColor)
        content()
    }
}

@Composable
private fun DiaperOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PrimaryColor else SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DiaperTypeButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    tint: Color,
    bgSelected: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) bgSelected else SurfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) OnPrimary else tint, modifier = Modifier.size(24.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    label: String,
    colorDot: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PrimaryFixed else SurfaceContainer,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(colorDot))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) PrimaryColor else OutlineColor
            )
        }
    }
}
