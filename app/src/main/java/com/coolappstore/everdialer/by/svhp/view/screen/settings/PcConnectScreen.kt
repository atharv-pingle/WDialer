package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun PcConnectScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val syncManager = koinInject<com.coolappstore.everdialer.by.svhp.controller.util.PcBluetoothSyncManager>()
    val scrollState = rememberScrollState()

    var syncFavorites by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_FAVOURITES, true))
    }
    var syncCallLogs by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CALL_LOGS, true))
    }
    var syncContacts by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CONTACTS, true))
    }
    var syncSms by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_SMS, true))
    }
    var syncNotes by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_NOTES, true))
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "PC Connect",
                onBackClick = { navigator.navigateUp() },
                actions = {
                    IconButton(
                        onClick = {
                            navigator.navigate(
                                com.ramcosta.composedestinations.generated.destinations.PcSyncSettingsScreenDestination()
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "PC Sync Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    SectionLabel("To sync")
                    RivoExpressiveCard {
                        // 1. Favourites
                        RivoSwitchListItem(
                            headline = "Favourites",
                            supporting = "Sync starred & favorite contacts",
                            leadingIcon = Icons.Default.Favorite,
                            iconContainerColor = Color(0xFFE91E63),
                            checked = syncFavorites,
                            onCheckedChange = { isChecked ->
                                syncFavorites = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_FAVOURITES, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                        CardDivider()

                        // 2. Call Logs
                        RivoSwitchListItem(
                            headline = "Call Logs",
                            supporting = "Sync incoming, outgoing, and missed call history",
                            leadingIcon = Icons.Default.History,
                            iconContainerColor = Color(0xFF4CAF50),
                            checked = syncCallLogs,
                            onCheckedChange = { isChecked ->
                                syncCallLogs = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_CALL_LOGS, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                        CardDivider()

                        // 3. Contacts
                        RivoSwitchListItem(
                            headline = "Contacts",
                            supporting = "Sync full contact list and details",
                            leadingIcon = Icons.Default.Person,
                            iconContainerColor = Color(0xFF2196F3),
                            checked = syncContacts,
                            onCheckedChange = { isChecked ->
                                syncContacts = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_CONTACTS, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                        CardDivider()

                        // 4. SMS
                        RivoSwitchListItem(
                            headline = "SMS",
                            supporting = "Sync text message conversations and chats",
                            leadingIcon = Icons.Default.Chat,
                            iconContainerColor = Color(0xFF9C27B0),
                            checked = syncSms,
                            onCheckedChange = { isChecked ->
                                syncSms = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_SMS, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                        CardDivider()

                        // 5. Notes
                        RivoSwitchListItem(
                            headline = "Notes",
                            supporting = "Sync call notes and custom notes",
                            leadingIcon = Icons.Default.Note,
                            iconContainerColor = Color(0xFFFF9800),
                            checked = syncNotes,
                            onCheckedChange = { isChecked ->
                                syncNotes = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_NOTES, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                    }
                }
            }
        }
    }
}
