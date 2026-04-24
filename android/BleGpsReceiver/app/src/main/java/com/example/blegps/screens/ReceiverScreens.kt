package com.example.blegps.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReceiverApp() {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Receiver Home", "Logs", "Settings")

    Scaffold(topBar = { TopAppBar(title = { Text("BLE GPS Receiver") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(selected = tab == idx, onClick = { tab = idx }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> ReceiverHome()
                1 -> LogsScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
fun ReceiverHome() {
    Text("Receiver running in foreground service.\nConnect to iOS peripheral to receive GPS.")
}

@Composable
fun LogsScreen() {
    Text("Sample log:\n- Connected to BLE device\n- Notification received\n- Location injected")
}

@Composable
fun SettingsScreen() {
    var injectionEnabled by remember { mutableStateOf(true) }
    RowWithSwitch("Inject into system mock provider", injectionEnabled) { injectionEnabled = it }
    Text("When disabled, app can still use in-app location source for embedded maps.")
}

@Composable
private fun RowWithSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}
