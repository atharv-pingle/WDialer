package com.coolappstore.everdialer.by.svhp.modal.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED
}

enum class CallSource {
    WHATSAPP,
    WHATSAPP_BUSINESS,
    CELLULAR
}

enum class MediaType {
    VOICE,
    VIDEO
}

@Entity(tableName = "unified_call_logs")
@Immutable
@Serializable
data class UnifiedCallLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val displayName: String? = null,
    val callType: CallType,
    val callSource: CallSource = CallSource.WHATSAPP,
    val mediaType: MediaType = MediaType.VOICE,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L,
    val simSlot: Int = -1,
    val isRead: Boolean = true
)
