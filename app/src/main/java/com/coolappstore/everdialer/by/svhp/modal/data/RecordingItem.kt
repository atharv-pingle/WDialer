package com.coolappstore.everdialer.by.svhp.modal.data

import android.net.Uri
import java.util.Date

data class RecordingItem(
    val uri: Uri = Uri.EMPTY,
    val displayName: String = "",
    val phoneNumber: String = "",
    val contactName: String? = null,
    val direction: String = "",
    val date: Date? = null,
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val extension: String = "",
    val isFavourite: Boolean = false,
    val noteText: String = ""
)
