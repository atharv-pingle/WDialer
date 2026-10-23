package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun CallRecordingSettingsScreen(
    navigator: DestinationsNavigator
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Call recording not active")
    }
}
