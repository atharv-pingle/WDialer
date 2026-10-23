package com.coolappstore.everdialer.by.svhp.view.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>(style = TabTransitionStyle::class)
@Composable
fun RecordingsScreen(
    navigator: DestinationsNavigator? = null,
    openedFromSettings: Boolean = false,
    openedRecordingUri: String? = null
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Recordings not available")
    }
}
