package com.tap.apk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tap.apk.models.FlashMode
import com.tap.apk.models.TapAction
import com.tap.apk.models.TapEvent
import com.tap.apk.models.TapPatternConfig
import kotlinx.coroutines.launch

@Composable
fun TapSettingsScreen(
    settings: Map<TapEvent, TapPatternConfig>,
    onSave: (TapEvent, TapPatternConfig) -> Unit,
    onTest: (TapEvent) -> Unit,
) {
    val tabs = listOf(TapEvent.Single, TapEvent.Double, TapEvent.Triple)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1D))
    ) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, event ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(event.name.uppercase()) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val event = tabs[page]
            val config = settings[event] ?: TapPatternConfig()
            PatternCard(event, config, onSave, onTest)
        }
    }
}

@Composable
private fun PatternCard(
    event: TapEvent,
    config: TapPatternConfig,
    onSave: (TapEvent, TapPatternConfig) -> Unit,
    onTest: (TapEvent) -> Unit,
) {
    var enabled by remember(config) { mutableStateOf(config.enabled) }
    var cooldown by remember(config) { mutableStateOf(config.cooldownMs.toFloat() / 1000f) }
    var actionType by remember(config) {
        mutableStateOf(
            when (config.action) {
                TapAction.None -> "None"
                is TapAction.Flashlight -> "Flashlight"
                is TapAction.LaunchApp -> "Open App"
                is TapAction.Termux -> "Termux Command"
            }
        )
    }
    var packageOrCommand by remember(config) {
        mutableStateOf(
            when (val action = config.action) {
                is TapAction.LaunchApp -> action.packageName
                is TapAction.Termux -> action.command
                else -> ""
            }
        )
    }
    var flashMode by remember(config) {
        mutableStateOf(
            if (config.action is TapAction.Flashlight) config.action.mode else FlashMode.Toggle
        )
    }

    LaunchedEffect(enabled, cooldown, actionType, packageOrCommand, flashMode) {
        val action = when (actionType) {
            "Flashlight" -> TapAction.Flashlight(flashMode)
            "Open App" -> TapAction.LaunchApp(packageOrCommand.trim())
            "Termux Command" -> TapAction.Termux(packageOrCommand.trim())
            else -> TapAction.None
        }
        onSave(event, TapPatternConfig(enabled, (cooldown * 1000f).toLong(), action))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFC3C2B7))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionDropdown(actionType) { actionType = it }

            if (actionType == "Flashlight") {
                FlashModeDropdown(flashMode) { flashMode = it }
            }

            if (actionType == "Open App" || actionType == "Termux Command") {
                OutlinedTextField(
                    value = packageOrCommand,
                    onValueChange = { packageOrCommand = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (actionType == "Open App") "Package Name" else "Command")
                    }
                )
            }

            Button(
                onClick = { onTest(event) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TEST ${event.name.uppercase()} TAP")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enabled", color = Color(0xFF1E1E1D))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Text("Cooldown: ${"%.1f".format(cooldown)}s", color = Color(0xFF1E1E1D))
            Slider(
                value = cooldown,
                onValueChange = { cooldown = it },
                valueRange = 0.5f..5f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionDropdown(value: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("None", "Flashlight", "Open App", "Termux Command")

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Action") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashModeDropdown(value: FlashMode, onSelect: (FlashMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = FlashMode.entries

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
