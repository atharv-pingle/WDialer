package com.coolappstore.everdialer.by.svhp.controller

import androidx.lifecycle.ViewModel
import com.coolappstore.everdialer.by.svhp.modal.data.RecordingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val _allRecordings = MutableStateFlow<List<RecordingItem>>(emptyList())
    val allRecordings: StateFlow<List<RecordingItem>> = _allRecordings.asStateFlow()

    fun refresh() {}
}
