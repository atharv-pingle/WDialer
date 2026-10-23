package com.coolappstore.everdialer.by.svhp.modal.repository

import android.content.Context
import com.coolappstore.everdialer.by.svhp.modal.data.AppDatabase
import com.coolappstore.everdialer.by.svhp.modal.data.CallSource
import com.coolappstore.everdialer.by.svhp.modal.data.CallType
import com.coolappstore.everdialer.by.svhp.modal.data.MediaType
import com.coolappstore.everdialer.by.svhp.modal.data.UnifiedCallLog
import com.coolappstore.everdialer.by.svhp.modal.data.UnifiedCallLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UnifiedCallLogRepository(
    private val context: Context,
    private val dao: UnifiedCallLogDao = AppDatabase.getInstance(context).unifiedCallLogDao()
) {

    val allCallLogsFlow: Flow<List<UnifiedCallLog>> = dao.getAllCallLogsFlow()
    val missedCallsFlow: Flow<List<UnifiedCallLog>> = dao.getMissedCallsFlow()

    fun getLogsBySource(source: CallSource): Flow<List<UnifiedCallLog>> =
        dao.getCallsBySourceFlow(source)

    suspend fun insertCall(callLog: UnifiedCallLog): Long = withContext(Dispatchers.IO) {
        dao.insert(callLog)
    }

    suspend fun logOutgoingWhatsAppCall(
        phoneNumber: String,
        displayName: String?,
        mediaType: MediaType = MediaType.VOICE
    ): Long = withContext(Dispatchers.IO) {
        dao.insert(
            UnifiedCallLog(
                phoneNumber = phoneNumber,
                displayName = displayName,
                callType = CallType.OUTGOING,
                callSource = CallSource.WHATSAPP,
                mediaType = mediaType,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCall(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteCallsForNumber(number: String) = withContext(Dispatchers.IO) {
        dao.deleteByNumber(number)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }
}
