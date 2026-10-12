package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorBlue   = Color(0xFF2196F3)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorPurple = Color(0xFF9C27B0)
private val ColorTeal   = Color(0xFF009688)
private val ColorRed    = Color(0xFFE53935)
private val ColorAmber  = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun CallLogsSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val keyboardController = LocalSoftwareKeyboardController.current
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var groupCallsByLatest by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, false)) }
    var showSimsInCallLogs by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, prefs.getShowSimsInCallLogsDefault())) }
    var showTotalCallsMade by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)) }
    var nameNonContactsAsUnknown by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NAME_NON_CONTACTS_AS_UNKNOWN, true)) }
    var showNumbersInCallLogs by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_NUMBERS_IN_CALL_LOGS, true)) }
    var autoDeleteUnknownEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED, false)) }
    var autoDeleteUnknownValue by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_VALUE, 1).toString()) }
    var autoDeleteUnknownUnit by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, "days") ?: "days") }
    var callTimeFormat24h by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false)) }
    var showTalkTimeInCallLogs by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_TALK_TIME_IN_CALL_LOGS, false)) }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Call Logs",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        "Call Log Options",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Group numbers in call logs based on latest calls",
                            supporting = "Sort call logs by latest calls made and group numbers, showing total call count",
                            leadingIcon = Icons.Outlined.Layers,
                            iconContainerColor = ColorBlue,
                            checked = groupCallsByLatest,
                            modifier = Modifier.settingsSearchHighlight("group_calls_by_latest", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                groupCallsByLatest = it
                                prefs.setBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, it)
                                if (it) {
                                    showTotalCallsMade = true
                                    prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, true)
                                } else {
                                    showTotalCallsMade = false
                                    prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)
                                }
                            }
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "In call logs, long pressing the all button will show a menu to choose grouped based on number or by date.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show Sims In Call Logs",
                            supporting = "Show a SIM icon with its number on calls in Call Logs",
                            leadingIcon = Icons.Outlined.SimCard,
                            iconContainerColor = ColorGreen,
                            checked = showSimsInCallLogs,
                            modifier = Modifier.settingsSearchHighlight("show_sims_call_logs", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showSimsInCallLogs = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show total number of calls made",
                            supporting = "Show the total call count for each contact or number in Call Logs",
                            leadingIcon = Icons.Outlined.Call,
                            iconContainerColor = ColorPurple,
                            checked = showTotalCallsMade,
                            modifier = Modifier.settingsSearchHighlight("show_total_calls_made", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showTotalCallsMade = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Name non contacts as Unknown",
                            supporting = if (nameNonContactsAsUnknown) "Show \"Unknown\" as the name for numbers not in your contacts" else "Directly show the phone number instead of \"Unknown\"",
                            leadingIcon = Icons.Outlined.PersonOff,
                            iconContainerColor = ColorTeal,
                            checked = nameNonContactsAsUnknown,
                            modifier = Modifier.settingsSearchHighlight("name_non_contacts_as_unknown", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                nameNonContactsAsUnknown = it
                                prefs.setBoolean(PreferenceManager.KEY_NAME_NON_CONTACTS_AS_UNKNOWN, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show numbers in call logs",
                            supporting = "Show phone numbers below contact names in call logs. When disabled, numbers are hidden for saved contacts",
                            leadingIcon = Icons.Outlined.Numbers,
                            iconContainerColor = ColorBlue,
                            checked = showNumbersInCallLogs,
                            modifier = Modifier.settingsSearchHighlight("show_numbers_in_call_logs", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showNumbersInCallLogs = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_NUMBERS_IN_CALL_LOGS, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Call Time Format in call logs",
                            supporting = if (callTimeFormat24h) "Showing call times in 24-hour format" else "Showing call times in 12-hour format",
                            leadingIcon = Icons.Outlined.Schedule,
                            iconContainerColor = ColorAmber,
                            checked = callTimeFormat24h,
                            modifier = Modifier.settingsSearchHighlight("call_time_format", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                callTimeFormat24h = it
                                prefs.setBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Talk time in call logs",
                            supporting = if (showTalkTimeInCallLogs) "Showing call duration for answered calls below the time" else "Call duration hidden in call logs",
                            leadingIcon = Icons.Outlined.Timer,
                            iconContainerColor = ColorGreen,
                            checked = showTalkTimeInCallLogs,
                            modifier = Modifier.settingsSearchHighlight("talk_time_in_call_logs", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showTalkTimeInCallLogs = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_TALK_TIME_IN_CALL_LOGS, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Auto Delete Unknown No in call log",
                            supporting = if (autoDeleteUnknownEnabled)
                                "Deletes call log entries from unsaved numbers older than $autoDeleteUnknownValue ${if (autoDeleteUnknownUnit == "hours") "hour(s)" else "day(s)"}"
                            else "Set a duration below, then turn this on to remove old call log entries from numbers not in your contacts",
                            leadingIcon = Icons.Outlined.AutoDelete,
                            iconContainerColor = ColorRed,
                            checked = autoDeleteUnknownEnabled,
                            modifier = Modifier.settingsSearchHighlight("auto_delete_unknown_calllog", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { enabled ->
                                autoDeleteUnknownEnabled = enabled
                                prefs.setBoolean(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED, enabled)
                                if (enabled) {
                                    val n = autoDeleteUnknownValue.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    autoDeleteUnknownValue = n.toString()
                                    prefs.setInt(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_VALUE, n)
                                    prefs.setString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, autoDeleteUnknownUnit)
                                    prefs.setLong(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED_AT, System.currentTimeMillis())
                                }
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(start = 68.dp, end = 16.dp, bottom = 14.dp, top = 2.dp)
                        ) {
                            OutlinedTextField(
                                value = autoDeleteUnknownValue,
                                onValueChange = { raw ->
                                    val digitsOnly = raw.filter { it.isDigit() }.take(4)
                                    autoDeleteUnknownValue = digitsOnly
                                    val n = digitsOnly.toIntOrNull()
                                    if (n != null && n > 0) {
                                        prefs.setInt(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_VALUE, n)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(90.dp)
                            )
                            FilterChip(
                                selected = autoDeleteUnknownUnit == "days",
                                onClick = {
                                    autoDeleteUnknownUnit = "days"
                                    prefs.setString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, "days")
                                },
                                label = { Text("Days") }
                            )
                            FilterChip(
                                selected = autoDeleteUnknownUnit == "hours",
                                onClick = {
                                    autoDeleteUnknownUnit = "hours"
                                    prefs.setString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, "hours")
                                },
                                label = { Text("Hours") }
                            )
                        }
                    }
                }
            }
        }
    }
}
